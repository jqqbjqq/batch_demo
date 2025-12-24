package com.cdb;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.IntStream;

public class K01DateQuery extends ApiBase{

    private static void settingParam() {
        String c_ip7Total = setting.get("ip7_total");
        if (StrUtil.isNotBlank(c_ip7Total)) {
            ip7Total = Long.parseLong(c_ip7Total);
        }
    }

    public static void main() {
        try {
            TimeInterval timer = DateUtil.timer();
            settingParam();

            Scanner scanner = new Scanner(System.in);
            Console.log("请输入开始日期，格式为(YYYY-MM-dd HH:mm:ss):");
            String startTime = scanner.nextLine();
            if (!isValidDateTime(startTime)) {
                Console.error("输入错误，请关闭当前窗口，重新打开窗口!");
                return;
            }

            Console.log("请输入结束日期，格式为(YYYY-MM-dd HH:mm:ss):");
            String endTime = scanner.nextLine();
            if (!isValidDateTime(endTime)) {
                Console.error("输入错误，请关闭当前窗口，重新打开窗口!");
                return;
            }

            String jsonStr = FileUtil.readUtf8String(ABSOLUTE_PATH + "ip_token.json");
            List<IpCookie> ipCookieList = JSONUtil.toList(jsonStr, IpCookie.class);

            IpTotal ipTotal = new IpTotal();
            ipTotal.setDate("["+startTime + "~" + endTime+"]");
            Console.log(ipTotal.getDate());
            ipCookieList.parallelStream().forEach(ipCookie -> {
                Long total = transform(ipCookie, startTime, endTime);
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
            });
            ipTotal.setIp7Total(ip7Total);
            ipTotal.setAllTotal(ipTotal.getIp1Total() + ipTotal.getIp2Total() + ipTotal.getIp3Total()
                    + ipTotal.getIp4Total() + ipTotal.getIp5Total() + ipTotal.getIp6Total() + ipTotal.getIp7Total());

            System.out.println("----------------------------------------");
            Console.log("日期:" + ipTotal.getDate());
            Console.log(IP.IP1 + "=>" + ipTotal.getIp1Total());
            Console.log(IP.IP2 + "=>" + ipTotal.getIp2Total());
            Console.log(IP.IP3 + "=>" + ipTotal.getIp3Total());
            Console.log(IP.IP4 + "=>" + ipTotal.getIp4Total());
            Console.log(IP.IP5 + "=>" + ipTotal.getIp5Total());
            Console.log(IP.IP6 + "=>" + ipTotal.getIp6Total());
            Console.log(IP.IP7 + "=>" + ipTotal.getIp7Total());
            Console.log("合计=>" + ipTotal.getAllTotal());
            Console.log("OK!用时:{}秒", timer.intervalSecond());
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            ThreadUtil.sleep(Integer.MAX_VALUE);
        }
    }

}


