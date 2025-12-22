package com.cdb;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.digest.Digester;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.hutool.setting.Setting;
import lombok.Data;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.IntStream;

public class K01Query {
    static final String ABSOLUTE_PATH = Paths.get("").toAbsolutePath() + File.separator ;
    static final String ATKMNTLOG_URL = "/api/v1/logsystem/atkmntlog/query";
    public static final Setting setting = new Setting(ABSOLUTE_PATH + File.separator + "config.setting");
    static int beforeDay = 1;
    static String hourTime = "16:30:00";
    static int beforeHour = -24;
    static long ip7Total = 100L;
    static String endDate =  DateUtil.today();
    static String startDate = "";
    static String KEY = "ed428495-29cc-4a2c-a9dd-4f106af9c104";

    static class IP {
        static final String IP1 = "10.4.6.255";
        static final String IP2 = "10.4.40.226";
        static final String IP3 = "10.32.3.33";
        static final String IP4 = "10.32.64.97";
        static final String IP5 = "192.168.254.11";
        static final String IP6 = "192.168.254.10";
        static final String IP7 = "10.42.255.90";
    }

    public static void main() {
        try {
            TimeInterval timer = DateUtil.timer();
            settingParam();
            Scanner scanner = new Scanner(System.in);
            Console.log("日期" + endDate + "往前查询几天(默认" + beforeDay + "天)，请输入1~100数字:");
            String input1 = scanner.nextLine();
            if (NumberUtil.isInteger(input1)) {
                beforeDay = Integer.parseInt(input1);
                if (beforeDay > 100 || beforeDay <= 0) {
                    Console.error("输入错误，请关闭当前窗口，重新打开窗口!");
                    return;
                }
            }
            startDate = getBeforeDate(endDate, --beforeDay);
            String jsonStr = FileUtil.readUtf8String(ABSOLUTE_PATH + "ip_token.json");
            List<IpCookie> ipCookieList = JSONUtil.toList(jsonStr, IpCookie.class);
            for (IpCookie ipCookie : ipCookieList) {
                System.out.println("url：" + ipCookie.getUrl());
            }
            List<String> dateListBetween = getDateListBetween(startDate, endDate);
            List<IpTotal> ipTotalList = new ArrayList<>();
            for (String time : dateListBetween) {
                IpTotal ipTotal = new IpTotal();
                String endTime = time + StrUtil.SPACE + hourTime;
                String startTime = DateUtil.format(DateUtil.offsetHour(DateUtil.parse(endTime), beforeHour), "YYYY-MM-dd HH:mm:ss");
                ipTotal.setDate(startTime + "~" + endTime);
                for (IpCookie ipCookie : ipCookieList) {
                    Long total = transform(ipCookie,startTime,endTime);
                    if (StrUtil.contains(ipCookie.getUrl(), IP.IP1)) {
                        ipTotal.setIp1Total(total);
                    } else if (StrUtil.contains(ipCookie.getUrl(), IP.IP2)) {
                        ipTotal.setIp2Total(total);
                    } else if (StrUtil.contains(ipCookie.getUrl(), IP.IP3)) {
                        ipTotal.setIp3Total(total);
                    } else if (StrUtil.contains(ipCookie.getUrl(), IP.IP4)) {
                        ipTotal.setIp4Total(total);
                    } else if (StrUtil.contains(ipCookie.getUrl(), IP.IP5)) {
                        ipTotal.setIp5Total(total);
                    } else if (StrUtil.contains(ipCookie.getUrl(), IP.IP6)) {
                        ipTotal.setIp6Total(total);
                    }
                }
                ipTotal.setIp7Total(ip7Total);
                ipTotal.setAllTotal(ipTotal.ip1Total + ipTotal.ip2Total + ipTotal.ip3Total + ipTotal.ip4Total + ipTotal.ip5Total + ipTotal.ip6Total + ipTotal.ip7Total);
                ipTotalList.add(ipTotal);
            }

            for (IpTotal ipTotal : ipTotalList) {
                System.out.println("------------------------------------------");
                Console.log("日期:" + ipTotal.getDate());
                Console.log(IP.IP1 + "=>" + ipTotal.getIp1Total());
                Console.log(IP.IP2 + "=>" + ipTotal.getIp2Total());
                Console.log(IP.IP3 + "=>" + ipTotal.getIp3Total());
                Console.log(IP.IP4 + "=>" + ipTotal.getIp4Total());
                Console.log(IP.IP5 + "=>" + ipTotal.getIp5Total());
                Console.log(IP.IP6 + "=>" + ipTotal.getIp6Total());
                Console.log(IP.IP7 + "=>" + ipTotal.getIp7Total());
                Console.log("合计=>" + ipTotal.getAllTotal());
            }
            writeExcel(ipTotalList);
            Console.log("OK!用时:{}秒",timer.intervalSecond());
        }finally {
            ThreadUtil.sleep(Integer.MAX_VALUE);
        }
    }

    private static void settingParam() {
        String c_hourTime =setting.get("hour_time");
        if (StrUtil.isNotBlank(c_hourTime)) {
            hourTime = c_hourTime;
        }
        String c_beforeDay =setting.get("before_day");
        if (StrUtil.isNotBlank(c_beforeDay)) {
            beforeDay = Integer.parseInt(c_beforeDay);
        }
        String c_end_date =setting.get("end_date");
        if (StrUtil.isNotBlank(c_end_date)) {
            endDate = c_end_date;
        }
        String c_ip7Total =setting.get("ip7_total");
        if (StrUtil.isNotBlank(c_ip7Total)) {
            ip7Total = Long.parseLong(c_ip7Total);
        }
    }

