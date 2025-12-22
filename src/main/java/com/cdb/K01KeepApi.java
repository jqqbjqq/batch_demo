package com.cdb;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import java.util.List;

public class K01KeepApi {

    public static void main() {
        try {
            String jsonStr = FileUtil.readUtf8String(K01Query.ABSOLUTE_PATH + "ip_token.json");
            List<K01Query.IpCookie> ipCookieList = JSONUtil.toList(jsonStr, K01Query.IpCookie.class);
            String datetime = DateUtil.format(DateTime.now(), "yyyy-MM-dd HH:mm:ss");
            for (K01Query.IpCookie ipCookie : ipCookieList) {
                Console.log(DateUtil.now() + " 定时刷新API接口:" + ipCookie.getUrl());
                K01Query.transform(ipCookie, datetime, datetime);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            Console.log("窗口10秒后，自动关闭");
            ThreadUtil.sleep(10 * 1000);
        }
    }
}
