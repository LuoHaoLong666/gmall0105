package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.bean.PmsBaseAttrValue;
import com.atguigu.gmall.bean.PmsBaseSaleAttr;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsBaseSaleAttrMapper;
import com.atguigu.gmall.manage.mapper.PmsProductSaleAttrValueMapper;
import com.atguigu.gmall.manage.service.AttrService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AttrServiceImpl implements AttrService {



    @Autowired
    PmsBaseAttrInfoMapper pmsBaseAttrInfoMapper;
    @Autowired
    PmsBaseAttrValueMapper pmsBaseAttrValueMapper;
    @Autowired
    PmsBaseSaleAttrMapper pmsBaseSaleAttrMapper;


    @Override
    public List<PmsBaseAttrInfo> attrInfoList(String catalog3Id) {
        PmsBaseAttrInfo pmsBaseAttrInfo = new PmsBaseAttrInfo();
        pmsBaseAttrInfo.setCatalog3Id(catalog3Id);  // 查询指定3级商品的平台属性列表
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = pmsBaseAttrInfoMapper.select(pmsBaseAttrInfo);
        for (PmsBaseAttrInfo baseAttrInfo : pmsBaseAttrInfos) {  // 根据查询列表的长度来做循环
            List<PmsBaseAttrValue> pmsBaseAttrValues  = new ArrayList<>(); // 创建一个平台属性值列表
            PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();  // 创建一个平台属性值对象
            pmsBaseAttrValue.setAttrId(baseAttrInfo.getId());  // 注意:是设置平台属性的id值 是给给平台属性值赋值一个指定的平台属性，来判断是哪个平台属性
            pmsBaseAttrValues = pmsBaseAttrValueMapper.select(pmsBaseAttrValue); // 再通过上面赋值的平台属性id来进行查询,并且赋值给平台属性值的集合中
            baseAttrInfo.setAttrValueList(pmsBaseAttrValues); // 将集合赋值给对应的平台属性值对象中
        }
        return pmsBaseAttrInfos;
    }


    @Override
    public String saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo) {

        String id =  pmsBaseAttrInfo.getId();
        if(StringUtils.isBlank(id)){  // 判断id是否为空
            // id为空，保存操作

            // 保存属性，将传递过来的值插入数据库中去，通用Mapper方法
            pmsBaseAttrInfoMapper.insertSelective(pmsBaseAttrInfo); // insert(会将空的值插入到数据库)  insertSelective (不会将空的值插入到数据库)   是否将null插入数据库

            // 保存属性值
            List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
            for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                pmsBaseAttrValue.setAttrId(pmsBaseAttrInfo.getId());

                pmsBaseAttrValueMapper.insertSelective(pmsBaseAttrValue);
            }
        }else{
            // id不为空，修改操作

            // 属性修改
            Example example = new Example(PmsBaseAttrInfo.class);
            example.createCriteria().andEqualTo("id", pmsBaseAttrInfo.getId());
            pmsBaseAttrInfoMapper.updateByExampleSelective(pmsBaseAttrInfo,example);



            // 按照属性id删除所有属性值
            PmsBaseAttrValue pmsBaseAttrValueDel = new PmsBaseAttrValue();
            pmsBaseAttrValueDel.setAttrId(pmsBaseAttrInfo.getId());
            pmsBaseAttrValueMapper.delete(pmsBaseAttrValueDel);

            // 属性值修改
            List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
            // 删除后将新的属性值插入
            for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                pmsBaseAttrValueMapper.insertSelective(pmsBaseAttrValue);
            }

        }

        return "success";
    }

    @Override
    public List<PmsBaseAttrValue> attrValueList(String attrId) {
        PmsBaseAttrValue pmsBaseAttrValue= new PmsBaseAttrValue();
        pmsBaseAttrValue.setAttrId(attrId);
        List<PmsBaseAttrValue> pmsBaseAttrValues = pmsBaseAttrValueMapper.select(pmsBaseAttrValue);
        return pmsBaseAttrValues;
    }

    @Override
    public List<PmsBaseSaleAttr> baseSaleAttrList() {
        return pmsBaseSaleAttrMapper.selectAll();
    }

    @Override
    public List<PmsBaseAttrInfo> getAttrValuleListByValueId(Set<String> valueIdSet) {
        String valueIdStr = StringUtils.join(valueIdSet, ",");// 41,45,46   将集合的结果转换为字符串的形式
        List<PmsBaseAttrInfo>  pmsBaseAttrInfos = pmsBaseAttrInfoMapper.selectAttrValuleListByValueId(valueIdStr);
        return pmsBaseAttrInfos;
    }
}