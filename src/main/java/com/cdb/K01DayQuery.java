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

public class K01DayQuery  extends ApiBase{

    static int beforeDay = 1;
    static String hourTime = "16:30:00";
    static int beforeHour = -24;
    static String endDate = DateUtil.today();
    static String startDate = "";

    public static void main() {
        try {
            TimeInterval timer = DateUtil.timer();
            settingParam();
            Scanner scanner = new Scanner(System.in);
            Console.log("日期" + endDate + "往前查询几天(默认" + beforeDay + "天)，请输入1~100数字:");
            while(true) {
                String input1 = scanner.nextLine();
                if (NumberUtil.isInteger(input1)) {
                    beforeDay = Integer.parseInt(input1);
                    if (beforeDay > 0 && beforeDay < 100) {
                        break;
                    }else{
                        Console.error("输入错误，请重新输入!");
                    }
                }
            }
            startDate = getBeforeDate(endDate, --beforeDay);
            String jsonStr = FileUtil.readUtf8String(ABSOLUTE_PATH + "ip_token.json");
            List<IpCookie> ipCookieList = JSONUtil.toList(jsonStr, IpCookie.class);
            List<String> dateListBetween = getDateListBetween(startDate, endDate);
            List<DataTotal> dataTotalList = new ArrayList<>();
            for (String time : dateListBetween) {
                DataTotal dataTotal = new DataTotal();
                String endTime = time + StrUtil.SPACE + hourTime;
                String startTime = DateUtil.format(DateUtil.offsetHour(DateUtil.parse(endTime), beforeHour), "yyyy-MM-dd HH:mm:ss");
                dataTotal.setDate("["+startTime + "~" + endTime+"]");
                Console.log(dataTotal.getDate());
                ipCookieList.parallelStream().forEach(ipCookie -> {
                    Long logTotal = atkmntlog(ipCookie, startTime, endTime);
                    if (StrUtil.contains(ipCookie.getUrl(), IP.IP1)) {
                        dataTotal.setLog1Total(logTotal);
                    } else if (StrUtil.contains(ipCookie.getUrl(), IP.IP2)) {
                        dataTotal.setLog2Total(logTotal);
                    } else if (StrUtil.contains(ipCookie.getUrl(), IP.IP3)) {
                        dataTotal.setLog3Total(logTotal);
                    } else if (StrUtil.contains(ipCookie.getUrl(), IP.IP4)) {
                        dataTotal.setLog4Total(logTotal);
                    } else if (StrUtil.contains(ipCookie.getUrl(), IP.IP5)) {
                        dataTotal.setLog5Total(logTotal);
                    } else if (StrUtil.contains(ipCookie.getUrl(), IP.IP6)) {
                        dataTotal.setLog6Total(logTotal);
                    }
                });
                dataTotal.setLog7Total(ip7Total);
                dataTotal.setAllLogTotal(dataTotal.getLog1Total() + dataTotal.getLog2Total() + dataTotal.getLog3Total()
                        + dataTotal.getLog4Total() + dataTotal.getLog5Total() + dataTotal.getLog6Total() + dataTotal.getLog7Total());
                dataTotalList.add(dataTotal);
            }

            for (DataTotal dataTotal : dataTotalList) {
                System.out.println("----------------------------------------");
                Console.log("日期:" + dataTotal.getDate());
                Console.log(IP.IP1 + "=>" + dataTotal.getLog1Total());
                Console.log(IP.IP2 + "=>" + dataTotal.getLog2Total());
                Console.log(IP.IP3 + "=>" + dataTotal.getLog3Total());
                Console.log(IP.IP4 + "=>" + dataTotal.getLog4Total());
                Console.log(IP.IP5 + "=>" + dataTotal.getLog5Total());
                Console.log(IP.IP6 + "=>" + dataTotal.getLog6Total());
                Console.log(IP.IP7 + "=>" + dataTotal.getLog7Total());
                Console.log("合计=>" + dataTotal.getAllLogTotal());
            }
            writeExcel(dataTotalList);
            Console.log("OK!用时:{}秒", timer.intervalSecond());
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            ThreadUtil.sleep(Integer.MAX_VALUE);
        }
    }

    private static void settingParam() {
        String c_hourTime = setting.get("hour_time");
        if (StrUtil.isNotBlank(c_hourTime)) {
            hourTime = c_hourTime;
        }
        String c_beforeDay = setting.get("before_day");
        if (StrUtil.isNotBlank(c_beforeDay)) {
            beforeDay = Integer.parseInt(c_beforeDay);
        }
        String c_end_date = setting.get("end_date");
        if (StrUtil.isNotBlank(c_end_date)) {
            endDate = c_end_date;
        }
        String c_ip7Total = setting.get("ip7_total");
        if (StrUtil.isNotBlank(c_ip7Total)) {
            ip7Total = Long.parseLong(c_ip7Total);
        }
    }

    private static void writeExcel(List<DataTotal> dataTotalList) {
        if (CollectionUtil.isEmpty(dataTotalList)) {
            Console.error("生成文件失败，数据为空");
            return;
        }
        File file = new File(ABSOLUTE_PATH + "K01_" + DateUtil.format(DateTime.now(), "yyyyMMddHHmmss") + ".xlsx");
        FileUtil.del(file);
        ExcelWriter writer = ExcelUtil.getWriter(file)
                .addHeaderAlias("date", "日期")
                .addHeaderAlias("log1Total", IP.IP1)
                .addHeaderAlias("log2Total", IP.IP2)
                .addHeaderAlias("log3Total", IP.IP3)
                .addHeaderAlias("log4Total", IP.IP4)
                .addHeaderAlias("log5Total", IP.IP5)
                .addHeaderAlias("log6Total", IP.IP6)
                .addHeaderAlias("log7Total", IP.IP7)
                .addHeaderAlias("allLogTotal", "合计");
        writer.setOnlyAlias(true);
        writer.getStyleSet().setBorder(BorderStyle.NONE, IndexedColors.AUTOMATIC)
                .setAlign(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        // 一次性写出内容，使用默认样式，强制输出标题
        writer.write(dataTotalList, true);

        //设置自适应所有列列宽
        Sheet sheet = writer.getSheet();
        int columnCount = sheet.getRow(0).getLastCellNum();
        IntStream.range(0, columnCount).forEach(colIndex -> {
            sheet.autoSizeColumn(colIndex);
            sheet.setColumnWidth(colIndex, sheet.getColumnWidth(colIndex) + 2 * 256);
        });
        // 关闭writer，释放内存
        writer.close();
        Console.log("生成文件：{},行数：{}", file.getName(), dataTotalList.size());
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
}


