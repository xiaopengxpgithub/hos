package com.xp.hos.rest;

import com.xp.hos.exception.ErrorCodes;
import com.xp.hos.pojo.ServiceAuth;
import com.xp.hos.pojo.SystemRole;
import com.xp.hos.pojo.TokenInfo;
import com.xp.hos.pojo.UserInfo;
import com.xp.hos.security.ContextUtil;
import com.xp.hos.service.IAuthService;
import com.xp.hos.service.IUserService;
import com.xp.hos.utils.CoreUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Controller
@RequestMapping(value = "/hos/v1/sys")
public class ManagerController extends BaseController {

    private static Logger logger=LoggerFactory.getLogger(ManagerController.class);

    @Autowired
    @Qualifier("userServiceImpl")
    private IUserService iUserService;

    @Autowired
    @Qualifier("AuthServiceImpl")
    private IAuthService iAuthService;


    //添加用户
    @ResponseBody
    @RequestMapping(value = "/user",method = RequestMethod.POST)
    public Object addUser(@RequestParam("userName") String userName,@RequestParam("password") String password,
                          @RequestParam(value = "detail",required = false,defaultValue = "") String detail,
                          @RequestParam(value = "role",required = false,defaultValue = "USER") String role){

        //判断当前用户是否有创建用户的权限
        UserInfo userInfo=ContextUtil.getCurrentUser();

        logger.info(userInfo.toString());

        if (iOperationAccessController.checkSystemRole(userInfo.getSystemRole(),SystemRole.valueOf(role))){
            //创建userinfo实例对象
            UserInfo userInfo1=new UserInfo();
            userInfo1.setUserId(CoreUtil.getUUIDStr())
                    .setUserName(userName)
                    .setSystemRole(SystemRole.valueOf(role))
                    .setDetail(detail)
                    .setPassword(password)
                    .setCreateTime(new Date());

            iUserService.addUserInfo(userInfo1);

            return getResult("success");
        }

        return getError(ErrorCodes.ERROR_PERMISSION_DENIED,"创建用户失败!");
    }

    //删除用户
    @ResponseBody
    @RequestMapping(value = "/deleteUser/{userId}",method = RequestMethod.DELETE)
    public Object deleteUser(@PathVariable(name = "userId") String userId){
        UserInfo userInfo=ContextUtil.getCurrentUser();
        //判断当前用户是否有删除用户的权限
        if (iOperationAccessController.checkSystemRole(userInfo.getSystemRole(),userId)){
            iUserService.deleteUser(userId);
            return getResult("success");
        }else {
            return getError(ErrorCodes.ERROR_PERMISSION_DENIED,"删除用户失败!");
        }
    }

    //添加token
    @ResponseBody
    @RequestMapping(value = "/createToken",method = RequestMethod.POST)
    public Object createToken(@RequestParam(value = "expireTime" ,required = false,defaultValue = "7") String expireTime,
                              @RequestParam(value = "isActive",required = false,defaultValue = "true") String isActive){
        UserInfo userInfo=ContextUtil.getCurrentUser();
        if (!userInfo.getSystemRole().equals(SystemRole.VISITOR)){
            //如果当前用户不是游客,那么他可以创建token
            TokenInfo tokenInfo=new TokenInfo();
            tokenInfo.setCreator(userInfo.getUserName())
                    .setToken(CoreUtil.getUUIDStr())
                    .setExpireTime(Integer.parseInt(expireTime))
                    .setActive(Boolean.parseBoolean(isActive))
                    .setRefreshTime(new Date())
                    .setCreateTime(new Date());

            iAuthService.addToken(tokenInfo);

            return getResult("success");
        }

        return getError(ErrorCodes.ERROR_PERMISSION_DENIED,"添加token失败!");
    }

    //删除token
    @ResponseBody
    @RequestMapping(value = "/deleteToken/{token}",method = RequestMethod.DELETE)
    public Object deleteToken(@PathVariable(name = "token") String token){
        UserInfo userInfo=ContextUtil.getCurrentUser();
        if (iOperationAccessController.checkTokenOwner(userInfo.getUserName(),token)){
            iAuthService.deleteToken(token);
            return getResult("success");
        }

        return getError(ErrorCodes.ERROR_PERMISSION_DENIED,"删除token失败!");
    }

    //授权
    @ResponseBody
    @RequestMapping(value = "/createAuth",method = RequestMethod.POST)
    public Object createAuth(ServiceAuth serviceAuth){
        UserInfo userInfo=ContextUtil.getCurrentUser();
        //如果当前用户是token的创建者,且是bucket的创建者,那么往授权表中添加记录
        if (iOperationAccessController.checkTokenOwner(userInfo.getUserName(),serviceAuth.getTargetToken())
                && iOperationAccessController.checkBucketOwner(userInfo.getUserName(),serviceAuth.getBucketName())){

            iAuthService.addAuth(serviceAuth);

            return getResult("success");
        }

        return getError(ErrorCodes.ERROR_PERMISSION_DENIED,"授权失败");
    }

    //取消授权
    @ResponseBody
    @RequestMapping(value = "/cancelAuth",method = RequestMethod.POST)
    public Object cancelAuth(@RequestParam(value = "bucket") String bucket,
                             @RequestParam(value = "token") String token){
        UserInfo userInfo=ContextUtil.getCurrentUser();
        //如果当前用户是token的创建者,且是bucket的创建者,那么往授权表中添加记录
        if (iOperationAccessController.checkTokenOwner(userInfo.getUserName(),token)
                && iOperationAccessController.checkBucketOwner(userInfo.getUserName(),bucket)){

            iAuthService.deleteAuth(bucket,token);

            return getResult("success");
        }

        return getError(ErrorCodes.ERROR_PERMISSION_DENIED,"取消授权失败");
    }

}
