package com.cdb;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.setting.Setting;
import lombok.Data;

import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;

public class ApiBase {
    static final String ABSOLUTE_PATH = Paths.get("").toAbsolutePath() + File.separator;
    static final String ATKMNTLOG_URL = "/api/v1/logsystem/atkmntlog/query";
    public static final Setting setting = new Setting(ABSOLUTE_PATH + File.separator + "config.setting");
    static String KEY = "ed428495-29cc-4a2c-a9dd-4f106af9c104";
    static long ip7Total = 100L;

    static class IP {
        static final String IP1 = "10.4.6.225";
        static final String IP2 = "10.4.40.226";
        static final String IP3 = "10.32.3.33";
        static final String IP4 = "10.32.64.97";
        static final String IP5 = "192.168.254.11";
        static final String IP6 = "192.168.254.10";
        static final String IP7 = "10.42.255.90";
    }

    public static Long transform(K01DayQuery.IpCookie ipCookie, String startTime, String endTime) {
        try {
            Console.log("query api:{} ", ipCookie.getUrl());
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
                    //.header("Csrf_refresh_token", ReUtil.getGroup1("csrf_refresh_token=([^;]+)", ipCookie.getCookie()))
                    .header("X-Nonce", xNonce)
                    .header("X-Sign", xSign)
                    .timeout(10000)
                    .body(jsonBody)
                    .execute().body();
            //Console.log("repsBody:"+repsBody);
            JSONObject dataObj = JSONUtil.parseObj(repsBody).getJSONObject("data");
            if (dataObj == null) {
                dataObj = new JSONObject(); // 空节点兜底
            }
            return dataObj.getLong("total", 0L);
        } catch (Exception e) {
            Console.error("query api:{} error:{}", ipCookie.getUrl(), e.getMessage());
            return 0L;
        }
    }

    public static String encrypt(String jsonBody, String xTimestamp, String xNonce) {
        String str1 = Base64.encode(DigestUtil.sha256(jsonBody + xNonce));
        String str2 = StrUtil.format("x-date: {}\ndigest: SHA-256={}", xTimestamp, str1);
        return Base64.encode(SecureUtil.hmacSha256(KEY).digest(str2));
    }

    public static boolean isValidDateTime(String dateStr) {
        // 1. 前置过滤：空值/长度不符直接返回false
        if (StrUtil.isBlank(dateStr) || dateStr.trim().length() != 19) {
            return false;
        }
        String format = "yyyy-MM-dd HH:mm:ss";
        try {
            // 2. 尝试解析日期（低版本DateUtil.parse）
            DateTime dateTime = DateUtil.parse(dateStr, format);
            // 3. 反向校验：解析后的日期转回字符串，必须与原字符串一致（严格匹配）
            String formatStr = DateUtil.format(dateTime, format);
            return StrUtil.equals(dateStr.trim(), formatStr);
        } catch (Exception e) {
            // 解析失败（如13月、25时）直接返回false
            return false;
        }
    }

    @Data
    static class IpCookie {
        private String url;
        private String cookie;
    }

    @Data
    static class IpTotal {
        private String date;
        private Long ip1Total = 0L;
        private Long ip2Total = 0L;
        private Long ip3Total = 0L;
        private Long ip4Total = 0L;
        private Long ip5Total = 0L;
        private Long ip6Total = 0L;
        private Long ip7Total = 0L;
        private Long allTotal = 0L;
    }

}
