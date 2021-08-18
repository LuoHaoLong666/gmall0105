package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.manage.service.UserService;
import com.atguigu.gmall.util.HttpclientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    UserService userService;

    @RequestMapping("vlogin") // 为了AuthInterceptor里的调用认证中心而服务的,需要返回状态和相关信息。
    public String vlogin(HttpServletRequest request,String code){

        // 授权码换取access_token
        // 3、换取access_token
        // 授权码: 12747d714ee1dcbec58c135323e5236c
        // client-secret: 89fd3b9261ccacb34bdba3000d894a25
        String s3 = "https://api.weibo.com/oauth2/access_token";

        Map<String,String> paramMap = new HashMap<>();
        paramMap.put("client_id","2902778671");
        paramMap.put("client_secret","89fd3b9261ccacb34bdba3000d894a25");
        paramMap.put("grant_type","authorization_code");
        paramMap.put("redirect_uri","http://passport.gmall.com:8085/vlogin");
        paramMap.put("code",code);  // 授权码有效期内可以使用,每新生成一次授权码,说明用户对第三方数据进行重新授权,之前的access_token和授权码全部过期
        String access_token_json = HttpclientUtil.doPost(s3, paramMap);

        Map<String,Object> access_map = JSON.parseObject(access_token_json,Map.class);

        // access_token换取用户信息
        String uid = (String)access_map.get("uid");
        String access_token = (String)access_map.get("access_token");

        String show_user_url = "https://api.weibo.com/2/users/show.json?access_token="+access_token+"&uid="+uid;
        String user_json = HttpclientUtil.doGet(show_user_url);

        Map<String,Object> user_map = JSON.parseObject(user_json,Map.class);

        // 将用户信息保存数据库,用户类型设置为微博用户
        UmsMember umsMember = new UmsMember();
        umsMember.setSourceType("2");
        umsMember.setAccessCode(code);
        umsMember.setAccessToken(access_token);
        umsMember.setSourceUid((String)user_map.get("idstr"));
        umsMember.setCity((String)user_map.get("location"));
        umsMember.setNickname((String)user_map.get("screen_name"));
        String g = "0";
        String gender = (String)user_map.get("gender");
        if(gender.equals("m")){
            g = "1";
        }
        umsMember.setGender(g);

        UmsMember umsCheck = new UmsMember();
        umsCheck.setSourceUid(umsMember.getSourceUid());;
        UmsMember umsMemberCheck = userService.checkOauthUser(umsCheck);  // 检查该用户(社交用户以前是否登录过系统)

        if(umsMemberCheck==null){
            umsMember = userService.addOauthUser(umsMember);
        }else{
            umsMember = umsMemberCheck;
        }


        // 生成jwt的token,并且重定向到首页,携带token
        String token = null;
        String memberId = umsMember.getId(); // rpc的主键返回策略失效,会是null值,所以需要将addOauthUser的返回值改为umsMember,rpc的主键返回策略才会生效
        String nickname = umsMember.getNickname();
        token = getToken(request, memberId, nickname, userService);


        return "redirect:http://search.gmall.com:8083/index?token="+token;

    }


    @RequestMapping("verify") // 为了AuthInterceptor里的调用认证中心而服务的,需要返回状态和相关信息。
    @ResponseBody
    public String verify(String token,String currentIp){

        // 通过jwt校验token的真假
        Map<String,String> map = new HashMap<>();



        Map<String, Object> decode = JwtUtil.decode(token, "2019gmall0105", currentIp);

        if(decode!=null){
            map.put("status","success");
            map.put("memberId",(String)decode.get("memberId"));
            map.put("nickname",(String)decode.get("nickname"));
        }else{
            map.put("status","fail");
        }

        return JSON.toJSONString(map);  // 将map转换为JSON形式的String字符串
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UmsMember umsMember,HttpServletRequest request){
        String token = "";

        // 调用用户服务验证用户名和密码,并且获取到该用户的相关信息
        UmsMember umsMemberLogin = userService.login(umsMember);

        if(umsMemberLogin!=null){ // umsMemberLogin不为空
            // 登录成功

            // 用jwt制作token
            String memberId = umsMemberLogin.getId();
            String nickname = umsMemberLogin.getNickname();
//            Map<String,Object> userMap = new HashMap<>();  // 将私人部分存储起来
//            userMap.put("memberId",memberId);
//            userMap.put("nickname",nickname);
//
//            String ip = request.getHeader("x-forwarded-for");  // 通过nginx转发的客户端ip
//            if(StringUtils.isBlank(ip)){
//                ip = request.getRemoteAddr(); // 从request中获取ip地址
//                if(StringUtils.isBlank(ip)){
//                    ip = "127.0.0.1";
//                }
//            }
//
//
//            // 需要按照设计的算法对参数进行加密后,生成token,安全算法问题
//            token = JwtUtil.encode("2019gmall0105", userMap, ip);
//
//
//            // 将token存入redis一份，以防cookie被篡改,所以为了安全问题,需要在redis存一份
//
//            userService.addUserToken(token,memberId);

            token = getToken(request,memberId,nickname,userService); // 获得token

        }else{
            // 登录失败
            token = "fail";  // 给前端返回一个标识符就可以了。
        }

        return token;
    }

    public static String getToken(HttpServletRequest request,String memberId,String nickname,UserService userService){  // 获得token
        Map<String,Object> userMap = new HashMap<>();  // 将私人部分存储起来
        userMap.put("memberId",memberId); // 是保存数据库后主键返回策略生成的id
        userMap.put("nickname",nickname);

        String ip = request.getHeader("x-forwarded-for");  // 通过nginx转发的客户端ip
        if(StringUtils.isBlank(ip)){
            ip = request.getRemoteAddr(); // 从request中获取ip地址
            if(StringUtils.isBlank(ip)){
                ip = "127.0.0.1";
            }
        }


        // 需要按照设计的算法对参数进行加密后,生成token,安全算法问题
        String token = JwtUtil.encode("2019gmall0105", userMap, ip);


        // 将token存入redis一份，以防cookie被篡改,所以为了安全问题,需要在redis存一份

        userService.addUserToken(token,memberId);

        return token;
    }

    @RequestMapping("index")
    public String index(String ReturnUrl, ModelMap map){
        if(StringUtils.isNotBlank(ReturnUrl)) {
            map.put("ReturnUrl", ReturnUrl);
        }
        return "index";
    }
}
