package com.cctv.security;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;

/**
 * @author jiqq
 * @date 2024/3/25
 * @description
 */
public class WarnFileMerge {

    //获取当前用户的执行路径
    public static final String ABSOLUTE_PATH = Paths.get("").toAbsolutePath()+File.separator;

    public static final String GENE_FILE_SUFFIX = "_new.xlsx";

    public static void main(String sourceFileName) {
        try {
            if(StrUtil.isBlank(sourceFileName)){
                Console.log("未指定加载的文件");
                return;
            }
            if (!FileUtil.exist(ABSOLUTE_PATH + sourceFileName)) {
                Console.log("【{}】文件不存在",sourceFileName);
                return;
            }
            Console.log("加载文件【{}】",sourceFileName);
            TimeInterval timer = DateUtil.timer();
            writeExcel(FileNameUtil.mainName(sourceFileName) + GENE_FILE_SUFFIX,readExcel(sourceFileName));
            Console.log("全部处理完成,耗时:{}秒",timer.intervalSecond());
        }catch (Exception ex){
            Console.error("失败-> 【{}】解析错误！",sourceFileName);
            ex.printStackTrace();
        }finally {
            ThreadUtil.sleep(Long.MAX_VALUE);
        }
    }

    private static List<WarnLog> readExcel(String sourceFileName) {
        ExcelReader reader = ExcelUtil.getReader(ABSOLUTE_PATH+sourceFileName)
                    .addHeaderAlias("生成告警时间", "warnTime")
                    .addHeaderAlias("告警名称", "warnName")
                    .addHeaderAlias("威胁等级", "warnLevel")
                    .addHeaderAlias("攻击链阶段", "warnStage")
                    .addHeaderAlias("攻击结果", "warnResult")
                    .addHeaderAlias("状态", "warnStatus")
                    .addHeaderAlias("攻击者ip", "attackerIp")
                    .addHeaderAlias("受害者ip", "victimIp");
        List<WarnLog> logs = reader.readAll(WarnLog.class);
        Console.log("总行数：{}",logs.size());

        List<WarnLog> groupLogs = new ArrayList<>();
        final Map<String, List<WarnLog>> rowList = logs.stream().peek(e-> e.setGroupType(e.getWarnName()+e.getAttackerIp())).collect(Collectors.groupingBy(
                WarnLog::getGroupType, Collectors.toList()));
        rowList.forEach((groupName,groupRows)->{
            String victimIp =  groupRows.stream().map(WarnLog::getVictimIp)
                    .collect(Collectors.joining(","));
            groupLogs.add(WarnLog.builder()
                                  .warnTime(groupRows.get(0).getWarnTime())
                                  .warnName(groupRows.get(0).getWarnName())
                                  .warnLevel(groupRows.get(0).getWarnLevel())
                                  .warnResult(groupRows.get(0).getWarnResult())
                                  .warnStage(groupRows.get(0).getWarnStage())
                                  .warnStatus(groupRows.get(0).getWarnStatus())
                                  .attackerIp(groupRows.get(0).getAttackerIp())
                                  .victimIp(victimIp).build());
        });
        return groupLogs.stream().sorted(Comparator.comparing(WarnLog::getWarnTime)).collect(Collectors.toList());
    }

    private static void writeExcel(String geneFileName,List<WarnLog> finalLogs) {
        File file = new File(ABSOLUTE_PATH+ geneFileName);
        FileUtil.del(file);
        ExcelWriter writer = ExcelUtil.getWriter(file)
                .addHeaderAlias("warnTime", "生成告警时间")
                .addHeaderAlias("warnName", "告警名称")
                .addHeaderAlias("warnLevel", "威胁等级")
                .addHeaderAlias("warnStage", "攻击链阶段")
                .addHeaderAlias("warnResult", "攻击结果")
                .addHeaderAlias("warnStatus", "状态")
                .addHeaderAlias("attackerIp", "攻击者ip")
                .addHeaderAlias("victimIp", "受害者ip");
        writer.setOnlyAlias(true);
        writer.getStyleSet().setBorder(BorderStyle.NONE, IndexedColors.AUTOMATIC)
                .setAlign(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        // 一次性写出内容，使用默认样式，强制输出标题
        writer.write(finalLogs, true);
        // 关闭writer，释放内存
        writer.close();
        Console.log("成功->生成文件：{},行数：{}",geneFileName, finalLogs.size());
    }

    @Data
    @Builder
    static class WarnLog{
        private String warnTime; //生成告警时间
        private String warnName; //告警名称
        private String warnLevel; //威胁等级
        private String warnStage; //攻击链阶段
        private String warnResult; //攻击结果
        private String warnStatus; //状态
        private String attackerIp; //攻击者ip
        private String victimIp; //受害者ip
        private String groupType; //分组字段
    }

}
