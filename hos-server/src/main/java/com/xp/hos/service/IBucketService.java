package com.xp.hos.service;

import com.xp.hos.pojo.BucketModel;
import com.xp.hos.pojo.UserInfo;

import java.util.List;

public interface IBucketService {

    boolean addBucket(UserInfo userInfo,String bucketName,String detail);

    boolean deleteBucket(String bucketName);

    boolean updateBucket(String bucketName, String detail);

    BucketModel getBucketById(String bucketId);

    BucketModel getBucketByName(String bucketName);

    List<BucketModel> getBucketModelsByCreator(String creator);

    List<BucketModel> getUserAuthBuckets(String token);
}
