package com.xp.hos.service.impl;

import com.google.common.base.Strings;
import com.xp.hos.exception.ErrorCodes;
import com.xp.hos.exception.HosServerException;
import com.xp.hos.hbase.HbaseServiceImpl;
import com.xp.hos.hdfs.IHDFSServer;
import com.xp.hos.pojo.HosObject;
import com.xp.hos.pojo.HosObjectSummary;
import com.xp.hos.pojo.ObjectListResult;
import com.xp.hos.pojo.ObjectMetaData;
import com.xp.hos.service.IHosStore;
import com.xp.hos.util.JsonUtil;
import com.xp.hos.utils.HosUtil;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.io.ByteBufferInputStream;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;

public class HosStoreImpl implements IHosStore {

    private static Logger logger = LoggerFactory.getLogger(HosStoreImpl.class);
    private Connection connection = null;
    private IHDFSServer fileStore;
    private String zkUrls;
    private CuratorFramework zkClient;

    public HosStoreImpl(Connection connection, IHDFSServer ihdfsServer, String zkUrls) {
        this.connection = connection;
        this.fileStore = ihdfsServer;
        this.zkUrls = zkUrls;
        zkClient = CuratorFrameworkFactory.newClient(zkUrls, new ExponentialBackoffRetry(20, 5));
        zkClient.start();
    }

    @Override
    public void createBucketStore(String bucket) throws IOException {
        //1.创建目录表
        HbaseServiceImpl.createTable(connection, HosUtil.getDirTableName(bucket), HosUtil.getDirColumnFamily(), null);

        //2.创建文件表
        HbaseServiceImpl.createTable(connection, HosUtil.getObjTableName(bucket), HosUtil.getObjColumnFamily(), HosUtil.OBJ_REGIONS);

        //3.将其添加到seq表
        Put put = new Put(bucket.getBytes());
        put.addColumn(HosUtil.BUCKET_DIR_SEQ_CF_BYTES, HosUtil.BUCKET_DIR_SEQ_COLUMN, Bytes.toBytes(0L));
        HbaseServiceImpl.putRow(connection, HosUtil.BUCKET_DIR_SEQ_TABLE, put);

        //4.创建hdfs目录
        fileStore.mkDir(HosUtil.FILE_STORE_ROOT + "/" + bucket);
    }

    @Override
    public void deleteBucketStore(String bucket) throws IOException {
        //删除目录表和文件表
        HbaseServiceImpl.deleteTable(connection, HosUtil.getDirTableName(bucket));
        HbaseServiceImpl.deleteTable(connection, HosUtil.getObjTableName(bucket));

        //删除seq表中的记录
        HbaseServiceImpl.deleteRow(connection, HosUtil.BUCKET_DIR_SEQ_TABLE, bucket);

        //删除hdfs上的目录
        fileStore.deleteDir(HosUtil.FILE_STORE_ROOT + "/" + bucket);
    }

    @Override
    public void createSeqIdTable() {
        HbaseServiceImpl.createTable(connection, HosUtil.BUCKET_DIR_SEQ_TABLE, new String[]{HosUtil.BUCKET_DIR_SEQ_CF}, null);
    }

