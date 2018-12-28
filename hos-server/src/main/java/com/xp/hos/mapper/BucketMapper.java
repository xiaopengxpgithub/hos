package com.xp.hos.mapper;

import com.xp.hos.pojo.BucketModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;

import java.util.List;

@Mapper
public interface BucketMapper {

    void addBucket(@Param("bucket") BucketModel bucketModel);

    void deleteBucket(@Param("bucketName") String bucketName);

    void updateBucket(@Param("bucketName") String bucketName,
                      @Param("detail") String detail);

    @ResultMap("bucketModelResultMap")
    BucketModel getBucket(@Param("bucketId") String bucketId);

    @ResultMap("bucketModelResultMap")
    BucketModel getBucketByName(@Param("bucketName") String bucketName);

    @ResultMap("bucketModelResultMap")
    List<BucketModel> getBucketModelsByCreator(@Param("creator") String creator);

    @ResultMap("bucketModelResultMap")
    List<BucketModel> getUserAuthBuckets(@Param("token") String token);
}
