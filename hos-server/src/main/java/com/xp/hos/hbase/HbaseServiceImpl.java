package com.xp.hos.hbase;

import com.xp.hos.exception.ErrorCodes;
import com.xp.hos.exception.HosServerException;
import com.xp.hos.utils.HosUtil;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class HbaseServiceImpl {

    //1.创建表
    public static boolean createTable(Connection connection, String tableName,
                                      String[] cfs, byte[][] splitKeys) {
        try {
            HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
            if (admin.tableExists(tableName)) {
                //如果表存在返回false;
                return false;
            }

            //创建表
            HTableDescriptor tableDescriptor = new HTableDescriptor(TableName.valueOf(tableName));
            Arrays.stream(cfs).forEach(cf -> {
                HColumnDescriptor hColumnDescriptor = new HColumnDescriptor(cf);
                hColumnDescriptor.setMaxVersions(1);

                tableDescriptor.addFamily(hColumnDescriptor);
            });

            //创建表,预先分区
            admin.createTable(tableDescriptor, splitKeys);
        } catch (Exception e) {
            e.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "create table " + tableName + " error");
        }

        return true;
    }

    //2.删除表
    public static boolean deleteTable(Connection connection, String tableName) {
        try {
            HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
            admin.disableTable(tableName);
            admin.deleteTable(tableName);

        } catch (Exception e) {
            e.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "delete table " + tableName + " error");
        }

        return true;
    }

    //3.删除列族
    public static boolean deleteColumnFamily(Connection connection, String tableName, String cf) {
        try {
            HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
            admin.deleteColumn(tableName, cf);
        } catch (Exception e) {
            e.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "delete columfamily " + tableName + " error");
        }

        return true;
    }

    //4.删除列
    public static boolean deleteColumn(Connection connection, String tableName, String rowKey,String cf, String columnName) {
        Delete delete=new Delete(rowKey.getBytes());
        delete.addColumn(cf.getBytes(),columnName.getBytes());

        return delete(connection,tableName,delete);
    }

    public static boolean delete(Connection connection, String tableName,Delete delete) {
        try {
            Table table=connection.getTable(TableName.valueOf(tableName));
            table.delete(delete);
        } catch (Exception e) {
            e.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "delete column " + tableName + " error");
        }

        return true;
    }

    //5.删除行
    public static boolean deleteRow(Connection connection,String tableName,String rowKey){
        Delete delete=new Delete(rowKey.getBytes());

        return delete(connection,tableName,delete);
    }

    //6.读取行数据
    public static Result getRow(Connection connection,String tableName,String rowKey){
        Get get=new Get(rowKey.getBytes());
        return get(connection,tableName,get);
    }

    public static Result get(Connection connection,String tableName,Get get){
        try {
            Table table=connection.getTable(TableName.valueOf(tableName));
            return table.get(get);
        }catch (Exception e){
            e.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE,"get row data error");
        }
    }

    public static Result getRow(Connection connection, String tableName, String row,
                                FilterList filterList) {
        Result rs;
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            Get g = new Get(Bytes.toBytes(row));
            g.setFilter(filterList);
            rs = table.get(g);
        } catch (IOException e) {
            String msg = String
                    .format("get row from table=%s error. msg=%s", tableName, e.getMessage());
            throw new HosServerException(ErrorCodes.ERROR_HBASE, msg);
        }
        return rs;
    }

    //7.获取scanner
    public static ResultScanner getScanner(Connection connection, String tableName, String startKey, String endKey, FilterList filterList){
        Scan scan=new Scan();
        scan.setStartRow(startKey.getBytes());
        scan.setStartRow(endKey.getBytes());
        scan.setFilter(filterList);
        scan.setCaching(1000);

        return getScanner(connection,tableName,scan);
    }

    public static ResultScanner getScanner(Connection connection,String tableName,Scan scan){
        try {
            Table table =connection.getTable(TableName.valueOf(tableName));
            return table.getScanner(scan);
        }catch (Exception e){
            e.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE,"scanner data error");
        }
    }
    /**
     * scanner.
     */
    public static ResultScanner scanner(Connection connection, String tableName, String startRowKey,
                                        String stopRowKey) {
        return scanner(connection, tableName, Bytes.toBytes(startRowKey), Bytes.toBytes(stopRowKey));
    }

    /**
     * scanner.
     */
    public static ResultScanner scanner(Connection connection, String tableName, byte[] startRowKey,
                                        byte[] stopRowKey) {
        ResultScanner results = null;
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            Scan scan = new Scan();
            scan.setStartRow(startRowKey);
            scan.setStopRow(stopRowKey);
            scan.setCaching(1000);
            results = table.getScanner(scan);
        } catch (IOException e) {
            String msg = String
                    .format("scan table=%s error. msg=%s", tableName, e.getMessage());
            throw new HosServerException(ErrorCodes.ERROR_HBASE, msg);
        }
        return results;
    }

    //8.插入行
    public static boolean putRow(Connection connection,String tableName,String rowKey,String cf,String columnName,String data){
        Put put =new Put(rowKey.getBytes());
        put.addColumn(cf.getBytes(),columnName.getBytes(),data.getBytes());
        return putRow(connection,tableName,put);
    }

    public static boolean putRow(Connection connection,String tableName,Put put){
        try {
            Table table=connection.getTable(TableName.valueOf(tableName));
            table.put(put);
        }catch (Exception e) {
            e.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "put data error");
        }
        return true;
    }

    //9.批量插入
    public static boolean batchPut(Connection connection, String tableName, List<Put> puts){
        try {
            Table table=connection.getTable(TableName.valueOf(tableName));
            table.put(puts);

        }catch (Exception e){
            e.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE,"batch put error");
        }
        return true;
    }

    //生成目录表的seqId
    public static long incrementColumnValue(Connection connection,String tableName,
                                            String rowKey,byte[] cf,
                                            byte[] columnName,int num){
        try {
            Table table=connection.getTable(TableName.valueOf(tableName));
            return table.incrementColumnValue(rowKey.getBytes(),cf,columnName,num);
        }catch (Exception e){
            e.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE,"increment column value error");
        }
    }

    public static boolean existsRow(Connection connection, String tableName, String row) {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            Get g = new Get(Bytes.toBytes(row));
            return table.exists(g);
        } catch (Exception e) {
            String msg = String
                    .format("check exists row from table=%s error. msg=%s", tableName, e.getMessage());
            throw new HosServerException(ErrorCodes.ERROR_HBASE, msg);
        }
    }
}
