package com.xp.hos.hdfs;

import java.io.IOException;
import java.io.InputStream;

/**
 * hdfs操作接口
 */
public interface IHDFSServer {

    public void saveFile(String dir, String name, InputStream inputStream,
                         long size, short replication) throws IOException;

    public void deleteFile(String dir,String name) throws IOException;

    public InputStream openFile(String dir,String name) throws IOException;

    public void mkDir(String dir) throws IOException;

    public void deleteDir(String dir) throws IOException;
}
