package com.cdb;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import java.util.List;
import java.util.Scanner;

public class K01DateQuery extends ApiBase{

    private static void settingParam() {
        String c_log7Total = setting.get("log7_total");
        if (StrUtil.isNotBlank(c_log7Total)) {
            log7Total = Long.parseLong(c_log7Total);
        }
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
            String p_startTime = "";
            while(true) {
                p_startTime = scanner.nextLine();
                if (isValidDateTime(p_startTime)) {
                    break;
                }else{
                    Console.error("开始日期格式错误，请重新输入!");
                }
            }

            Console.log("请输入结束日期，格式为(YYYY-MM-dd HH:mm:ss):");
            String p_endTime = "";
            while(true) {
                p_endTime = scanner.nextLine();
                if (isValidDateTime(p_endTime)) {
                    break;
                }else{
                    Console.error("结束日期格式错误，请重新输入!");
                }
            }

            String jsonStr = FileUtil.readUtf8String(ABSOLUTE_PATH + "ip_token.json");
            List<IpCookie> ipCookieList = JSONUtil.toList(jsonStr, IpCookie.class);
            DataTotal dataTotal = new DataTotal();
            final String startTime = p_startTime;
            final String endTime = p_endTime;
            dataTotal.setDate("["+startTime + "~" + endTime+"]");
            Console.log(dataTotal.getDate());
            ipCookieList.parallelStream().forEach(ipCookie -> {
                Long logTotal = atkmntlog(ipCookie, startTime, endTime);
                Long ipTotal = atkip(ipCookie, startTime, endTime);
                if (StrUtil.contains(ipCookie.getUrl(), IP.IP1)) {
                    dataTotal.setLog1Total(logTotal);
                    dataTotal.setIp1Total(ipTotal);
                } else if (StrUtil.contains(ipCookie.getUrl(), IP.IP2)) {
                    dataTotal.setLog2Total(logTotal);
                    dataTotal.setIp2Total(ipTotal);
                } else if (StrUtil.contains(ipCookie.getUrl(), IP.IP3)) {
                    dataTotal.setLog3Total(logTotal);
                    dataTotal.setIp3Total(ipTotal);
                } else if (StrUtil.contains(ipCookie.getUrl(), IP.IP4)) {
                    dataTotal.setLog4Total(logTotal);
                    dataTotal.setIp4Total(ipTotal);
                } else if (StrUtil.contains(ipCookie.getUrl(), IP.IP5)) {
                    dataTotal.setLog5Total(logTotal);
                    dataTotal.setIp5Total(ipTotal);
                } else if (StrUtil.contains(ipCookie.getUrl(), IP.IP6)) {
                    dataTotal.setLog6Total(logTotal);
                    dataTotal.setIp6Total(ipTotal);
                }
            });
            dataTotal.setLog7Total(log7Total);
            dataTotal.setIp7Total(ip7Total);
            dataTotal.setAllLogTotal(dataTotal.getLog1Total() + dataTotal.getLog2Total() + dataTotal.getLog3Total()
                    + dataTotal.getLog4Total() + dataTotal.getLog5Total() + dataTotal.getLog6Total() + dataTotal.getLog7Total());
            dataTotal.setAllIpTotal(dataTotal.getIp1Total() + dataTotal.getIp2Total() + dataTotal.getIp3Total()
                    + dataTotal.getIp4Total() + dataTotal.getIp5Total() + dataTotal.getIp6Total() + dataTotal.getIp7Total());
            System.out.println("----------------------------------------");
            Console.log("日期:" + dataTotal.getDate());
            Console.log(IP.IP1 + " => log:" + dataTotal.getLog1Total() + "  ip:" + dataTotal.getIp1Total());
            Console.log(IP.IP2 + " => log:" + dataTotal.getLog2Total() + "  ip:" + dataTotal.getIp2Total());
            Console.log(IP.IP3 + " => log:" + dataTotal.getLog3Total() + "  ip:" + dataTotal.getIp3Total());
            Console.log(IP.IP4 + " => log:" + dataTotal.getLog4Total() + "  ip:" + dataTotal.getIp4Total());
            Console.log(IP.IP5 + " => log:" + dataTotal.getLog5Total() + "  ip:" + dataTotal.getIp5Total());
            Console.log(IP.IP6 + " => log:" + dataTotal.getLog6Total() + "  ip:" + dataTotal.getIp6Total());
            Console.log(IP.IP7 + " => log:" + dataTotal.getLog7Total() + "  ip:" + dataTotal.getIp7Total());
            Console.log("合计 => log:" + dataTotal.getAllLogTotal()+ "  ip:"+dataTotal.getAllIpTotal());
            Console.log("OK!用时:{}秒", timer.intervalSecond());
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            ThreadUtil.sleep(Integer.MAX_VALUE);
        }
    }

}


