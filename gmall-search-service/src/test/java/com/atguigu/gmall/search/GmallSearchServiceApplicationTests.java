package com.atguigu.gmall.search;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.manage.service.SkuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.elasticsearch.jest.JestAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallSearchServiceApplicationTests {

    @Reference
    SkuService skuService;  // 查询mysql

    @Autowired
    JestClient jestClient;

    @Test
    public void contextLoads() throws IOException {
        put();
    }

    @Test
    public void put() throws IOException {  // 转换mysql中的数据为es中的数据结构,和es表对应的java类表,并添加到es库中
        // 查询mysql数据
        List<PmsSkuInfo> pmsSkuInfoList = new ArrayList<>(); // 从mysql数据库中获取到的数据都存入该集合中这里面

        pmsSkuInfoList = skuService.getAllSku("287");  // 从mysql数据库获取所有的skuInfo中的数据

        // 转化为es的数据结构
        List<PmsSearchSkuInfo> pmsSearchSkuInfos = new ArrayList<>();

        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfoList) {
            PmsSearchSkuInfo pmsSearchSkuInfo = new PmsSearchSkuInfo();

            BeanUtils.copyProperties(pmsSkuInfo,pmsSearchSkuInfo);  // mysql数据库中的id数据是String类型，而es中的id是long类型，类型不一致就会出现错误，所有的id都变成了0

            pmsSearchSkuInfo.setId(Long.parseLong(pmsSkuInfo.getId()));
            // 为什么数据只有一条,因为主键id都是0,都互相覆盖掉了
            pmsSearchSkuInfos.add(pmsSearchSkuInfo);
        }

        // 导入es,不需要写close关闭，因为是http请求
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
            Index put = new Index.Builder(pmsSearchSkuInfo).index("gmall0105").type("PmsSkuInfo").id(pmsSearchSkuInfo.getId()+"").build();  // 转化成Json文本
            jestClient.execute(put);
        }

    }


    @Test
    public void get(){
        // jest的dsl工具
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        // filter
        TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId","39");
        boolQueryBuilder.filter(termQueryBuilder);
        TermQueryBuilder termQueryBuilder1 = new TermQueryBuilder("skuAttrValueList.valueId","43");
        boolQueryBuilder.filter(termQueryBuilder1);

        //must
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName","华为");
        boolQueryBuilder.must(matchQueryBuilder);

        // query
        searchSourceBuilder.query(boolQueryBuilder);


        // from
        searchSourceBuilder.from(0);
        // size
        searchSourceBuilder.size(20);
        // highlight
        searchSourceBuilder.highlight(null);

        String dslStr = searchSourceBuilder.toString();

        System.err.println(dslStr);

        // 用API执行复杂查询
        List<PmsSearchSkuInfo> searchSkuInfos = new ArrayList<>(); // 用来封装获取到的数据

        Search search = new Search.Builder(dslStr).addIndex("gmall0105").addType("PmsSkuInfo").build();

        SearchResult execute = null;  // 查询的结果 最外层的Hits
        try {
            execute = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = execute.getHits(PmsSearchSkuInfo.class);
        for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits) {
            PmsSearchSkuInfo source = hit.source;  // 获取到hits中对象的所有属性的结果
            searchSkuInfos.add(source);
        }
        System.out.println(searchSkuInfos.size());

    }

}
