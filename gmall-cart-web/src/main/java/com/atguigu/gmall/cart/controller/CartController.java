package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.manage.service.CartService;
import com.atguigu.gmall.manage.service.SkuService;
import com.atguigu.gmall.util.CookieUtil;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class CartController {

    List<OmsCartItem> omsCartItems = new ArrayList<>();
    @Reference
    SkuService skuService;
    @Reference
    CartService cartService;



    @RequestMapping("checkCart")
    @LoginRequired(loginSuccess = false)
    public String checkCart(String isChecked,String skuId,HttpServletRequest request, HttpServletResponse response,ModelMap modelMap) {

        String memberId = (String)request.getAttribute("memberId"); // 如果memberId为空的话，那么就是null.toString()了。会报空指针异常
        String nickname = (String)request.getAttribute("nickname");

        // 调用服务,修改状态
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setIsChecked(isChecked);
        omsCartItem.setProductSkuId(skuId);
        cartService.checkCart(omsCartItem);

        // 将最新的数据从缓存中查出，渲染给内嵌页
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);
        modelMap.put("cartList",omsCartItems);
        // 被勾选的商品的总额
        BigDecimal totalAmount = getTotalAmount(omsCartItems);
        modelMap.put("totalAmount",totalAmount);
        return "cartListInner";
    }


    @RequestMapping("cartList")
    @LoginRequired(loginSuccess = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response,ModelMap modelMap){
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        String memberId = (String)request.getAttribute("memberId"); // 如果memberId为空的话，那么就是null.toString()了。会报空指针异常
        String nickname = (String)request.getAttribute("nickname");

        if(StringUtils.isNotBlank(memberId)){
            // 已经登录查询db
            omsCartItems =  cartService.cartList(memberId);
        }else{
            // 没有登录查询cookie
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if(StringUtils.isNotBlank(cartListCookie)){
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class); // 将JSON形式数据转换成集合
            }
        }

        for (OmsCartItem omsCartItem : omsCartItems) {
            omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));  // 商品总价
        }

        modelMap.put("cartList",omsCartItems);
        // 被勾选的商品的总额
        BigDecimal totalAmount = getTotalAmount(omsCartItems);
        modelMap.put("totalAmount",totalAmount);
        return "cartList";
    }

    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {
        BigDecimal totalAmount = new BigDecimal("0");
        for (OmsCartItem omsCartItem : omsCartItems) {
            BigDecimal totalPrice = omsCartItem.getTotalPrice();
            if(omsCartItem.getIsChecked().equals("1")){
                totalAmount = totalAmount.add(totalPrice);
            }
        }
        return totalAmount;
    }

    @RequestMapping("addToCart")
    @LoginRequired(loginSuccess = false)
    public String addToCart(String skuId, int quantity, HttpServletRequest request, HttpServletResponse response){
        
        // 调用商品服务查询商品信息
        PmsSkuInfo skuInfo = skuService.getSkuById(skuId, ""); // ip没有实际的作用，只是可以用来查看访问该方法的ip地址

        // 将商品信息封装成购物车信息
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setCreateDate((new Date()));
        omsCartItem.setDeleteStatus(0);
        omsCartItem.setModifyDate(new Date());
        omsCartItem.setPrice(skuInfo.getPrice());
        omsCartItem.setProductAttr("");
        omsCartItem.setProductBrand("");
        omsCartItem.setProductCategoryId(skuInfo.getCatalog3Id());
        omsCartItem.setProductId(skuInfo.getProductId());
        omsCartItem.setProductName(skuInfo.getSkuName());
        omsCartItem.setProductPic(skuInfo.getSkuDefaultImg());
        omsCartItem.setProductSkuCode("11111111111111111");
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setQuantity(new BigDecimal(quantity));


        //判断用户是否登录
        String memberId = (String)request.getAttribute("memberId"); // 如果memberId为空的话，那么就是null.toString()了。会报空指针异常
        String nickname = (String)request.getAttribute("nickname");

        if(StringUtils.isBlank(memberId)){
            // 用户没有登录


            // cookie里原有的购物车数据
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);

            // 判断添加的购物车数据在cookie中是否存在
            if(StringUtils.isBlank(cartListCookie)){
                // cookie为空
                omsCartItems.add(omsCartItem);
            }else{
                // cookie不为空
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);  // 当前购物车里的数据
                // 判断添加的购物车数据在cookie中是否存在
                boolean exist = if_cart_exist(omsCartItems,omsCartItem);
                if(exist){
                    // 之前添加过,更新购物车添加数量
                    for (OmsCartItem cartItem : omsCartItems) {
                        if(cartItem.getProductId().equals(omsCartItem.getProductId())){
                            cartItem.setQuantity(cartItem.getQuantity().add(omsCartItem.getQuantity()));
//                            cartItem.setPrice(cartItem.getPrice().add(omsCartItem.getPrice()));
                        }
                    }
                }else{
                    // 之前没有添加过,新增当前的购物车
                    omsCartItems.add(omsCartItem);
                }
            }

            // 更新cookie
            CookieUtil.setCookie(request,response,"cartListCookie", JSON.toJSONString(omsCartItems),60*60*72,true);

        }else{
            // 用户已经登录
            // 从db中查出购物车数据
            OmsCartItem omsCartItemFromDb = cartService.ifCartExistByUser(memberId,skuId);  // 只需要判断当前的商品,有没有被该用户添加过,这样写可以让代码更加简洁

            if(omsCartItemFromDb==null){
                // 该用户没有添加当前商品
                omsCartItem.setMemberId(memberId); // 设置为当前操作用户
                omsCartItem.setMemberNickname("test小明");
                omsCartItem.setQuantity(new BigDecimal(quantity));
                cartService.addCart(omsCartItem);  // 注意，不要写成omsCartItemFromDb

            }else{
                // 该用户添加过当前商品
                omsCartItemFromDb.setQuantity(omsCartItemFromDb.getQuantity().add(omsCartItem.getQuantity()));
                cartService.updateCart(omsCartItemFromDb);

            }

            // 同步缓存
            cartService.flushCartCache(memberId);


        }

        return "redirect:/success.html";
    }


    private boolean if_cart_exist(List<OmsCartItem> omsCartItems, OmsCartItem omsCartItem) {

        boolean b = false;

        for (OmsCartItem cartItem : omsCartItems) {
            String productSkuId = cartItem.getProductSkuId();

            if(productSkuId.equals(omsCartItem.getProductId())){
                b = true;
            }
        }

        return true;
    }


}
