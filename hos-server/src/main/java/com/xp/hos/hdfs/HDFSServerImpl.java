package com.xp.hos.hdfs;

import com.xp.hos.exception.ErrorCodes;
import com.xp.hos.exception.HosServerException;
import com.xp.hos.utils.HosConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class HDFSServerImpl implements IHDFSServer {

    private static Logger logger = LoggerFactory.getLogger(HDFSServerImpl.class);

    private FileSystem fileSystem;
    private long defaultBlockSize = 128 * 1024 * 1024;
    private long initBlockSize = defaultBlockSize / 2;

    //初始化hdfs相关信息
    public HDFSServerImpl() throws Exception {
        //1.读取hdfs相关配置
        HosConfig hosConfig = HosConfig.getConfig();
        String confDir = hosConfig.getString("hadoop.conf.dir");
        String hdfsUri = hosConfig.getString("hadoop.uri");

        //2.通过配置,获取一个filesystem实例对象
        Configuration configuration = new Configuration();
        configuration.addResource(confDir + "/hdfs-site.xml");
        configuration.addResource(confDir + "/core-site.xml");

        fileSystem = FileSystem.get(new URI(hdfsUri), configuration);
    }

    @Override
    public void saveFile(String dir, String name, InputStream inputStream, long size, short replication) throws IOException {
        //1.判断目录是否存在,如果不存在则创建
        Path path = new Path(dir);
        try {
            if (!fileSystem.exists(path)) {
                boolean result = fileSystem.mkdirs(path, FsPermission.getDirDefault());
                logger.info("create dir path:" + path);

                if (!result) {
                    //目录创建失败,抛出异常
                    throw new HosServerException(ErrorCodes.ERROR_HDFS, "create dir:" + path + " error");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //2.上传文件
        Path filePath = new Path(dir + "/" + name);
        //如果文件的大小小于等于64M,那么将hfds的blocksize设置为64M,否则设置为128M
        long blockSize=size<=initBlockSize?64:defaultBlockSize;
        //文件流,将文件写入到hfds上
        FSDataOutputStream outputStream=fileSystem.create(path,true,512*1024,replication,blockSize);

        try {
            //文件权限
            fileSystem.setPermission(path,FsPermission.getFileDefault());
            byte[] buffer=new byte[512*1024];
            int len=-1;
            while ((len=inputStream.read(buffer))>0){
                //将数据读取到buffer缓冲区中,再将缓冲区中的数据写入到hfds
                outputStream.write(buffer,0,len);
            }
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            inputStream.close();
            outputStream.close();
        }
    }

    @Override
    public void deleteFile(String dir, String name) throws IOException {
        //false表示是否递归删除
        fileSystem.delete(new Path(dir+"/"+name),false);
    }

    @Override
    public InputStream openFile(String dir, String name) throws IOException {
        return fileSystem.open(new Path(dir+"/"+name));
    }

    @Override
    public void mkDir(String dir) throws IOException {
        fileSystem.mkdirs(new Path(dir));
    }

    @Override
    public void deleteDir(String dir) throws IOException {
        //删除目录时递归删除子目录
        fileSystem.delete(new Path(dir),true);
    }
}