    @Override
    public void put(String bucket, String key, ByteBuffer content, long length, String mediaType, Map<String, String> attrs) throws Exception {
        //创建目录
        if (key.endsWith("/")) {
            putDir(bucket, key);
            return;
        }

        //获取seqId
        String dir = key.substring(0, key.lastIndexOf("/") + 1);
        String hash = null;
        while (hash == null) {
            if (!this.dirExist(bucket, dir)) {
                hash = putDir(bucket, key);
            } else {
                hash = getDirSeqId(bucket, dir);
            }
        }

        //上传文件
        //获取锁
        InterProcessMutex lock = null;
        String lockey = key.replace("/", "_");
        lock = new InterProcessMutex(zkClient, "/hos/" + bucket + "/" + lockey);
        lock.acquire();

        String fileKey = hash + "_" + key.substring(key.lastIndexOf("/") + 1);
        Put put = new Put(fileKey.getBytes());
        if (!Strings.isNullOrEmpty(mediaType)) {
            put.addColumn(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_FILETYPE_COLUMN, Bytes.toBytes(mediaType));
        }
        //还需要添加文件表中cf列族下creator,size列的值
        //...

        //添加文件表下c列族下content列的值(文件内容)
        //判断文件大小
        if (length <= HosUtil.FILE_STORE_THRESHOLD) {
            //存储到hbase
            ByteBuffer byteBuffer = ByteBuffer.wrap(HosUtil.OBJ_CONT_COLUMN);
            put.addColumn(HosUtil.OBJ_CONTENT_CF_BYTES, byteBuffer, System.currentTimeMillis(), content);
            byteBuffer.clear();
        } else {
            //存储到hdfs
            //文件存储路径(root根目录/表名/seqid)
            String fileDir = HosUtil.FILE_STORE_ROOT + "/" + bucket + "/" + hash;
            String fileName = key.substring(key.lastIndexOf("/") + 1);
            InputStream inputStream = new ByteBufferInputStream(content);
            fileStore.saveFile(fileDir, fileName, inputStream, length, (short) 1);
        }

        HbaseServiceImpl.putRow(connection, HosUtil.getObjTableName(bucket), put);

        //释放锁
        if (lock != null) {
            lock.release();
        }
    }

    //获取目录表或文件表cf列族内的基础属性
    @Override
    public HosObjectSummary getSummary(String bucket, String key) throws IOException {
        //判断是文件夹还是文件
        if (key.endsWith("/")) {
            //文件夹
            Result result = HbaseServiceImpl.getRow(connection, HosUtil.getDirTableName(bucket), key);
            if (!result.isEmpty()) {
                //读取文件夹的基础属性转换为HosObjectSummary类
                return this.dirObjectToSummary(result, bucket, key);
            }
            return null;
        } else {
            //文件
            //父目录对应的seqId
            String parentDir = key.substring(0, key.lastIndexOf("/") + 1);
            String seqId = this.getDirSeqId(bucket, parentDir);
            if (seqId == null) {
                //文件不存在
                return null;
            }

            String objKey = seqId + "_" + key.substring(key.lastIndexOf("/") + 1);
            Result result = HbaseServiceImpl.getRow(connection, HosUtil.getObjTableName(bucket), objKey);
            if (result.isEmpty()) {
                return null;
            }

            return this.resultToObjectSummary(result, bucket, key);
        }
    }

    @Override
    public List<HosObjectSummary> list(String bucket, String startKey, String endKey) throws IOException {
        String dir1 = startKey.substring(0, startKey.lastIndexOf("/") + 1).trim();
        if (dir1.length() == 0) {
            dir1 = "/";
        }
        String dir2 = endKey.substring(0, startKey.lastIndexOf("/") + 1).trim();
        if (dir2.length() == 0) {
            dir2 = "/";
        }
        String name1 = startKey.substring(startKey.lastIndexOf("/") + 1);
        String name2 = endKey.substring(startKey.lastIndexOf("/") + 1);
        String seqId = this.getDirSeqId(bucket, dir1);
        //查询dir1中大于name1的全部文件
        List<HosObjectSummary> keys = new ArrayList<>();
        if (seqId != null && name1.length() > 0) {
            byte[] max = Bytes.createMaxByteArray(100);
            byte[] tail = Bytes.add(Bytes.toBytes(seqId), max);
            if (dir1.equals(dir2)) {
                tail = (seqId + "_" + name2).getBytes();
            }
            byte[] start = (seqId + "_" + name1).getBytes();
            ResultScanner scanner1 = HbaseServiceImpl.scanner(connection, HosUtil.getObjTableName(bucket), start, tail);
            Result result = null;
            while ((result = scanner1.next()) != null) {
                HosObjectSummary summary = this.resultToObjectSummary(result, bucket, dir1);
                keys.add(summary);
            }
            if (scanner1 != null) {
                scanner1.close();
            }
        }
        //startkey~endkey之间的全部目录
        ResultScanner scanner2 = HbaseServiceImpl.scanner(connection, HosUtil.getDirTableName(bucket), startKey, endKey);
        Result result = null;
        while ((result = scanner2.next()) != null) {
            String seqId2 = Bytes.toString(result.getValue(HosUtil.DIR_META_CF_BYTES,
                    HosUtil.DIR_SEQID_COLUMN));
            if (seqId2 == null) {
                continue;
            }
            String dir = Bytes.toString(result.getRow());
            keys.add(dirObjectToSummary(result, bucket, dir));
            getDirAllFiles(bucket, dir, seqId2, keys, endKey);
        }
        if (scanner2 != null) {
            scanner2.close();
        }
        Collections.sort(keys);
        return keys;
    }

