package com.xp.hos.serivce;

import com.xp.hos.pojo.BucketModel;
import com.xp.hos.pojo.HosObjectSummary;
import com.xp.hos.pojo.PutRequest;

import java.io.IOException;
import java.util.List;

/**
 * sdk客户端
 */
public interface IHosClient {

    public void createBucket(String bucketName, String detail) throws IOException;

    public void deleteBucket(String bucketName) throws IOException;

    public void deleteBucket(String bucketName, String key) throws IOException;

    public List<BucketModel> listBuckets() throws IOException;

    public HosObjectSummary getObjectSummery(String bucket,String key) throws IOException;

    public void putObject(PutRequest putRequest);

    public void putObject(String bucket, String key);

    public void putObject(String bucket, String key, byte[] content, String fileType);
}
