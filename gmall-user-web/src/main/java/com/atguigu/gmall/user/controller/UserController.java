package com.atguigu.gmall.user.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.manage.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class UserController {

    @Reference
    UserService userService;

    @RequestMapping("getUserByMemberId")
    @ResponseBody
    public List<UmsMember> getUserByMemberId(String id) {
        List<UmsMember>  umsMember =  userService.getUserById(id);
        return umsMember;
    }



    @RequestMapping("getAllUser")
    @ResponseBody
    public List<UmsMember> getAllUser() {
        List<UmsMember> umsMembers = userService.getAllUser();
        return umsMembers;
    }


    @RequestMapping("getReceiveAddressByMemberId")
    @ResponseBody
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {
        List<UmsMemberReceiveAddress>  umsMemberReceiveAddress =  userService.getReceiveAddressByMemberId(memberId);
        return umsMemberReceiveAddress;
    }


    @RequestMapping("getAllReceiveAddressByMember")
    @ResponseBody
    public List<UmsMemberReceiveAddress> getAllUmsMemberReceiveAddress() {
        List<UmsMemberReceiveAddress>  umsMemberReceiveAddresses =  userService.getAllReceiveAddress();
        return umsMemberReceiveAddresses;
    }



    @RequestMapping("index")
    @ResponseBody
    public String index() {
        return "hello user";
    }
}
