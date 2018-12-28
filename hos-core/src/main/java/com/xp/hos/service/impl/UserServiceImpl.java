package com.xp.hos.service.impl;

import com.google.common.base.Strings;
import com.xp.hos.mapper.TokenInfoMapper;
import com.xp.hos.mapper.UserInfoMapper;
import com.xp.hos.pojo.TokenInfo;
import com.xp.hos.pojo.UserInfo;
import com.xp.hos.service.IAuthService;
import com.xp.hos.service.IUserService;
import com.xp.hos.utils.CoreUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service("userServiceImpl")
public class UserServiceImpl implements IUserService {


    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    @Qualifier("AuthServiceImpl")
    private IAuthService iAuthService;

    @Transactional
    @Override
    public boolean addUserInfo(UserInfo userInfo) {
        userInfoMapper.addUserInfo(userInfo);

        //添加用户信息的同时需要添加一个token信息
        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setToken(userInfo.getUserId());
        tokenInfo.setActive(true);
        tokenInfo.setExpireTime(7);
        tokenInfo.setRefreshTime(new Date());
        tokenInfo.setCreator(CoreUtil.SYSTEM_USER);
        tokenInfo.setCreateTime(new Date());

        iAuthService.addToken(tokenInfo);

        return true;
    }

    @Transactional
    @Override
    public boolean deleteUser(String userId) {
        userInfoMapper.deleteUser(userId);

        //还要删除token信息
        iAuthService.deleteToken(userId);
        iAuthService.deleteAuthByToken(userId);

        return true;
    }

    @Transactional
    @Override
    public boolean updateUserInfo(String userId, String password, String detail) {
        userInfoMapper.updateUserInfo(userId, Strings.isNullOrEmpty(password) ? null : CoreUtil.getPasswordMD5(password),
                Strings.isNullOrEmpty(detail) ? null : detail);
        return true;
    }

    @Override
    public UserInfo getUserInfoById(String userId) {
        return userInfoMapper.getUserInfoById(userId);
    }

    @Override
    public UserInfo getUserInfoByName(String userName) {
        return userInfoMapper.getUserInfoByName(userName);
    }

    @Override
    public UserInfo getUserInfoByUsernamePassword(String userName, String password) {
        return userInfoMapper.getUserInfoByUsernamePassword(userName, CoreUtil.getPasswordMD5(password));
    }
}