    //浏览目录
    @Override
    public ObjectListResult listDir(String bucket, String dir, String start, int maxCount) throws IOException {
        //查询目录表
        start = Strings.nullToEmpty(start);
        Get get = new Get(Bytes.toBytes(dir));
        get.addFamily(HosUtil.DIR_SUBDIR_CF_BYTES);
        if (!Strings.isNullOrEmpty(start)) {
            get.setFilter(new QualifierFilter(CompareFilter.CompareOp.GREATER_OR_EQUAL, new BinaryComparator(Bytes.toBytes(start))));
        }
        Result result = HbaseServiceImpl.get(connection, HosUtil.getObjTableName(bucket), get);
        List<HosObjectSummary> subDirs = null;
        if (!result.isEmpty()) {
            subDirs = new ArrayList<>();
            for (Cell cell : result.rawCells()) {
                HosObjectSummary summary = new HosObjectSummary();
                byte[] qualifierBytes = new byte[cell.getQualifierLength()];
                CellUtil.copyQualifierTo(cell, qualifierBytes, 0);
                String name = Bytes.toString(qualifierBytes);
                summary.setKey(dir + name + "/");
                summary.setName(name);
                summary.setLastModifyTime(cell.getTimestamp());
                summary.setFileType("");
                summary.setBucket(bucket);
                summary.setLength(0);
                subDirs.add(summary);
                if (subDirs.size() > maxCount + 1) {
                    break;
                }
            }
        }

        //查询文件表
        String dirSeq = this.getDirSeqId(bucket, dir);
        byte[] objStart = Bytes.toBytes(dirSeq + "_" + start);
        Scan objScan = new Scan();
        objScan.setStartRow(objStart);
        objScan.setRowPrefixFilter(Bytes.toBytes(dirSeq + "_"));
        objScan.setMaxResultsPerColumnFamily(maxCount + 1);
        objScan.addFamily(HosUtil.OBJ_META_CF_BYTES);
        ResultScanner resultScanner = HbaseServiceImpl.getScanner(connection, HosUtil.getObjTableName(bucket), objScan);

        List<HosObjectSummary> objectSummaries = new ArrayList<HosObjectSummary>();
        Result result1 = null;
        while (objectSummaries.size() < maxCount + 2 && (result1 = resultScanner.next()) != null) {
            HosObjectSummary hosObjectSummary = this.resultToObjectSummary(result1, bucket, dir);
            objectSummaries.add(hosObjectSummary);
        }

        if (resultScanner != null) {
            resultScanner.close();
        }

        if (subDirs != null && subDirs.size() > 0) {
            objectSummaries.addAll(subDirs);
        }

        //返回条数maxcount
        Collections.sort(objectSummaries);
        ObjectListResult listResult = new ObjectListResult();
        HosObjectSummary nextMarkerObj =
                objectSummaries.size() > maxCount ? objectSummaries.get(objectSummaries.size() - 1)
                        : null;
        if (nextMarkerObj != null) {
            listResult.setNextMarker(nextMarkerObj.getKey());
        }
        if (objectSummaries.size() > maxCount) {
            objectSummaries = objectSummaries.subList(0, maxCount);
        }
        listResult.setMaxKeyNumber(maxCount);
        if (objectSummaries.size() > 0) {
            listResult.setMinKey(objectSummaries.get(0).getKey());
            listResult.setMaxKey(objectSummaries.get(objectSummaries.size() - 1).getKey());
        }
        listResult.setObjectCount(objectSummaries.size());
        listResult.setHosObjectSummaries(objectSummaries);
        listResult.setBucket(bucket);

        return listResult;
    }

