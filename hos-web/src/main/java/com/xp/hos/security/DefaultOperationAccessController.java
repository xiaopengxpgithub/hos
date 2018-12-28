package com.xp.hos.security;

import com.xp.hos.pojo.*;
import com.xp.hos.service.IAuthService;
import com.xp.hos.service.IBucketService;
import com.xp.hos.service.IUserService;
import com.xp.hos.utils.CoreUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("defaultOperationAccessController")
public class DefaultOperationAccessController implements IOperationAccessController {

    @Autowired
    @Qualifier("AuthServiceImpl")
    private IAuthService iAuthService;

    @Autowired
    @Qualifier("userServiceImpl")
    private IUserService iUserService;

    @Autowired
    @Qualifier("bucketServiceImpl")
    private IBucketService iBucketService;

    @Override
    public UserInfo checkLogin(String userName, String password) {
        UserInfo userInfo = iUserService.getUserInfoByName(userName);
        if (userInfo == null) {
            //用户不存在
            return null;
        }

        return userInfo.getPassword().equals(CoreUtil.getPasswordMD5(password)) ? userInfo : null;
    }

    @Override
    public boolean checkSystemRole(SystemRole systemRole1, SystemRole systemRole2) {
        if (systemRole1.equals(SystemRole.SUPERADMIN)) {
            //如果用户是超级管理员,那么他拥有操作权限
            return true;
        }

        //如果用户是管理员且用户是正常用户,也可以操作系统
        return systemRole1.equals(SystemRole.ADMIN) && systemRole2.equals(SystemRole.USER);
    }

    @Override
    public boolean checkSystemRole(SystemRole systemRole, String userId) {
        if (systemRole.equals(SystemRole.SUPERADMIN)){
            return true;
        }

        UserInfo userInfo=iUserService.getUserInfoById(userId);
        return userInfo.getSystemRole().equals(SystemRole.SUPERADMIN) && userInfo.getSystemRole().equals(SystemRole.USER);
    }

    //检查该用户是否为该token的创建者
    @Override
    public boolean checkTokenOwner(String userName, String token) {
        TokenInfo tokenInfo=iAuthService.getTokenInfoByToken(token);

        return tokenInfo.getCreator().equals(userName);
    }

    @Override
    public boolean checkBucketOwner(String userName, String bucket) {
        BucketModel bucketModel= iBucketService.getBucketByName(bucket);

        return bucketModel.getCreator().equals(userName);
    }

    //检查token(/userid)是否有操作bucket的权限
    @Override
    public boolean checkPermission(String token, String bucket) {
        if (iAuthService.checkToken(token)){
            //获取某个token对应bucket的权限
            ServiceAuth serviceAuth=iAuthService.getAuth(bucket,token);
            if (serviceAuth==null ){
                return false;
            }

            return true;
        }
        return false;
    }
}
