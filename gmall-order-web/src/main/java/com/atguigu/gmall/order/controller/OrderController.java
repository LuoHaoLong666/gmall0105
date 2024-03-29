package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.manage.service.CartService;
import com.atguigu.gmall.manage.service.SkuService;
import com.atguigu.gmall.manage.service.UserService;
import com.atguigu.gmall.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    CartService cartService;

    @Reference
    UserService userService;

    @Reference
    OrderService orderService;

    @Reference
    SkuService skuService;

    @RequestMapping("submitOrder")
    @LoginRequired(loginSuccess=true)
    public ModelAndView submitOrder(String receiveAddressId, BigDecimal totalAmount, String tradeCode, HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {

        String memberId = (String)request.getAttribute("memberId"); // 如果memberId为空的话，那么就是null.toString()了。会报空指针异常
        String nickname = (String)request.getAttribute("nickname");

        // 检查交易码
        String success = orderService.checkTradeCode(memberId,tradeCode);


        if(success.equals("success")){
            List<OmsOrderItem> omsOrderItems = new ArrayList<>();

            // 订单对象
            OmsOrder omsOrder = new OmsOrder();
            omsOrder.setAutoConfirmDay(7);  // 收货确认时间
            omsOrder.setCreateTime(new Date());  // 创建订单时间
            omsOrder.setDiscountAmount(null);
//            omsOrder.setFreightAmount();  运费,支付后，在生成物流信息时
            omsOrder.setMemberId(memberId);
            omsOrder.setMemberUsername(nickname);
            omsOrder.setNote("快点发货"); // 订单备注
            String outTradeNo1 = "gmall"; // 外部订单号
            outTradeNo1 = outTradeNo1+System.currentTimeMillis(); // 将毫秒时间戳拼接到外部订单号
            SimpleDateFormat sdf1 = new SimpleDateFormat("YYYYMMDDHHmmss");
            outTradeNo1 = outTradeNo1+sdf1.format(new Date()); // 将时间字符串拼接到外部订单号
            
            omsOrder.setOrderSn(outTradeNo1); // 外部订单号
            omsOrder.setPayAmount(totalAmount);  // 这里是需要调购物车的商品信息的服务来获得总金额
            omsOrder.setOrderType(1); // 订单类型
            UmsMemberReceiveAddress umsMemberReceiveAddress = userService.getReceiveAddressById(receiveAddressId);
            omsOrder.setReceiverCity(umsMemberReceiveAddress.getCity());
            omsOrder.setReceiverDetailAddress(umsMemberReceiveAddress.getDetailAddress());
            omsOrder.setReceiverName(umsMemberReceiveAddress.getName());
            omsOrder.setReceiverPhone(umsMemberReceiveAddress.getPhoneNumber());
            omsOrder.setReceiverPostCode(umsMemberReceiveAddress.getPostCode());
            omsOrder.setReceiverProvince(umsMemberReceiveAddress.getProvince());
            omsOrder.setReceiverRegion(umsMemberReceiveAddress.getRegion());
            // 当前日期加一天,一天后配送
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE,1);
            Date time = c.getTime();
            omsOrder.setReceiveTime(time);
//            omsOrder.setSourceType(new BigDecimal(0));
//            omsOrder.setStatus(new BigDecimal(0));
            omsOrder.setTotalAmount(totalAmount);

            // 根据用户id获得要购买的商品列表(购物车),和总价格
            List<OmsCartItem> omsCartItems = cartService.cartList(memberId);
            for (OmsCartItem omsCartItem : omsCartItems) {
                if(omsCartItem.getIsChecked().equals("1")){
                    // 获得订单详情列表
                    OmsOrderItem omsOrderItem = new OmsOrderItem();
                    // 检验价格
                    boolean b = skuService.checkPrice(omsCartItem.getProductSkuId(),omsCartItem.getPrice());
                    if(b==false){
                        ModelAndView mv = new ModelAndView("tradeFail");
                        return mv;
                    }
                    // 验库存，远程调用库存系统
                    omsOrderItem.setProductPic(omsCartItem.getProductPic());
                    omsOrderItem.setProductName(omsCartItem.getProductName());
                    String outTradeNo = "gmall"; // 外部订单号
                    outTradeNo = outTradeNo+System.currentTimeMillis(); // 将毫秒时间戳拼接到外部订单号
                    SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMDDHHmmss");
                    outTradeNo = outTradeNo+sdf.format(new Date()); // 将时间字符串拼接到外部订单号
                    omsOrderItem.setOrderSn(outTradeNo);  // 外部订单号,用来和其他系统进行交互,防止重复
                    omsOrderItem.setProductCategoryId(omsCartItem.getProductCategoryId()); // 三级分类id
                    omsOrderItem.setProductPrice(omsCartItem.getPrice());
                    omsOrderItem.setRealAmount(omsCartItem.getTotalPrice());
                    omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                    omsOrderItem.setProductSkuCode("11111111111");
                    omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());  // skuId
                    omsOrderItem.setProductId(omsCartItem.getProductId());  // spuId
                    omsOrderItem.setProductSn("仓库对应的商品编号");  // 在仓库中的skuId

                    // 检验库存，远程调用库存系统
                    omsOrderItems.add(omsOrderItem);

                }

            }
            omsOrder.setOmsOrderItems(omsOrderItems);





            // 将订单和订单详情写入数据库
            // 删除购物车的对应商品
            orderService.saveOrder(omsOrder);


            // 重定向到支付系统
            ModelAndView mv = new ModelAndView("redirect:http://payment.gmall.com:8087/index");
            mv.addObject("outTradeNo",outTradeNo1);
            mv.addObject("totalAmount",totalAmount);
            return mv;
        }else{
            ModelAndView mv = new ModelAndView("tradeFail");
            return mv;
        }


    }



    @RequestMapping("toTrade")
    @LoginRequired(loginSuccess=true)
    public String toTrade(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {


        String memberId = (String)request.getAttribute("memberId"); // 如果memberId为空的话，那么就是null.toString()了。会报空指针异常
        String nickname = (String)request.getAttribute("nickname");

        // 收件人地址列表
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = userService.getReceiveAddressByMemberId(memberId);

        // 将购物车集合转化为页面结算清单集合
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);

        List<OmsOrderItem> omsOrderItems = new ArrayList<>();

        for (OmsCartItem omsCartItem : omsCartItems) {
            // 每循环一个购物车对象,就封装一个商品的详情到OmsOrderItem
            if(omsCartItem.getIsChecked().equals("1")){
                OmsOrderItem omsOrderItem = new OmsOrderItem();
                omsOrderItem.setProductName(omsCartItem.getProductName());
                omsOrderItem.setProductPic(omsCartItem.getProductPic());
                omsOrderItems.add(omsOrderItem);
            }

        }

        modelMap.put("omsOrderItems",omsOrderItems);
        modelMap.put("userAddressList",umsMemberReceiveAddresses);
        modelMap.put("totalAmount",getTotalAmount(omsCartItems));


        // 生成交易码,为了在提交订单时做交易码的校验
        String tradeCode = orderService.genTradeCode(memberId);
        modelMap.put("tradeCode",tradeCode);

        return "trade";
    }


    // 获得总价格
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



}
