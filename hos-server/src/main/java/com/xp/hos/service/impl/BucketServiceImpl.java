package com.xp.hos.service.impl;

import com.xp.hos.mapper.BucketMapper;
import com.xp.hos.pojo.BucketModel;
import com.xp.hos.pojo.ServiceAuth;
import com.xp.hos.pojo.UserInfo;
import com.xp.hos.service.IAuthService;
import com.xp.hos.service.IBucketService;
import com.xp.hos.utils.CoreUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service("bucketServiceImpl")
public class BucketServiceImpl implements IBucketService {

    @Autowired
    private BucketMapper bucketMapper;

    @Autowired
    @Qualifier("AuthServiceImpl")
    private IAuthService iAuthService;

    @Transactional
    @Override
    public boolean addBucket(UserInfo userInfo,String bucketName,String detail) {
        BucketModel bucketModel=new BucketModel();
        bucketModel.setBucketId(CoreUtil.getUUIDStr())
                .setBucketName(bucketName)
                .setCreateTime(new Date())
                .setCreator(userInfo.getUserName())
                .setDetail(detail);

        bucketMapper.addBucket(bucketModel);


        //添加bucket信息的同时还要添加serviceAuth记录
        ServiceAuth serviceAuth=new ServiceAuth();
        serviceAuth.setAuthTime(new Date())
                .setBucketName(bucketName)
                .setTargetToken(userInfo.getUserId());
        iAuthService.addAuth(serviceAuth);

        return true;
    }

    @Transactional
    @Override
    public boolean deleteBucket(String bucketName) {
        bucketMapper.deleteBucket(bucketName);

        //删除bucket的同时还要删除serviceauth记录
        iAuthService.deleteAuthByBucket(bucketName);

        return true;
    }

    @Override
    public boolean updateBucket(String bucketName, String detail) {
        bucketMapper.updateBucket(bucketName,detail);
        return true;
    }

    @Override
    public BucketModel getBucketById(String bucketId) {
        return bucketMapper.getBucket(bucketId);
    }

    @Override
    public BucketModel getBucketByName(String bucketName) {
        return bucketMapper.getBucketByName(bucketName);
    }

    @Override
    public List<BucketModel> getBucketModelsByCreator(String creator) {
        return bucketMapper.getBucketModelsByCreator(creator);
    }

    @Override
    public List<BucketModel> getUserAuthBuckets(String token) {
        return bucketMapper.getUserAuthBuckets(token);
    }
}
