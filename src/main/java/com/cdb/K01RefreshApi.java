package com.cdb;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.List;
import java.util.UUID;

public class K01RefreshApi extends ApiBase {

    static String apiCron = "0 0 * * * ?";

    public static void main() {
        Console.log("refresh cookies running");
        String api_cron = setting.get("api_cron");
        if (StrUtil.isNotBlank(api_cron)) {
            apiCron = api_cron;
        }
        CronUtil.schedule(apiCron, (Task) () -> {
            Console.log("刷新cookies程序,窗口不要关闭!!!");
            api();
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
            String jsonStr = FileUtil.readUtf8String(ABSOLUTE_PATH + "ip_token.json");
            List<IpCookie> ipCookieList = JSONUtil.toList(jsonStr, IpCookie.class);
            String datetime = DateUtil.format(DateTime.now(), "yyyy-MM-dd HH:mm:ss");
            for (IpCookie ipCookie : ipCookieList) {
                Console.log(DateUtil.now() + " 定时刷新API接口:" + ipCookie.getUrl());
                refresh(ipCookie, datetime, datetime);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private static void refresh(IpCookie ipCookie, String startTime, String endTime) {
        try {
            String xNonce = String.valueOf(UUID.randomUUID());
            String xTimestamp = String.valueOf(DateUtil.current() / 1000);
            String jsonBody = StrUtil.format("{\"count\":50,\"page\":1,\"filename\":\"Attack_monitoring_log\",\"action_mask\":[]" +
                    ",\"party_3rd_mask\":[],\"type_mask\":[],\"severity_mask\":[],\"r_s_time\":\"{}\",\"r_e_time\":\"{}\"" +
                    ",\"r_sip\":\"\",\"r_dip\":\"\",\"country\":255,\"province\":255,\"cmsn\":\"\"" +
                    ",\"reqCheckUrl\":\"/api/v1/logsystem/atkmntlog/query\"}", startTime, endTime);
            String xSign = encrypt(jsonBody, xTimestamp, xNonce);
            String repsBody = HttpRequest.post(ipCookie.getUrl() + ATKMNTLOG_URL)
                    .header("Cookie", ipCookie.getCookie())
                    .header("Content-Type", ContentType.JSON.getValue())
                    .header("X-Appkey", "frontend")
                    .header("X-Timestamp", xTimestamp)
                    .header("X-Csrf-Access-Token", ReUtil.getGroup1("csrf_access_token=([^;]+)", ipCookie.getCookie()))
                    .header("X-Csrf_Refresh_Token", ReUtil.getGroup1("csrf_refresh_token=([^;]+)", ipCookie.getCookie()))
                    .header("X-Nonce", xNonce)
                    .header("X-Sign", xSign)
                    .timeout(10000)
                    .body(jsonBody)
                    .execute().body();
            Console.log("query api:{} repsBody:{}",ipCookie.getUrl(),repsBody);
        } catch (Exception e) {
            Console.error("query api:{} error:{}", ipCookie.getUrl(), e.getMessage());
        }
    }
}
