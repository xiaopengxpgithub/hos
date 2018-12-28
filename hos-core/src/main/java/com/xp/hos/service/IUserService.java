package com.xp.hos.service;

import com.xp.hos.pojo.UserInfo;

public interface IUserService {

    boolean addUserInfo(UserInfo userInfo);

    boolean deleteUser(String userId);

    boolean updateUserInfo(String userId, String password,
                       String detail);

    UserInfo getUserInfoById(String userId);

    UserInfo getUserInfoByName(String userName);

    UserInfo getUserInfoByUsernamePassword(String userName,
                                           String password);

}
