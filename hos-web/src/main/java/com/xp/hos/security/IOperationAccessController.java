package com.xp.hos.security;


import com.xp.hos.pojo.SystemRole;
import com.xp.hos.pojo.UserInfo;

/**
 * 用户访问控制
 */
public interface IOperationAccessController {

    public UserInfo checkLogin(String userName, String password);

    public boolean checkSystemRole(SystemRole systemRole1, SystemRole systemRole2);

    public boolean checkSystemRole(SystemRole systemRole, String userId);

    public boolean checkTokenOwner(String userName, String token);

    public boolean checkBucketOwner(String userName, String bucket);

    public boolean checkPermission(String token, String bucket);

}
