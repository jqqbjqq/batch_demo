package com.cctv.security;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.compress.utils.Lists;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.util.StringUtil;

/**
 * @author jiqq
 * @date 2024/4/11
 * @description
 */
public class VideoWarn {

    //获取当前用户的执行路径
    public static final String ABSOLUTE_PATH = Paths.get("").toAbsolutePath()+File.separator;

    public static final String GENE_FILE_SUFFIX = "_new.xlsx";

    public static void main(String sourceFileNameList) {
        try {
            if(StrUtil.isBlank(sourceFileNameList)){
                Console.log("未指定加载的文件");
                return;
            }
            String[] sourceFileNames = sourceFileNameList.split(",");
            Console.log("找到指定的文件{}", (Object) sourceFileNames);
            Scanner scanner = new Scanner(System.in);
            Console.log("请输入需要处理的警告级别，1：高危 2：中危 3：低危");
            String inputLevel = scanner.nextLine();
            TimeInterval timer = DateUtil.timer();
            Arrays.stream(sourceFileNames).parallel().forEach(fileName->{ //
                if (!FileUtil.exist(ABSOLUTE_PATH + fileName)) {
                    Console.error("失败-> 【{}】文件不存在！",fileName);
                    return;
                }
                try{
                    writeExcel(FileNameUtil.mainName(fileName) + GENE_FILE_SUFFIX,readExcel(fileName,inputLevel));
                }catch (Exception ex){
                    Console.error("失败-> 【{}】解析错误！",fileName);
                    ex.printStackTrace();
                }
            });
            Console.log("全部处理完成,耗时:{}秒",timer.intervalSecond());
        }finally {
            ThreadUtil.sleep(Long.MAX_VALUE);
        }
    }

    private static List<WarnLog> readExcel(String sourceFileName,String inputLevel) {
        ExcelReader reader = ExcelUtil.getReader(ABSOLUTE_PATH+sourceFileName)
                .addHeaderAlias("站点名称", "siteName")
                .addHeaderAlias("站点地址", "siteAddr")
                .addHeaderAlias("所属资产组", "assetGroup")
                .addHeaderAlias("安全状态", "secureStatus")
                .addHeaderAlias("可用性", "usability")
                .addHeaderAlias("端口服务组件", "postService")
                .addHeaderAlias("所属用户姓名", "userName")
                .addHeaderAlias("漏洞信息", "bugInfo")
                .addHeaderAlias("资产类型", "assetType");
        List<WarnLog> logs = reader.readAll(WarnLog.class);
        //Console.log("【{}】总行数：{}",sourceFileName,logs.size());
        List<WarnLog> allLogs = Lists.newArrayList();
        logs.forEach(e->{
            if(StringUtil.isBlank(e.getBugInfo())){
                return;
            }
            String[] bugList = e.getBugInfo().replaceAll("【", "【】【").split("【】");
            List<WarnLog> groupLogs = new ArrayList<>(bugList.length);
            for (String bug : bugList) {
                if(StrUtil.isBlank(bug)){continue;}
                WarnLog warnLog = WarnLog.builder().siteName(e.getSiteName()).siteAddr(e.getSiteAddr())
                        .assetGroup(e.getAssetGroup()).secureStatus(e.getSecureStatus()).usability(e.getUsability())
                        .postService(e.getPostService())
                        .userName(e.getUserName()).bugInfo(bug).assetType(e.getAssetType()).build();
                if(inputLevel.contains("1")){
                   if(bug.contains("【高危】")){
                       groupLogs.add(warnLog);
                    }
                }
                if(inputLevel.contains("2")){
                    if(bug.contains("【中危】")){
                        groupLogs.add(warnLog);
                    }
                }
                if(inputLevel.contains("3")){
                    if(bug.contains("【低危】")){
                        groupLogs.add(warnLog);
                    }
                }
            }
            allLogs.addAll(groupLogs);
        });
        return allLogs;
    }

    private static void writeExcel(String geneFileName,List<WarnLog> finalLogs) {
        File file = new File(ABSOLUTE_PATH+ geneFileName);
        FileUtil.del(file);
        ExcelWriter writer = ExcelUtil.getWriter(file)
                .addHeaderAlias("siteName", "站点名称")
                .addHeaderAlias("siteAddr", "站点地址")
                .addHeaderAlias("assetGroup", "所属资产组")
                .addHeaderAlias("secureStatus", "安全状态")
                .addHeaderAlias("usability", "可用性")
                .addHeaderAlias("postService", "端口服务组件")
                .addHeaderAlias("userName", "所属用户姓名")
                .addHeaderAlias("bugInfo", "漏洞信息")
                .addHeaderAlias("assetType", "资产类型");
        writer.setOnlyAlias(true);
        writer.getStyleSet().setAlign(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        // 一次性写出内容，使用默认样式，强制输出标题
        writer.write(finalLogs, true);
        // 关闭writer，释放内存
        writer.close();
        Console.log("成功-> 生成文件【{}】,行数：{}", geneFileName, finalLogs.size());
    }

    @Data
    @Builder
    static class WarnLog{
        private String siteName; //站点名称
        private String siteAddr; //站点地址
        private String assetGroup; //所属资产组
        private String secureStatus; //安全状态
        private String usability; //可用性
        private String postService; //端口服务组件
        private String userName; //所属用户姓名
        private String bugInfo; //漏洞信息
        private String assetType; //资产类型
    }

}
