package com.xp.hos.test;

import com.xp.hos.datasource.HosDatasourceConfig;
import com.xp.hos.pojo.ServiceAuth;
import com.xp.hos.pojo.SystemRole;
import com.xp.hos.pojo.TokenInfo;
import com.xp.hos.pojo.UserInfo;
import com.xp.hos.service.IAuthService;
import com.xp.hos.service.IUserService;
import com.xp.hos.utils.CoreUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@Import(HosDatasourceConfig.class)
@PropertySource("classpath:application.properties")
@ComponentScan("com.xp.hos.*")
@MapperScan("com.xp.hos.*")
public class HosCoreTest {

    @Autowired
    @Qualifier("userServiceImpl")
    private IUserService userService;

    @Autowired
    @Qualifier("AuthServiceImpl")
    private IAuthService iAuthService;

    @Test
    public void test06(){
        ServiceAuth serviceAuth=iAuthService.getAuth("bucket1","49cf500e6e1c4e409ef9cc7db7772c01");
        System.out.println(serviceAuth);
    }

    @Test
    public void test05(){
        ServiceAuth serviceAuth=new ServiceAuth();
        serviceAuth.setTargetToken("49cf500e6e1c4e409ef9cc7db7772c01");
        serviceAuth.setBucketName("bucket1");
        serviceAuth.setAuthTime(new Date());

        iAuthService.addAuth(serviceAuth);
    }

    @Test
    public void test04(){
        List<TokenInfo> tokenInfos =iAuthService.getTokenInfos("tom");
        for (TokenInfo tokenInfo : tokenInfos) {
            System.out.println(tokenInfo);
        }
    }

    @Test
    public void test03(){
        TokenInfo tokenInfo=new TokenInfo();
        tokenInfo.setToken(CoreUtil.getUUIDStr());
        tokenInfo.setCreator("tom");
        tokenInfo.setCreateTime(new Date());
        tokenInfo.setRefreshTime(new Date());
        tokenInfo.setExpireTime(7);
        tokenInfo.setActive(true);

        iAuthService.addToken(tokenInfo);
    }

    @Test
    public void test02(){
        UserInfo userInfo=userService.getUserInfoById("28fb420e233f48b39442351d07467320");
        System.out.println(userInfo);
     }

    @Test
    public void test01(){
        UserInfo userInfo=new UserInfo();
        userInfo.setUserId(CoreUtil.getUUIDStr())
                .setUserName("张三")
                .setSystemRole(SystemRole.ADMIN)
                .setPassword(CoreUtil.getPasswordMD5("123456"))
                .setDetail("this is zhangsan")
                .setCreateTime(new Date());

        userService.addUserInfo(userInfo);
    }
}
