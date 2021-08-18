package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsBaseCatalog1;
import com.atguigu.gmall.bean.PmsBaseCatalog2;
import com.atguigu.gmall.bean.PmsBaseCatalog3;
import com.atguigu.gmall.manage.service.CatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@CrossOrigin
public class CatalogController {

    @Reference
    CatalogService catalogService;

    @RequestMapping("getCatalog1")
    @ResponseBody  // 自动转换成json格式返回给前端界面
    public List<PmsBaseCatalog1> getCatalog1(){
        List<PmsBaseCatalog1> catalog1s = catalogService.getCatalog1();
        return catalog1s;
    }


    // 注意: 如果前端没有传递json格式的数据的话,就不需要使用@RequestBody来接收。
    @RequestMapping("getCatalog2")
    @ResponseBody  // 自动转换成json格式返回给前端界面
    public List<PmsBaseCatalog2> getCatalog2(String catalog1Id){
        List<PmsBaseCatalog2> catalog2s = catalogService.getCatalog2(catalog1Id);
        return catalog2s;
    }

    // 注意: 如果前端没有传递json格式的数据的话,就不需要使用@RequestBody来接收。
    @RequestMapping("getCatalog3")
    @ResponseBody  // 自动转换成json格式返回给前端界面
    public List<PmsBaseCatalog3> getCatalog3(String catalog2Id){
        List<PmsBaseCatalog3> catalog3s = catalogService.getCatalog3(catalog2Id);
        return catalog3s;
    }

}
