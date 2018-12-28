package com.xp.hos.rest;

import com.google.common.base.Strings;
import com.xp.hos.exception.ErrorCodes;
import com.xp.hos.pojo.UserInfo;
import com.xp.hos.security.ContextUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
public class LoginController extends BaseController {


    @ResponseBody
    @RequestMapping(value = "/logout")
    public Object logout(String username,String password,HttpSession session){
        session.removeAttribute(ContextUtil.SESSION_KEY);

        return getResult("success");
    }

    @ResponseBody
    @RequestMapping(value = "/loginPost")
    public Object loginPost(String username, String password, HttpSession session){
        if (Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(password)){
            return getError(ErrorCodes.ERROR_PERMISSION_DENIED,"用户名或者密码不能为空");
        }

        UserInfo userInfo =iOperationAccessController.checkLogin(username,password);
        if (userInfo!=null){
            //将userid存储到session中
            session.setAttribute(ContextUtil.SESSION_KEY,userInfo.getUserId());

            return getResult("success");
        }else{
            return getError(ErrorCodes.ERROR_PERMISSION_DENIED,"用户名或密码错误,登陆失败");
        }
    }
}
