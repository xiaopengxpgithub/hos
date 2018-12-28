package com.xp.hos.utils;

import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * hbase,hdfs相关属性
 */
public class HosUtil {

    //目录表/文件表名称
    public  static final String DIR_TABLE_PREFIX = "hos_dir_";
    public static final String OBJ_TABLE_PREFIX = "hos_obj_";

    //目录表列族
    public  static final String DIR_META_CF = "cf";
    public static final byte[] DIR_META_CF_BYTES = DIR_META_CF.getBytes();
    public static final String DIR_SUBDIR_CF = "sub";
    public  static final byte[] DIR_SUBDIR_CF_BYTES = DIR_SUBDIR_CF.getBytes();

    //文件表列族
    public static final String OBJ_META_CF = "cf";
    public static final byte[] OBJ_META_CF_BYTES = OBJ_META_CF.getBytes();
    public static final String OBJ_CONTENT_CF = "c";
    public  static final byte[] OBJ_CONTENT_CF_BYTES = OBJ_CONTENT_CF.getBytes();

    //列名
    public static final byte[] DIR_SEQID_COLUMN = "u".getBytes();
    public static final byte[] OBJ_CONT_COLUMN = "c".getBytes();
    public static final byte[] OBJ_LEN_COLUMN = "l".getBytes();
    public static final byte[] OBJ_PROPS_COLUMN = "p".getBytes();
    public static final byte[] OBJ_FILETYPE_COLUMN = "t".getBytes();

    //hdfs上hos根目录
    public static final String FILE_STORE_ROOT = "/hos";

    //文件大小限定(如果大于这个值,将这个文件存储到hdfs)
    public static final int FILE_STORE_THRESHOLD = 20 * 1024 * 1024;

    //存储hbase的目录表的squid的信息的表
    public static final String BUCKET_DIR_SEQ_TABLE = "hos_dir_seq";
    public  static final String BUCKET_DIR_SEQ_CF = "cf";
    public static final byte[] BUCKET_DIR_SEQ_CF_BYTES = BUCKET_DIR_SEQ_CF.getBytes();
    public static final byte[] BUCKET_DIR_SEQ_COLUMN = "s".getBytes();

    //获取目录表名
    public static String getDirTableName(String bucketName) {
        return DIR_TABLE_PREFIX + bucketName;
    }
    //获取目录表所有的列族
    public static String[] getDirColumnFamily(){
        return new String[]{DIR_META_CF,DIR_SUBDIR_CF};
    }

    //获取文件表名
    public static String getObjTableName(String bucketName) {
        return OBJ_TABLE_PREFIX + bucketName;
    }
    //获取文件表所有的列族
    public static String[] getObjColumnFamily(){
        return new String[]{OBJ_META_CF,OBJ_CONTENT_CF};
    }

    //文件表预先分区
    public static final byte[][] OBJ_REGIONS=new byte[][]{
            Bytes.toBytes("1"),
            Bytes.toBytes("4"),
            Bytes.toBytes("7")
    };

    public static final FilterList OBJ_META_SCAN_FILTER = new FilterList(FilterList.Operator.MUST_PASS_ONE);

    static {
        try {
            byte[][] qualifiers = new byte[][]{HosUtil.DIR_SEQID_COLUMN,
                    HosUtil.OBJ_LEN_COLUMN,
                    HosUtil.OBJ_FILETYPE_COLUMN};
            for (byte[] b : qualifiers) {
                Filter filter = new QualifierFilter(CompareFilter.CompareOp.EQUAL,
                        new BinaryComparator(b));
                OBJ_META_SCAN_FILTER.addFilter(filter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
