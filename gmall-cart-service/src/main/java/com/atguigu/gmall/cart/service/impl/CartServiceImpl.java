package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.cart.mapper.OmsCartItemMapper;
import com.atguigu.gmall.manage.service.CartService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.io.InputStream;
import java.util.*;

@Service
public class CartServiceImpl implements CartService{

    @Autowired
    RedisUtil redisUtil;
    @Autowired
    OmsCartItemMapper omsCartItemMapper;


    @Override
    public OmsCartItem ifCartExistByUser(String memberId, String skuId) {  // 在mysql数据库中查询当前用户添加到购物车的商品
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        OmsCartItem omsCartItem1 = omsCartItemMapper.selectOne(omsCartItem);  // 如果返回的是由值的，就说明该用户添加过该商品到购物车,如果没有值就说明该用户没有添加过该商品到购物车
        return omsCartItem1;
    }

    @Override
    public void addCart(OmsCartItem omsCartItem) {

        if(StringUtils.isNotBlank(omsCartItem.getMemberId())){  // 判断用户Id是否为空
            omsCartItemMapper.insertSelective(omsCartItem);  // 避免添加空值
        }

    }

    @Override
    public void updateCart(OmsCartItem omsCartItemFromDb) {

        Example e = new Example(OmsCartItem.class);
        e.createCriteria().andEqualTo("id",omsCartItemFromDb.getId());  // 只有是指定的添加到购物车商品才更新,否则不更新

        omsCartItemMapper.updateByExampleSelective(omsCartItemFromDb,e);
    }

    @Override
    public void flushCartCache(String memberId) {

        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        List<OmsCartItem> omsCartItems = omsCartItemMapper.select(omsCartItem);

        // 同步到redis缓存中
        Jedis jedis = redisUtil.getJedis();

        Map<String,String> map = new HashMap<>();
        for (OmsCartItem cartItem : omsCartItems) {
            cartItem.setTotalPrice(cartItem.getPrice().multiply(cartItem.getQuantity()));  // 同步redis缓存中解决计算问题
            map.put(cartItem.getProductSkuId(), JSON.toJSONString(cartItem));  // 将购物车对象变成JSON格式的字符串
        }

        List<Map.Entry<String,String>> list = new ArrayList<Map.Entry<String,String>>();
        Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        }); // map集合排序
        jedis.del("user:"+memberId+":cart"); // 先删除再添加
        jedis.hmset("user:"+memberId+":cart",map);  // 将map集合转换成redis对象

        jedis.close();

    }

    @Override
    public List<OmsCartItem> cartList(String userId) {

        Jedis jedis = null;
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        try{
            jedis = redisUtil.getJedis();

            List<String> hvals = jedis.hvals("user:"+userId+":cart");  // 将从redis缓存中查询出来的所有user:userId:cart中的数据进行循环遍历,将循环出来的JSON格式的数据转换成对象，并加入集合中。
            for (String hval : hvals) {
                OmsCartItem omsCartItem = JSON.parseObject(hval,OmsCartItem.class);
                omsCartItems.add(omsCartItem);
            }
        }catch (Exception e){
            // 处理异常，记录系统日志
            e.printStackTrace();
//            String message = e.getMessage();
//            logService.addErrLog(message);
            return null;
        }finally {
            jedis.close();
        }

        return omsCartItems;
    }

    @Override
    public void checkCart(OmsCartItem omsCartItem) {

        Example e = new Example(OmsCartItem.class);
        e.createCriteria().andEqualTo("memberId",omsCartItem.getMemberId()).andEqualTo("productSkuId",omsCartItem.getProductSkuId()); // 写对应的java对象中的属性名

        omsCartItemMapper.updateByExampleSelective(omsCartItem,e); // 只更新有值的，不更新没值的。

        // 缓存同步
        flushCartCache(omsCartItem.getMemberId());


    }
}
