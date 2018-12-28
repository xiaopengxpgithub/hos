package com.xp.hos.service;

import com.xp.hos.hbase.HbaseServiceImpl;
import com.xp.hos.pojo.HosObject;
import com.xp.hos.pojo.HosObjectSummary;
import com.xp.hos.pojo.ObjectListResult;
import com.xp.hos.utils.HosUtil;
import org.apache.hadoop.hbase.client.Connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public interface IHosStore {


    public void createBucketStore(String bucket) throws IOException;

    public void deleteBucketStore(String bucket) throws IOException;

    public void createSeqIdTable();

    public void put(String bucket, String key, ByteBuffer content, long length,
                    String mediaType, Map<String, String> attrs) throws Exception;

    public HosObjectSummary getSummary(String bucket, String key) throws IOException;

    public List<HosObjectSummary> list(String bucket, String startKey, String endKey) throws IOException;

    //浏览数据
    public ObjectListResult listDir(String bucket, String dir, String start, int maxCount) throws IOException;
    public ObjectListResult listByPrefix(String bucket, String dir, String start, String prefix, int max) throws IOException;

    //下载文件
    public HosObject getObject(String bucket, String key) throws IOException;
    //删除文件
    public void deleteObject(String bucket,String key) throws Exception;
}