    @Override
    public ObjectListResult listByPrefix(String bucket, String dir, String start, String prefix, int maxCount) throws IOException {
        if (start == null) {
            start = "";
        }
        FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
        filterList.addFilter(new ColumnPrefixFilter(prefix.getBytes()));
        if (start.length() > 0) {
            filterList.addFilter(new QualifierFilter(CompareFilter.CompareOp.GREATER_OR_EQUAL,
                    new BinaryComparator(Bytes.toBytes(start))));
        }
        int maxCount1 = maxCount + 2;
        Result dirResult = HbaseServiceImpl
                .getRow(connection, HosUtil.getDirTableName(bucket), dir, filterList);
        List<HosObjectSummary> subDirs = null;
        if (!dirResult.isEmpty()) {
            subDirs = new ArrayList<>();
            for (Cell cell : dirResult.rawCells()) {
                HosObjectSummary summary = new HosObjectSummary();
                byte[] qualifierBytes = new byte[cell.getQualifierLength()];
                CellUtil.copyQualifierTo(cell, qualifierBytes, 0);
                String name = Bytes.toString(qualifierBytes);
                summary.setKey(dir + name + "/");
                summary.setName(name);
                summary.setLastModifyTime(cell.getTimestamp());
                summary.setFileType("");
                summary.setBucket(bucket);
                summary.setLength(0);
                subDirs.add(summary);
                if (subDirs.size() >= maxCount1) {
                    break;
                }
            }
        }

        String dirSeq = this.getDirSeqId(bucket, dir);
        byte[] objStart = Bytes.toBytes(dirSeq + "_" + start);
        Scan objScan = new Scan();
        objScan.setRowPrefixFilter(Bytes.toBytes(dirSeq + "_" + prefix));
        objScan.setFilter(new PageFilter(maxCount + 1));
        objScan.setStartRow(objStart);
        objScan.setMaxResultsPerColumnFamily(maxCount1);
        objScan.addFamily(HosUtil.OBJ_META_CF_BYTES);
        logger.info("scan start: " + Bytes.toString(objStart) + " - ");
        ResultScanner objScanner = HbaseServiceImpl.getScanner(connection, HosUtil.getObjTableName(bucket), objScan);
        List<HosObjectSummary> objectSummaryList = new ArrayList<>();
        Result result = null;
        while (objectSummaryList.size() < maxCount1 && (result = objScanner.next()) != null) {
            HosObjectSummary summary = this.resultToObjectSummary(result, bucket, dir);
            objectSummaryList.add(summary);
        }
        if (objScanner != null) {
            objScanner.close();
        }
        logger.info("scan complete: " + Bytes.toString(objStart) + " - ");
        if (subDirs != null && subDirs.size() > 0) {
            objectSummaryList.addAll(subDirs);
        }
        Collections.sort(objectSummaryList);
        ObjectListResult listResult = new ObjectListResult();
        HosObjectSummary nextMarkerObj =
                objectSummaryList.size() > maxCount ? objectSummaryList.get(objectSummaryList.size() - 1)
                        : null;
        if (nextMarkerObj != null) {
            listResult.setNextMarker(nextMarkerObj.getKey());
        }
        if (objectSummaryList.size() > maxCount) {
            objectSummaryList = objectSummaryList.subList(0, maxCount);
        }
        listResult.setMaxKeyNumber(maxCount);
        if (objectSummaryList.size() > 0) {
            listResult.setMinKey(objectSummaryList.get(0).getKey());
            listResult.setMaxKey(objectSummaryList.get(objectSummaryList.size() - 1).getKey());
        }
        listResult.setObjectCount(objectSummaryList.size());
        listResult.setHosObjectSummaries(objectSummaryList);
        listResult.setBucket(bucket);

        return listResult;
    }

