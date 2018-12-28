package com.xp.hos.service.impl;

import com.xp.hos.mapper.ServiceAuthMapper;
import com.xp.hos.mapper.TokenInfoMapper;
import com.xp.hos.pojo.ServiceAuth;
import com.xp.hos.pojo.TokenInfo;
import com.xp.hos.service.IAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service("AuthServiceImpl")
public class AuthServiceImpl implements IAuthService {

    @Autowired
    private TokenInfoMapper tokenInfoMapper;

    @Autowired
    private ServiceAuthMapper serviceAuthMapper;

    @Transactional
    @Override
    public boolean addAuth(ServiceAuth serviceAuth) {
        serviceAuthMapper.addAuth(serviceAuth);
        return true;
    }

    @Transactional
    @Override
    public boolean deleteAuth(String bucket, String token) {
        serviceAuthMapper.deleteAuth(bucket, token);
        return true;
    }

    @Transactional
    @Override
    public boolean deleteAuthByToken(String token) {
        serviceAuthMapper.deleteAuthByToken(token);
        return true;
    }

    @Transactional
    @Override
    public boolean deleteAuthByBucket(String bucket) {
        serviceAuthMapper.deleteAuthByBucket(bucket);
        return true;
    }

    @Override
    public ServiceAuth getAuth(String bucket, String token) {
        return serviceAuthMapper.getAuth(bucket, token);
    }

    @Transactional
    @Override
    public boolean addToken(TokenInfo tokenInfo) {
        tokenInfoMapper.addToken(tokenInfo);
        return true;
    }

    @Transactional
    @Override
    public boolean deleteToken(String token) {
        tokenInfoMapper.deleteToken(token);

        //删除token信息的同时,还要删除Auth授权信息
        serviceAuthMapper.deleteAuthByToken(token);

        return true;
    }

    @Transactional
    @Override
    public boolean updateToken(String token, int expireTime, int active) {
        tokenInfoMapper.updateToken(token, expireTime, active);
        return true;
    }

    @Transactional
    @Override
    public boolean refreshToken(String token, Date refreshTime) {
        tokenInfoMapper.refreshToken(token, refreshTime);
        return true;
    }

    @Override
    public TokenInfo getTokenInfoByToken(String token) {
        return tokenInfoMapper.getTokenInfoByToken(token);
    }

    @Override
    public List<TokenInfo> getTokenInfos(String creator) {
        return tokenInfoMapper.getTokenInfos(creator);
    }

    @Override
    public boolean checkToken(String token) {
        TokenInfo tokenInfo = tokenInfoMapper.getTokenInfoByToken(token);
        if (tokenInfo == null) {
            return false;
        }

        if (tokenInfo.isActive()) {
            //检验token是否过期
            Date now = new Date();

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(tokenInfo.getRefreshTime());
            calendar.add(Calendar.DATE, tokenInfo.getExpireTime());

            //如果当前时间在过期时间范围之内,则token有效
            return now.before(calendar.getTime());
        }

        return false;
    }
}