    public static Long transform(IpCookie ipCookie, String startTime,String endTime){
        try {
            String xNonce = String.valueOf(UUID.randomUUID());
            String xTimestamp = String.valueOf(DateUtil.current() / 1000);
            String jsonBody = StrUtil.format("{\"count\":50,\"page\":1,\"filename\":\"Attack_monitoring_log\",\"action_mask\":[]" +
                    ",\"party_3rd_mask\":[],\"type_mask\":[],\"severity_mask\":[],\"r_s_time\":\"{}\",\"r_e_time\":\"{}\"" +
                    ",\"r_sip\":\"\",\"r_dip\":\"\",\"country\":255,\"province\":255,\"cmsn\":\"\"" +
                    ",\"reqCheckUrl\":\"/api/v1/logsystem/atkmntlog/query\"}",startTime,endTime);
            String xSign = encrypt(jsonBody,xTimestamp,xNonce);
            String repsBody = HttpRequest.post(ipCookie.getUrl() + ATKMNTLOG_URL)
                    .header("Cookie", ipCookie.getCookie())
                    .header("Content-Type", ContentType.JSON.getValue())
                    .header("X-Appkey" , "frontend")
                    .header("X-Timestamp" , xTimestamp)
                    .header("X-Csrf-Access-Token", ReUtil.getGroup1("csrf_access_token=([^;]+)", ipCookie.getCookie()))
                    //.header("Csrf_refresh_token", ReUtil.getGroup1("csrf_refresh_token=([^;]+)", ipCookie.getCookie()))
                    .header("X-Nonce",xNonce)
                    .header("X-Sign",xSign)
                    .timeout(5000)
                    .body(jsonBody)
                    .execute().body();
            Console.log("query api:{} ok!", ipCookie.getUrl());
            //Console.log("repsBody:"+repsBody);
            JSONObject dataObj = JSONUtil.parseObj(repsBody).getJSONObject("data");
            if (dataObj == null) {
                dataObj = new JSONObject(); // 空节点兜底
            }
            return dataObj.getLong("total", 0L);
        }catch (Exception e){
            Console.error("query api:{} error:{}",ipCookie.getUrl(),e.getMessage());
            return 0L;
        }
    }

    public static String encrypt(String jsonBody,String xTimestamp,String xNonce){
        String str1 = Base64.encode(DigestUtil.sha256(jsonBody+xNonce));
        String str2 = StrUtil.format("x-date: {}\ndigest: SHA-256={}",xTimestamp,str1);
        return Base64.encode(HmacUtils.hmacSha256(KEY,str2));
    }

    private static void writeExcel(List<IpTotal> ipTotalList) {
        if (CollectionUtil.isEmpty(ipTotalList)) {
            Console.error("生成文件失败，数据为空");
            return;
        }
        File file = new File(ABSOLUTE_PATH + "K01_"+DateUtil.format(DateTime.now(), "yyyyMMddHHmmss")+".xlsx");
        FileUtil.del(file);
        ExcelWriter writer = ExcelUtil.getWriter(file)
                .addHeaderAlias("date", "日期")
                .addHeaderAlias("ip1Total", IP.IP1)
                .addHeaderAlias("ip2Total", IP.IP2)
                .addHeaderAlias("ip3Total", IP.IP3)
                .addHeaderAlias("ip4Total", IP.IP4)
                .addHeaderAlias("ip5Total", IP.IP5)
                .addHeaderAlias("ip6Total", IP.IP6)
                .addHeaderAlias("ip7Total", IP.IP7)
                .addHeaderAlias("allTotal", "合计");
        writer.setOnlyAlias(true);
        writer.getStyleSet().setBorder(BorderStyle.NONE, IndexedColors.AUTOMATIC)
                .setAlign(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        // 一次性写出内容，使用默认样式，强制输出标题
        writer.write(ipTotalList, true);

        //设置自适应所有列列宽
        Sheet sheet = writer.getSheet();
        int columnCount = sheet.getRow(0).getLastCellNum();
        IntStream.range(0, columnCount).forEach(colIndex -> {
            sheet.autoSizeColumn(colIndex);
            sheet.setColumnWidth(colIndex, sheet.getColumnWidth(colIndex) + 2 * 256);
        });
        // 关闭writer，释放内存
        writer.close();
        Console.log("生成文件：{},行数：{}", file.getName(), ipTotalList.size());
    }


    public static List<String> getDateListBetween(String start, String end) {
        // 入参非空+日期顺序校验
        if (StrUtil.isBlank(start) || StrUtil.isBlank(end)) throw new IllegalArgumentException("日期不能为空");
        DateTime startDt = DateUtil.parse(start, "yyyy-MM-dd");
        DateTime endDt = DateUtil.parse(end, "yyyy-MM-dd");
        if (startDt.isAfter(endDt)) throw new IllegalArgumentException("开始日期不能晚于结束日期");

        // 核心遍历逻辑
        List<String> dateList = new ArrayList<>();
        DateTime currDt = startDt;
        while (!currDt.isAfter(endDt)) {
            dateList.add(DateUtil.format(currDt, "yyyy-MM-dd"));
            currDt = DateUtil.offsetDay(currDt, 1);
        }
        return dateList;
    }

    public static String getBeforeDate(String dateStr, int days) {
        // 入参校验
        if (StrUtil.isBlank(dateStr)) throw new IllegalArgumentException("日期不能为空");
        if (days < 0) throw new IllegalArgumentException("偏移天数不能为负数");

        // 核心逻辑：解析日期 + 往前偏移days天 + 格式化
        return DateUtil.format(
                DateUtil.offsetDay(DateUtil.parse(dateStr, "yyyy-MM-dd"), -days),
                "yyyy-MM-dd"
        );
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


    @Data
    static class IpCookie {
        private String url;
        private String cookie;
    }
}


