package com.atguigu.gmall.manage.service;

import com.atguigu.gmall.bean.UmsMember; // api
import com.atguigu.gmall.bean.UmsMemberReceiveAddress; // api


import java.util.List;

public interface UserService {
    List<UmsMember> getAllUser();

    List<UmsMember> getUserById(String id);

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);

    List<UmsMemberReceiveAddress> getAllReceiveAddress();

    UmsMember login(UmsMember umsMember);

    void addUserToken(String token, String memberId);

    UmsMember addOauthUser(UmsMember umsMember);

    UmsMember checkOauthUser(UmsMember umsCheck);

    UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId);
}
