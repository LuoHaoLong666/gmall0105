package com.atguigu.gmall.passport.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.util.HttpclientUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.util.HashMap;
import java.util.Map;

public class TestOauth2 {

    public static String getCode(){
        // 1、获得授权码
        // 2902778671
        // http://passport.gmall.com:8085/vlogin  授权成功跳转的页面
        String s1 = HttpclientUtil.doGet("https://api.weibo.com/oauth2/authorize?client_id=2902778671&response_type=code&redirect_uri=http://passport.gmall.com:8085/vlogin");

        System.out.println(s1);

        // 在第一步和第二步返回回调地址之间,有一个用户操作授权的过程,就会跳转到指定的回调地址上面去


        // 授权码: 12747d714ee1dcbec58c135323e5236c
        // 2、返回授权码到回调地址
        String s2 = "http://passport.gmall.com:8085/vlogin?code=12747d714ee1dcbec58c135323e5236c";
        return null;
    }

    public static String getAccess_token(){
        // 3、换取access_token
        // 授权码: 12747d714ee1dcbec58c135323e5236c
        // client-secret: 89fd3b9261ccacb34bdba3000d894a25
        String s3 = "https://api.weibo.com/oauth2/access_token";  // ?client_id=2902778671&client_secret=89fd3b9261ccacb34bdba3000d894a25&grant_type=authorization_code&redirect_uri=http://passport.gmall.com:8085/vlogin&code=12747d714ee1dcbec58c135323e5236c

        Map<String,String> paramMap = new HashMap<>();
        paramMap.put("client_id","2902778671");
        paramMap.put("client_secret","89fd3b9261ccacb34bdba3000d894a25");
        paramMap.put("grant_type","authorization_code");
        paramMap.put("redirect_uri","http://passport.gmall.com:8085/vlogin");
        paramMap.put("code","e2cdeb0824c18b56a03c28f51cf8c751");  // 授权码有效期内可以使用,每新生成一次授权码,说明用户对第三方数据进行重新授权,之前的access_token和授权码全部过期


        String access_token_json = HttpclientUtil.doPost(s3, paramMap);

        Map<String,String> access_map = JSON.parseObject(access_token_json,Map.class);

        System.out.println(access_map.get("access_token"));
        System.out.println(access_map.get("uid"));
        return access_map.get("access_token");
    }

    public static Map<String, String> getUser_info(){
        // 4、用access_token查询用户信息

        String s4 = "https://api.weibo.com/2/users/show.json?access_token=2.00UDI4UGp1l8KDb13adb2caeIedUJC&uid=5945599584";
        String user_json = HttpclientUtil.doGet(s4);

        Map<String,String> user_map = JSON.parseObject(user_json,Map.class);

        System.out.println(user_map.get("1"));

        return user_map;
    }

    public static void main(String[] args) {
        getUser_info();

    }


}
