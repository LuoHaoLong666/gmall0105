package com.atguigu.gmall.user.service.Impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.manage.service.UserService;  // api
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.atguigu.gmall.user.mapper.UserMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;

    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<UmsMember> getAllUser() {
        List<UmsMember> umsMemberList = userMapper.selectAll();
        // userMapper.selectAllUser();
        return umsMemberList;
    }

    @Override
    public List<UmsMember> getUserById(String id) {
        UmsMember umsMember1 = new UmsMember();
        umsMember1.setId(id);
        List<UmsMember> umsMember = userMapper.select(umsMember1);
        return umsMember;
    }


    @Override
    public List<UmsMemberReceiveAddress> getAllReceiveAddress() {
        List<UmsMemberReceiveAddress> umsMemberReceiveAddressList = umsMemberReceiveAddressMapper.selectAll();
        return umsMemberReceiveAddressList;
    }



    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {
//       Example e = new Example(UmsMemberReceiveAddress.class);
//       e.createCriteria().andEqualTo("memberId",memberId);

        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);

//       List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.selectByExample(umsMemberReceiveAddress);
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);
        return umsMemberReceiveAddresses;
    }

    @Override
    public UmsMember login(UmsMember umsMember) {

        // 可以优化,将两个else中的启动数据库的代码都抽取出来,将两个相同的else删掉
        Jedis jedis = null;
        try{
            jedis = redisUtil.getJedis();

            if(jedis!=null){
                String umsMemberStr = jedis.get("user:" + umsMember.getPassword() + ":info");
                if (StringUtils.isNotBlank(umsMemberStr)) {
                    // 密码正确
                    UmsMember umsMemberFromCache = JSON.parseObject(umsMemberStr, UmsMember.class);
                    return umsMemberFromCache;
                }
            }

            // 连接redis失败,开启数据库
            // 分布式锁
            UmsMember umsMemberFromDb  = loginFromDb(umsMember);
            if(umsMemberFromDb!=null){
                jedis.setex("user:" + umsMember.getPassword() + ":info",60*60*24,JSON.toJSONString(umsMemberFromDb));
            }
            return umsMemberFromDb;

        }finally {
            jedis.close();
        }

    }

    @Override
    public void addUserToken(String token, String memberId) {

    }

    private UmsMember loginFromDb(UmsMember umsMember) {


        List<UmsMember> umsMembers = userMapper.select(umsMember);
        if(umsMembers!=null){
            return umsMembers.get(0);
        }
//        UmsMember umsMember1 = userMapper.selectOne(umsMember);
//        if(umsMember1!=null){
//            return umsMember1;
//        }
        return null;
    }

}
