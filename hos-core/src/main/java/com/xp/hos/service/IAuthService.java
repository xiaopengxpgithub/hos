package com.xp.hos.service;

import com.xp.hos.pojo.ServiceAuth;
import com.xp.hos.pojo.TokenInfo;

import java.util.Date;
import java.util.List;

public interface IAuthService {

    public boolean addAuth(ServiceAuth serviceAuth);

    public boolean deleteAuth(String bucket, String token);

    public boolean deleteAuthByToken(String token);

    public boolean deleteAuthByBucket(String bucket);

    public ServiceAuth getAuth(String bucket, String token);

    public boolean addToken(TokenInfo tokenInfo);

    public boolean deleteToken(String token);

    public boolean updateToken(String token, int expireTime, int active);

    public boolean refreshToken(String token, Date refreshTime);

    public TokenInfo getTokenInfoByToken(String token);

    public List<TokenInfo> getTokenInfos(String creator);

    public boolean checkToken(String token);
}
