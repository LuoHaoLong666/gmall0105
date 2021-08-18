package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.service.AttrService;
import com.atguigu.gmall.manage.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import tk.mybatis.mapper.util.StringUtil;

import java.util.*;

@Controller
public class SearchController {

    @Reference
    SearchService searchService;

    @Reference
    AttrService attrService;

    @RequestMapping("list.html")
    public String list(PmsSearchParam pmsSearchParam, ModelMap modelMap) {  // 三级分类id、关键字、平台属性列表  使用ModelMap来返回数据

        // 调用搜索服务、返回搜索结果
        List<PmsSearchSkuInfo> searchSkuInfos = searchService.list(pmsSearchParam);
        modelMap.put("skuLsInfoList", searchSkuInfos);  // 需要看前端的命名是怎么样的

        // 抽取检索结果所包含的平台属性集合,性能优化
        Set<String> valueIdSet = new HashSet<>();

        for (PmsSearchSkuInfo searchSkuInfo : searchSkuInfos) {
            List<PmsSkuAttrValue> skuAttrValueList = searchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                String valueId = pmsSkuAttrValue.getValueId();
                valueIdSet.add(valueId);
            }
        }

        // 根据valueId将属性列表查询出来

        List<PmsBaseAttrInfo> pmsBaseAttrInfos = attrService.getAttrValuleListByValueId(valueIdSet);
        modelMap.put("attrList", pmsBaseAttrInfos);

        // 对平台属性集合进一步处理，去掉当前条件中valueId所在的属性组,  delValueIds是平台属性列表
        String[] delValueIds = pmsSearchParam.getValueId();  // 从前台点击属性列表过后传,递过来的平台属性值

        // 删除平台属性和面包屑功能
        if (delValueIds != null) {
            // 面包屑
            // pmsSearchParam
            // delValueIds
            // 如果valueIds参数不为空,说明当前请求中包含属性的参数,每一个属性参数,都会生成一个面包屑。
            List<PmsSearchCrumb> pmsSearchCrumbs = new ArrayList<>();  // 创建面包屑集合对象
            // 如果直接使用嵌套for循环删除属性组的话，会报数组越界异常，因为在删除数组的时候,数组会自动再进行排序,索引值就会少
            for (String delValueId : delValueIds) {
                Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfos.iterator();   // iterator迭代器最适合这种检查式的循环了,删除了集合不会受到影响,迭代器用过一遍就没了
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                // 生成面包屑的参数
                pmsSearchCrumb.setValueId(delValueId);
                // 面包屑上的url地址
                pmsSearchCrumb.setUrlParam(getUrlParamForCrumb(pmsSearchParam, delValueId));  // pmsSearchParam为当前请求

                while (iterator.hasNext()) {
                    PmsBaseAttrInfo PmsBaseAttrInfo = iterator.next();
                    List<PmsBaseAttrValue> attrValueList = PmsBaseAttrInfo.getAttrValueList();
                    for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                        String valueId = pmsBaseAttrValue.getId();
                        if (delValueId.equals(valueId)) {
                            // 查找面包屑的属性值名称
                            pmsSearchCrumb.setValueName(pmsBaseAttrValue.getValueName());
                            // 删除该属性值所在的属性组
                            iterator.remove();
                        }
                    }
                }
                pmsSearchCrumbs.add(pmsSearchCrumb);
            }
            modelMap.put("attrValueSelectedList", pmsSearchCrumbs);
        }

        // 获取pmsSearchParam中的值 平台属性对应的平台属性值列表的url地址
        String urlParam = getUrlParam(pmsSearchParam);

        modelMap.put("urlParam", urlParam);

        String keyword = pmsSearchParam.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            modelMap.put("keyword", keyword);
        }

        return "list";
    }

    private String getUrlParamForCrumb(PmsSearchParam pmsSearchParam, String delValueId) {
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String[] skuAttrValueList = pmsSearchParam.getValueId();

        String urlParam = "";

        if (StringUtils.isNotBlank(keyword)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "keyword=" + keyword;
        }

        if (StringUtils.isNotBlank(catalog3Id)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "catalog3Id=" + catalog3Id;
        }
        if (skuAttrValueList != null) {
            for (String pmsSkuAttrValue : skuAttrValueList) {
                if (!pmsSkuAttrValue.equals(delValueId)) {
                    urlParam = urlParam + "&valueId=" + pmsSkuAttrValue;
                }
            }
        }

        return urlParam;
    }


    private String getUrlParam(PmsSearchParam pmsSearchParam) {
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String[] skuAttrValueList = pmsSearchParam.getValueId();

        String urlParam = "";

        if (StringUtils.isNotBlank(keyword)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "keyword=" + keyword;
        }

        if (StringUtils.isNotBlank(catalog3Id)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "catalog3Id=" + catalog3Id;
        }
        if (skuAttrValueList != null) {
            for (String pmsSkuAttrValue : skuAttrValueList) {
                urlParam = urlParam + "&valueId=" + pmsSkuAttrValue;
            }
        }

        return urlParam;
    }

    @RequestMapping("index")
    @LoginRequired(loginSuccess = false)
    public String index() {
        return "index";
    }
}
