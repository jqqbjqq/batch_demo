package com.cdb;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.json.JSONUtil;

import java.util.List;

public class K01KeepApi {

    static String apiCron = "0 0 * * * ?";

    public static void main() {
        Console.log("refresh cookies running");
        String api_cron = K01Query.setting.get("api_cron");
        if (StrUtil.isNotBlank(api_cron)) {
            apiCron = api_cron;
        }
        CronUtil.schedule(apiCron, new Task() {
            @Override
            public void execute() {
                Console.log("刷新cookies程序,窗口不要关闭!!!");
                api();
            }
        });
        CronUtil.setMatchSecond(true);
        CronUtil.start();
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void api() {
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
        }
    }
}
