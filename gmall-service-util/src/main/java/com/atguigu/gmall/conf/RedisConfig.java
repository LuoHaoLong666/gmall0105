package com.atguigu.gmall.conf;

import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

        //读取配置文件中的redis的ip地址
        @Value("${spring.redis.host:disabled}")  // 如果没有设置值，默认值
        private String host;

        @Value("${spring.redis.port:0}")   // 如果没有设置值，默认值
        private int port;

        @Value("${spring.redis.database:0}")   // 如果没有设置值，默认值
        private int database;

        @Bean
        public RedisUtil getRedisUtil(){
            if(host.equals("disabled")){
                return null;
            }
            RedisUtil redisUtil=new RedisUtil();
            redisUtil.initPool(host,port,database);
            return redisUtil;
        }

}
