package com.cdb;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.json.JSONUtil;

import java.util.List;

public class K01KeepApi {

    public static void main() {
        String jsonStr = FileUtil.readUtf8String(K01Query.ABSOLUTE_PATH + "ip_token.json");
        List<K01Query.IpCookie> ipCookieList = JSONUtil.toList(jsonStr, K01Query.IpCookie.class);
        String datetime = DateUtil.format(DateTime.now(), "yyyy-MM-dd HH:mm");
        for (K01Query.IpCookie ipCookie : ipCookieList) {
            K01Query.transform(ipCookie, datetime,datetime);
            Console.log(DateUtil.now()+":API调用"+ipCookie.getUrl());
        }
    }
}
