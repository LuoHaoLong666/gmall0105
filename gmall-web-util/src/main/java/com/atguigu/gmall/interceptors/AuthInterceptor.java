package com.atguigu.gmall.interceptors;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import sun.net.www.http.HttpClient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 拦截代码
        // 判断被拦截的请求的访问的方法的注解(是否是需要拦截的)
        HandlerMethod hm = (HandlerMethod)handler;  // handler的一个子类
        LoginRequired methodAnnotation = hm.getMethodAnnotation(LoginRequired.class);  // 使用反射方式来得到方法的注解，通过类获得类的整体信息，也可以通过类的整体信息获得类。

        StringBuffer url = request.getRequestURL();

        // 是否拦截
        if(methodAnnotation==null){
            return true;
        }

        String token = "";  // 获取token,login是使用jwt制作token

        // 如果新的token和老的token都有的话，就说明该用户过期了，需要重新登录
        String oldToken = CookieUtil.getCookieValue(request,"oldToken",true);
        if(StringUtils.isNotBlank(oldToken)){
            token = oldToken;
        }

        String newToken= request.getParameter("token");
        if(StringUtils.isNotBlank(newToken)){
            token = newToken;
        }

        // 调用认证中心进行验证
        String success = "fail";
        Map<String,String> successMap = new HashMap<>();
        if(StringUtils.isNotBlank(token)){
            String ip = request.getHeader("x-forwarded-for");  // 通过nginx转发的客户端ip
            if(StringUtils.isBlank(ip)){
                ip = request.getRemoteAddr(); // 从request中获取ip地址
                if(StringUtils.isBlank(ip)){
                    ip = "127.0.0.1";
                }
            }
            String  successJson = HttpclientUtil.doGet("http://passport.gmall.com:8085/verify?token="+token+"&currentIp="+ip); // 海关将token发送给认证中心验证

            successMap = JSON.parseObject(successJson, Map.class);

            success = successMap.get("status");  // 代表返回状态

        }


        // 是否必须登录
        boolean loginSuccess = methodAnnotation.loginSuccess();  // 获得该请求是否必须登录成功

        if(loginSuccess){
          if(!success.equals("success")) {
              // 重定向回passport登录
              StringBuffer requestURL = request.getRequestURL();
              response.sendRedirect("http://passport.gmall.com:8085/index?ReturnUrl=" + requestURL);
              return false;
          }
              // 验证通过，覆盖cookie中的token
              request.setAttribute("memberId",successMap.get("memberId"));
              request.setAttribute("nickname",successMap.get("nickname")); // 为了在应用里面，直接拿到用户的信息

              // 验证通过,覆盖cookie中的token
              if(StringUtils.isNotBlank(token)){
                  CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
              }

        }else{
            // 没有登录也能用，但是必须验证
            if(success.equals("success")) {
                // 需要将token携带的用户信息写入
                request.setAttribute("memberId",successMap.get("memberId"));
                request.setAttribute("nickname",successMap.get("nickname"));

                // 验证通过,覆盖cookie中的token
                if(StringUtils.isNotBlank(token)){
                    CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
                }

            }

        }

        return true;
    }
}