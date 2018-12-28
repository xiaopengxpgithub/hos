package com.xp.hos.test;

import com.xp.hos.datasource.HosDatasourceConfig;
import com.xp.hos.pojo.BucketModel;
import com.xp.hos.pojo.UserInfo;
import com.xp.hos.service.IBucketService;
import com.xp.hos.service.IUserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@Import(HosDatasourceConfig.class)
@PropertySource("classpath:application.properties")
@ComponentScan("com.xp.hos.*")
@MapperScan("com.xp.hos.*")
public class BucketTest {


    @Autowired
    @Qualifier("bucketServiceImpl")
    private IBucketService iBucketService;

    @Autowired
    @Qualifier("userServiceImpl")
    private IUserService iUserService;

    @Test
    public void test02(){
        BucketModel bucketModel=iBucketService.getBucketByName("bucket1");
        System.out.println(bucketModel);
    }

    @Test
    public void test01() {
        UserInfo userInfo=iUserService.getUserInfoByName("张三");
        System.out.println(userInfo);
        iBucketService.addBucket(userInfo,"bucket1","this is bucket1");
    }
}
