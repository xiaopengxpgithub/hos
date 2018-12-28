package com.xp.hos.mapper;

import com.xp.hos.pojo.TokenInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;

import java.util.Date;
import java.util.List;

@Mapper
public interface TokenInfoMapper {

    public void addToken(@Param("tokenInfo") TokenInfo tokenInfo);

    public void deleteToken(@Param("token") String token);

    public void updateToken(@Param("token") String token,
                            @Param("expireTime") int expireTime,
                            @Param("active") int active);

    public void refreshToken(@Param("token") String token,
                             @Param("refreshTime") Date refreshTime);

    @ResultMap("tokenInfoMapper")
    public TokenInfo getTokenInfoByToken(@Param("token") String token);

    @ResultMap("tokenInfoMapper")
    public List<TokenInfo> getTokenInfos(@Param("creator") String creator);
}