    @Override
    public HosObject getObject(String bucket, String key) throws IOException {
        //判断key是文件还是目录
        if (key.endsWith("/")) {
            Result result = HbaseServiceImpl.getRow(connection, HosUtil.getDirTableName(bucket), key);
            if (result.isEmpty()) {
                return null;
            }

            ObjectMetaData objectMetaData = new ObjectMetaData();
            objectMetaData.setBucket(bucket);
            objectMetaData.setKey(key);
            objectMetaData.setLength(0);
            objectMetaData.setLastModifyTime(result.rawCells()[0].getTimestamp());

            HosObject hosObject = new HosObject();
            hosObject.setObjectMetaData(objectMetaData);

            return hosObject;
        } else {
            //文件
            //父目录对应的seqId
            String parentDir = key.substring(0, key.lastIndexOf("/") + 1);
            String seqId = this.getDirSeqId(bucket, parentDir);
            if (seqId == null) {
                //文件不存在
                return null;
            }
            String name = key.substring(key.lastIndexOf("/") + 1);
            String objKey = seqId + "_" + key.substring(key.lastIndexOf("/") + 1);
            Result result = HbaseServiceImpl.getRow(connection, HosUtil.getObjTableName(bucket), objKey);
            if (result.isEmpty()) {
                return null;
            }

            HosObject object = new HosObject();

            //读取文件内容,从hdfs中或者hbase中读取
            if (result.containsNonEmptyColumn(HosUtil.OBJ_CONTENT_CF_BYTES,
                    HosUtil.OBJ_CONT_COLUMN)) {
                ByteArrayInputStream bas = new ByteArrayInputStream(
                        result.getValue(HosUtil.OBJ_CONTENT_CF_BYTES,
                                HosUtil.OBJ_CONT_COLUMN));
                object.setInputStream(bas);
            } else {
                String fileDir = HosUtil.FILE_STORE_ROOT + "/" + bucket + "/" + seqId;
                InputStream inputStream = this.fileStore.openFile(fileDir, name);
                object.setInputStream(inputStream);
            }
            long len = Bytes.toLong(result.getValue(HosUtil.OBJ_META_CF_BYTES,
                    HosUtil.OBJ_LEN_COLUMN));
            ObjectMetaData metaData = new ObjectMetaData();
            metaData.setBucket(bucket);
            metaData.setKey(key);
            metaData.setLastModifyTime(result.rawCells()[0].getTimestamp());
            metaData.setLength(len);
            metaData.setFileType(Bytes.toString(result.getValue(HosUtil.OBJ_META_CF_BYTES,
                    HosUtil.OBJ_FILETYPE_COLUMN)));
            byte[] b = result
                    .getValue(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_PROPS_COLUMN);
            if (b != null) {
                metaData.setAttrs(JsonUtil.fromJson(Map.class, Bytes.toString(b)));
            }
            object.setObjectMetaData(metaData);
            return object;
        }
    }

