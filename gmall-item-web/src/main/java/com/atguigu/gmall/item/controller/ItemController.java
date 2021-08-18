package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsProductSaleAttrValue;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.manage.service.SkuService;
import com.atguigu.gmall.manage.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Controller
public class ItemController {

    @Reference
    SkuService skuService;
    @Reference
    SpuService spuService;

    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, ModelMap modelMap, HttpServletRequest request){

        String remoteAddr = request.getRemoteAddr(); // 远程地址

//        request.getHeader(""); // nginx负载均衡

        PmsSkuInfo pmsSkuInfo = skuService.getSkuById(skuId,remoteAddr);

        // sku对象
        modelMap.put("skuInfo",pmsSkuInfo);
        // 销售属性列表
        List<PmsProductSaleAttr>  pmsProductSaleAttrs = spuService.spuSaleAttrListCheckBySku(pmsSkuInfo.getProductId(),pmsSkuInfo.getId());
        modelMap.put("spuSaleAttrListCheckBySku",pmsProductSaleAttrs);

        // 查询当前sku的spu的其他sku的集合的hash表, (这里可以换成js静态文件，来通过读取js文件中的数据来操作)
        HashMap<String, String> skuSaleAttrHash = new HashMap<>();
        List<PmsSkuInfo> pmsSkuInfos = skuService.getSkuSaleAttrValueListBySpu(pmsSkuInfo.getProductId());   // 获取spu下的sku的销售属性id和skuid

        for (PmsSkuInfo skuInfo : pmsSkuInfos) {   // 获取到的数据
            String k = "";
            String v = skuInfo.getId();

            List<PmsSkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();

            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
                k += pmsSkuSaleAttrValue.getSaleAttrValueId() + "|";  // "239|245"
            }
            skuSaleAttrHash.put(k,v);
        }

        // 将sku的销售属性hash表放到页面，完全的客户端操作，要当成一个js，json字符串。
        String skuSaleAttrHashJsonStr = JSON.toJSONString(skuSaleAttrHash);
        modelMap.put("skuSaleAttrHashJsonStr",skuSaleAttrHashJsonStr);  // 将sku的销售属性hash表中的数据转换成json字符串发送给前端页面

        return "item";
    }

    @RequestMapping("index")
    public String index(ModelMap modelMap){

        List<String> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            list.add("循环数据"+ i);
        }
        modelMap.put("list",list);
        modelMap.put("hello","hello thymeleaf !!");
        modelMap.put("check","1");
        return "index";
    }

}
