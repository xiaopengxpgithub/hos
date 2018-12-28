package com.xp.hos.mapper;

import com.xp.hos.pojo.ServiceAuth;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;

@Mapper
public interface ServiceAuthMapper {

    public void addAuth(@Param("auth") ServiceAuth serviceAuth);

    public void deleteAuth(@Param("bucket") String bucket, @Param("token") String token);

    public void deleteAuthByToken(@Param("token") String token);

    public void deleteAuthByBucket(@Param("bucket") String bucket);

    @ResultMap("serviceAuthMap")
    public ServiceAuth getAuth(@Param("bucket") String bucket,@Param("token") String token);
}