    @Override
    public void deleteObject(String bucket, String key) throws Exception {
        //判断key是目标还是文件
        if (key.endsWith("/")) {
            //判断目录是否为空
            if (!isDirEmpty(bucket, key)) {
                throw new HosServerException(ErrorCodes.ERROR_HBASE, "dir is not empty,can't delete");
            } else {
                //获取锁
                InterProcessMutex lock = null;
                String lockey = key.replace("/", "_");
                lock = new InterProcessMutex(zkClient, "/hos/" + bucket + "/" + lockey);
                lock.acquire();

                //从父目录删除数据
                //"/hos/dir/dir2/"
                String parentDir = key.substring(0, key.lastIndexOf("/"));
                String name = parentDir.substring(key.lastIndexOf("/") + 1);
                if (name.length() > 0) {
                    //删除子目录
                    String parent = key.substring(0, key.lastIndexOf(name));
                    HbaseServiceImpl.deleteColumn(connection, HosUtil.getDirTableName(bucket), parent, HosUtil.DIR_SUBDIR_CF, name);
                }

                //删除父目录
                HbaseServiceImpl.deleteRow(connection, HosUtil.getDirTableName(bucket), key);
                //释放锁
                lock.release();
                return;
            }
        } else {
            //删除文件
            //删除父目录
            String dir = key.substring(0, key.lastIndexOf("/") + 1);
            String name = key.substring(key.lastIndexOf("/") + 1);
            String seqId = this.getDirSeqId(bucket, dir);
            String objKey = seqId + "_" + name;

            Get get = new Get(objKey.getBytes());
            get.addColumn(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_LEN_COLUMN);
            Result result = HbaseServiceImpl.get(connection, HosUtil.getObjTableName(bucket), get);
            if (result.isEmpty()) {
                return;
            }

            long len = Bytes.toLong(result.getValue(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_LEN_COLUMN));
            if (len > HosUtil.FILE_STORE_THRESHOLD) {
                //从hdfs中删除
                String fileDir = HosUtil.FILE_STORE_ROOT + "/" + bucket + "/" + seqId;
                fileStore.deleteFile(fileDir, name);
            } else {
                HbaseServiceImpl.deleteRow(connection, HosUtil.getObjTableName(bucket), objKey);
            }
        }
    }

    //判断目录是否存在
    private boolean dirExist(String bucket, String key) {
        return HbaseServiceImpl.existsRow(connection, HosUtil.getDirTableName(bucket), key);
    }

    //获取某个目录对应的seqId
    private String getDirSeqId(String bucket, String key) {
        Result result = HbaseServiceImpl.getRow(connection, HosUtil.getDirTableName(bucket), key);
        if (result.isEmpty()) {
            return null;
        } else {
            return Bytes.toString(result.getValue(HosUtil.DIR_META_CF_BYTES, HosUtil.DIR_SEQID_COLUMN));
        }
    }

