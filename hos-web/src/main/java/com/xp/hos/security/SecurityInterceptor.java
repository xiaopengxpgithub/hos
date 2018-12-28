package com.xp.hos.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xp.hos.pojo.SystemRole;
import com.xp.hos.pojo.TokenInfo;
import com.xp.hos.pojo.UserInfo;
import com.xp.hos.service.IAuthService;
import com.xp.hos.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

/**
 * 拦截器
 */
@Component
public class SecurityInterceptor implements HandlerInterceptor {

    @Autowired
    @Qualifier("AuthServiceImpl")
    private IAuthService authService;

    @Autowired
    @Qualifier("userServiceImpl")
    private IUserService iUserService;

    private Cache<String, UserInfo> userInfoCache = CacheBuilder.newBuilder().expireAfterWrite(20, TimeUnit.MINUTES)
            .build();

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {
        if (httpServletRequest.getRequestURI().contains("/loginPost")) {
            //如果是登陆请求,直接放行,执行handler方法
            return true;
        }

        //登陆验证
        String token = "";
        HttpSession session = httpServletRequest.getSession();
        if (session.getAttribute(ContextUtil.SESSION_KEY) != null) {
            token = session.getAttribute(ContextUtil.SESSION_KEY).toString();
        } else {
            //用户还没有登陆,当前用户是否为游客
            token = httpServletRequest.getHeader("X-Auth-Token");
        }

        //验证token
        TokenInfo tokenInfo = authService.getTokenInfoByToken(token);
        if (tokenInfo == null) {
            //token是假的,回到登陆页面
            String url = "/loginPost";
            httpServletResponse.sendRedirect(url);
            return false;
        }

        //用户已经登陆过了,判断用户登陆信息是否过期/是否存在
        UserInfo userInfo=userInfoCache.getIfPresent(tokenInfo.getToken());
        if (userInfo==null){
            //用户是新登录的,将token信息添加到cache中
            userInfo=iUserService.getUserInfoById(token);

            if (userInfo==null){
                //该token是游客登陆的
                userInfo=new UserInfo();
                userInfo.setSystemRole(SystemRole.VISITOR);
                userInfo.setUserName("Visitor");
                userInfo.setDetail("this is a visitor");
                userInfo.setUserId(token);
            }
            //将用户信息添加到缓存中
            userInfoCache.put(tokenInfo.getToken(),userInfo);
        }

        ContextUtil.setCurrentUser(userInfo);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {

    }
}
