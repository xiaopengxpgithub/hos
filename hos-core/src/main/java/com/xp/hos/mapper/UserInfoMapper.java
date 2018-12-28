package com.xp.hos.mapper;

import com.xp.hos.pojo.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;

@Mapper
public interface UserInfoMapper {


    void addUserInfo(@Param("userInfo") UserInfo userInfo);

    int deleteUser(@Param("userId") String userId);

    int updateUserInfo(@Param("userId") String userId, @Param("password") String password,
                       @Param("detail") String detail);

    @ResultMap("userInfoResultMap")
    UserInfo getUserInfoById(@Param("userId") String userId);

    @ResultMap("userInfoResultMap")
    UserInfo getUserInfoByName(@Param("userName") String userName);

    @ResultMap("userInfoResultMap")
    UserInfo getUserInfoByUsernamePassword(@Param("userName") String userName,
                                           @Param("password") String password);
}
