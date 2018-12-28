package com.xp.hos;

import com.xp.hos.pojo.SystemRole;
import com.xp.hos.pojo.UserInfo;
import com.xp.hos.service.IHosStore;
import com.xp.hos.service.IUserService;
import com.xp.hos.utils.CoreUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 对象初始化类
 */
@Component
public class ApplicationInitialization implements ApplicationRunner {

    @Autowired
    @Qualifier("userServiceImpl")
    private IUserService iUserService;

    @Autowired
    @Qualifier("iHosStore")
    private IHosStore iHosStore;

    //程序启动的时候运行
    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        //创建管理员用户
        UserInfo userInfo = iUserService.getUserInfoByName(CoreUtil.SYSTEM_USER);
        if (userInfo == null) {
            //系统第一次启动,要添加系统管理员
            UserInfo userInfo1 = new UserInfo();
            userInfo1.setCreateTime(new Date());
            userInfo1.setUserName(CoreUtil.SYSTEM_USER);
            userInfo1.setUserId(CoreUtil.getUUIDStr());
            userInfo1.setPassword("123456");
            userInfo1.setSystemRole(SystemRole.SUPERADMIN);
            userInfo1.setDetail("this is a super admin");

            iUserService.addUserInfo(userInfo);
        }

        //创建seqid table
        iHosStore.createSeqIdTable();
    }
}
