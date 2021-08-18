package com.atguigu.gmall.manage.redissonTest;

import com.atguigu.gmall.util.RedisUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;

@Controller
public class RedissonController {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    RedissonClient redissonClient;

    @RequestMapping("testRedisson")
    @ResponseBody
    public String  testRedisson(){
        Jedis jedis = redisUtil.getJedis();
        RLock lock = redissonClient.getLock("lock");
        return "success";
    }
}
