package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuImage;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.manage.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.session.SessionProperties;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;
    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;
    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;
    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;
    @Autowired
    RedisUtil redisUtil;

    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {

        // 插入skuInfo
        int i = pmsSkuInfoMapper.insertSelective(pmsSkuInfo); // 要配置主键返回策略
        String skuId = pmsSkuInfo.getId();

        // 插入平台属性关联     sku平台属性值关联表
        List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
            pmsSkuAttrValue.setSkuId(skuId);
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }

        // 插入销售属性关联  sku销售属性值关联表
        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
            pmsSkuSaleAttrValue.setSkuId(skuId);
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }

        // 插入图片信息
        List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : skuImageList) {
            pmsSkuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }

        // 发出商品的缓存同步消息,解耦合
        // 发出商品的搜索引擎的同步消息



    }


    public PmsSkuInfo getSkuByIdFromDb(String skuId){   // 通过数据库来获取到skuinfo信息
        // sku商品对象 前台主要需要的
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(skuId);
        PmsSkuInfo skuInfos= pmsSkuInfoMapper.selectOne(pmsSkuInfo);

        // sku的图片集合
        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> pmsSkuImages= pmsSkuImageMapper.select(pmsSkuImage);
        skuInfos.setSkuImageList(pmsSkuImages);
        return skuInfos;
    }

    @Override
    public PmsSkuInfo getSkuById(String skuId,String ip) {
        System.out.println("ip为"+ip+"的同学"+Thread.currentThread().getName()+"进入的商品详情的请求");
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();

        // 连接缓存
        Jedis jedis = redisUtil.getJedis();
        // 查询缓存
        String skuKey = "sku:"+skuId+":info";
        String skuJson = jedis.get(skuKey);

        if(StringUtils.isNotBlank(skuJson)){  // if(skuJson!=null&&skuJson.equals(""))  如果获取到缓存，就直接从缓存中获取数据
            System.out.println("ip为"+ip+"的同学"+Thread.currentThread().getName()+"从缓存中获取商品详情");
            pmsSkuInfo = JSON.parseObject(skuJson, PmsSkuInfo.class);// 把从缓存中获取的json字符串转换成java类
        }else{  // 如果没有获取到缓存,则需要去数据库中获取数据

            // 如果缓存中没有，查询mysql数据库
            System.out.println("ip为"+ip+"的同学"+Thread.currentThread().getName()+"发现缓存中没有，申请缓存的分布式锁:"+"sku:" + skuId + ":lock");

            // 设置分布式锁, 为了防止缓存击穿，需要设置一个分布式锁，来限制访问数据库的权限,只能一个一个来访问数据库并传入缓存中。
            String token = UUID.randomUUID().toString();
            String OK = jedis.set("sku:" + skuId + ":lock", token, "nx", "px", 10*1000); // 拿到锁的线程有10秒的过期时间
            if(StringUtils.isNotBlank(OK)&&OK.equals("OK")){  // 如果设置成功则放回OK的标识

                 // 设置成功,有权在十秒的过期时间内访问数据库
                System.out.println("ip为"+ip+"的同学"+Thread.currentThread().getName()+"有权在十秒的过期时间内访问数据库: "+"sku:" + skuId + ":lock");
                pmsSkuInfo = getSkuByIdFromDb(skuId);  // 访问数据库

                // 为了测试并发效果
//                try {
//                    Thread.sleep(1000*5);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

                if(pmsSkuInfo!=null){  // 如果数据库中有值，则传入redis缓存中
                    // mysql查询结果存入redis,并返回给页面
                    jedis.set("sku:"+skuId+":info", JSON.toJSONString(pmsSkuInfo));
                }else{  // 如果数据库中没有值,还需要防止缓存穿透,则需要将不存在的sku当做""值传入。
                    // 数据库中不存在该sku
                    // 为了防止缓存穿透, null值或者空字符串设置给redis
                    jedis.setex("sku:"+skuId+":info", 60*3,JSON.toJSONString(""));
                }


                // 在访问mysql后，将mysql的分布式锁释放
                System.out.println("ip为"+ip+"的同学"+Thread.currentThread().getName()+"使用完毕，将锁归还: "+"sku:" + skuId + ":lock");
                String lockToken = jedis.get("sku:" + skuId + ":lock");
                if(StringUtils.isNotBlank(lockToken)&&lockToken.equals(token)){
                    // jedis.eval("lua");  可以用lua脚本，在查询到key的同时删除该key,防止该并发下的意外的发生
                    jedis.del("sku:" + skuId + ":lock"); // 用token确认删除的是自己的sku的锁
                }
                


            }else{
                // 设置失败,自旋 (该线程在睡眠几秒后,重新尝试访问本方法)
                System.out.println("ip为"+ip+"的同学"+Thread.currentThread().getName()+"没有拿到锁，开始自旋");
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                return getSkuById(skuId,ip);  // 该线程睡眠3秒后，再次返回该方法的从头再执行一遍


            }

        }

        jedis.close();
        return pmsSkuInfo;

    }

    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId) {
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectSkuSaleAttrValueListBySpu(productId);
        return pmsSkuInfos;
    }

    @Override
    public List<PmsSkuInfo> getAllSku(String catalog3Id) {
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectAll();

        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos) {
            String skuId = pmsSkuInfo.getId();

            PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
            pmsSkuAttrValue.setSkuId(skuId);
            List<PmsSkuAttrValue> select = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);

            pmsSkuInfo.setSkuAttrValueList(select);
        }

        return pmsSkuInfos;
    }

    @Override
    public boolean checkPrice(String productSkuId, BigDecimal productPrice) {

        boolean b = false;

        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(productSkuId);
        PmsSkuInfo pmsSkuInfo1 = pmsSkuInfoMapper.selectOne(pmsSkuInfo);

        BigDecimal price = pmsSkuInfo1.getPrice();

        if(price.compareTo(productPrice)==0){
            b = true;
        }

        return b;
    }
}