    private String putDir(String bucket, String key) {
        if (dirExist(bucket, key)) {
            return null;
        }

        //注意:创建目录时候要加锁,防止有用户正在删除(父)目录
        //从zk获取锁
        InterProcessMutex lock = null;
        try {
            String lockey = key.replace("/", "_");
            //获取锁的本质就是在zookeeper上创建一个临时节点(path)
            lock = new InterProcessMutex(zkClient, "/hos/" + bucket + key);
            //对目录进行加锁,其他线程就无法对该目录进行修改操作了
            lock.acquire();

            //'/dir1/dir2/...'
            String dir1 = key.substring(0, key.lastIndexOf("/"));
            String name = dir1.substring(dir1.lastIndexOf("/"));

            if (name.length() > 0) {
                String parentDir = dir1.substring(0, dir1.lastIndexOf("/"));
                if (!dirExist(bucket, parentDir)) {
                    //创建父目录
                    this.putDir(bucket, parentDir);
                }

                //在父目录添加sub列族,添加子项
                Put put = new Put(Bytes.toBytes(parentDir));
                put.addColumn(HosUtil.DIR_SUBDIR_CF_BYTES, Bytes.toBytes(name), Bytes.toBytes("1"));
                HbaseServiceImpl.putRow(connection, bucket, put);
            }

            String seqId = getDirSeqId(bucket, key);
            String hash = seqId == null ? makeDirSeqId(bucket) : seqId;
            Put put = new Put(key.getBytes());
            put.addColumn(HosUtil.DIR_META_CF_BYTES, HosUtil.DIR_SEQID_COLUMN, Bytes.toBytes(hash));

            HbaseServiceImpl.putRow(connection, bucket, put);

            return hash;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (lock != null) {
                try {
                    lock.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    private String makeDirSeqId(String bucket) throws IOException {
        long v = HbaseServiceImpl.incrementColumnValue(connection, HosUtil.BUCKET_DIR_SEQ_TABLE, bucket,
                HosUtil.BUCKET_DIR_SEQ_CF_BYTES, HosUtil.BUCKET_DIR_SEQ_COLUMN, 1);
        return String.format("%da%d", v % 64, v);
    }

    //将目录表中的cf基础属性转换成HosObjectSummary
    private HosObjectSummary dirObjectToSummary(Result result, String bucket, String dir) {
        HosObjectSummary summary = new HosObjectSummary();
        String id = Bytes.toString(result.getRow());
        summary.setId(id);
        summary.setAttrs(new HashMap<>(0));
        if (dir.length() > 1) {
            summary.setName(dir.substring(dir.lastIndexOf("/") + 1));
        } else {
            summary.setName("");
        }
        summary.setBucket(bucket);
        summary.setKey(dir);
        summary.setLastModifyTime(result.rawCells()[0].getTimestamp());
        summary.setLength(0);
        summary.setFileType("");
        return summary;
    }


    //将文件表中的cf基础属性转换成HosObjectSummary
    private HosObjectSummary resultToObjectSummary(Result result, String bucket, String dir)
            throws IOException {
        HosObjectSummary summary = new HosObjectSummary();
        long timestamp = result.rawCells()[0].getTimestamp();
        summary.setLastModifyTime(timestamp);
        String id = new String(result.getRow());
        summary.setId(id);
        String name = id.split("_", 2)[1];
        String key = dir + name;
        summary.setKey(key);
        summary.setName(name);
        summary.setBucket(bucket);
        String s = Bytes.toString(result.getValue(HosUtil.OBJ_META_CF_BYTES,
                HosUtil.OBJ_PROPS_COLUMN));
        if (s != null) {
            summary.setAttrs(JsonUtil.fromJson(Map.class, s));
        }
        summary.setLength(Bytes.toLong(result.getValue(HosUtil.OBJ_META_CF_BYTES,
                HosUtil.OBJ_LEN_COLUMN)));
        summary
                .setFileType(Bytes.toString(result.getValue(HosUtil.OBJ_META_CF_BYTES,
                        HosUtil.OBJ_FILETYPE_COLUMN)));

        return summary;
    }

    private boolean isDirEmpty(String bucket, String dir) throws IOException {
        return listDir(bucket, dir, null, 2).getHosObjectSummaries().size() == 0;
    }

    private void getDirAllFiles(String bucket, String dir, String seqId, List<HosObjectSummary> keys,
                                String endKey) throws IOException {

        byte[] max = Bytes.createMaxByteArray(100);
        byte[] tail = Bytes.add(Bytes.toBytes(seqId), max);
        if (endKey.startsWith(dir)) {
            String endKeyLeft = endKey.replace(dir, "");
            String fileNameMax = endKeyLeft;
            if (endKeyLeft.indexOf("/") > 0) {
                fileNameMax = endKeyLeft.substring(0, endKeyLeft.indexOf("/"));
            }
            tail = Bytes.toBytes(seqId + "_" + fileNameMax);
        }

        Scan scan = new Scan(Bytes.toBytes(seqId), tail);
        scan.setFilter(HosUtil.OBJ_META_SCAN_FILTER);
        ResultScanner scanner = HbaseServiceImpl
                .getScanner(connection, HosUtil.getObjTableName(bucket), scan);
        Result result = null;
        while ((result = scanner.next()) != null) {
            HosObjectSummary summary = this.resultToObjectSummary(result, bucket, dir);
            keys.add(summary);
        }
        if (scanner != null) {
            scanner.close();
        }
    }

}
