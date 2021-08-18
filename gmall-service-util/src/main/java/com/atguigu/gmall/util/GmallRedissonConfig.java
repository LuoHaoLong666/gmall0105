package com.atguigu.gmall.util;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration  // Spring配置类,将redisson整合到Spring容器中
public class GmallRedissonConfig {

    @Value("${spring.redis.host:0}")
    private String host;

    @Value("${spring.redis.port:6379}")
    private String port;


    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config(); // 创建Redisson配置类
        config.useSingleServer().setAddress("redis://" + host +":"+ port).setPassword("asd123"); // 设置redisson的host和port的配置到Config配置类中
        RedissonClient redissonClient = Redisson.create(config); // 创建redisson客户端。
        return redissonClient;
    }


}
