package com.cctv.security;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.compress.utils.Lists;

/**
 * @author jiqq
 */
public class AssetsCheck {

    public static void main(String[] args) {
        TimeInterval timer = DateUtil.timer();

        String sysMappingPath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\系统名整理.xlsx";
        Map<String, SysMapping> sysMappingMap = sysMappingRead(sysMappingPath);

        String antiVirusPath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\防病毒";
        Map<String,List<Assets>> avMap = toMapBySysname(antiVirusRead(antiVirusPath));

        String auditHostPath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\堡垒机";
        Map<String,List<Assets>> ahMap = toMapBySysname(auditHostRead(auditHostPath));

        String frontLinePath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\一线资产";
        Map<String, String> fileMap = handleExcelFiles(frontLinePath);

        //many task
        fileMap.forEach((fileName, filePath) -> { //fileMap.stream().parallelStream().forEach
            Console.log("load filePath:{} ",filePath);
            List<String> sheetNameList = ExcelUtil.getReader(filePath).getSheetNames();
            filterSheetName(sheetNameList);
            if(sheetNameList.size()==1&&sheetNameList.get(0).startsWith("Sheet")){
                List<Assets> flList = Lists.newArrayList();
                sheetNameList.forEach(sheetName -> {
                    ExcelReader reader = ExcelUtil.getReader(new File(filePath), sheetName)
                            .addHeaderAlias("内网IP地址/云内地址", "ip")
                            .addHeaderAlias("信息系统名称", "sysname");
                    List<Assets> list = restoreData(reader.readAll(Assets.class));
                    Console.log("  -> sheetname:{} count:{}", sheetName, flList.size());
                    flList.addAll(list);
                });
                Map<String, List<Assets>> groupList = flList.stream().collect(Collectors.groupingBy(
                        Assets::getSysname, Collectors.toList()));
                groupList.forEach((sysname,list)-> {
                    dataHandler(flList, avMap, ahMap);
                });
                return;
            }
            if (StrUtil.contains(filePath, "近期确认的资产")) {
                List<Assets> flList = Lists.newArrayList();
                sheetNameList.forEach(sheetName -> {
                    ExcelReader reader = ExcelUtil.getReader(new File(filePath), sheetName)
                            .addHeaderAlias("资产IP", "ip")
                            .addHeaderAlias("资产名称", "sysname");
                    List<Assets> list = restoreData(reader.readAll(Assets.class));
                    Console.log("  -> sheetname:{} count:{}", sheetName, flList.size());
                    flList.addAll(list);
                });
                flList.forEach(e -> {
                    e.setSysname(FileNameUtil.getPrefix(fileName));
                });
                dataHandler(flList, avMap, ahMap);
            } else {
                sheetNameList.forEach(sheetName -> {
                    ExcelReader reader = ExcelUtil.getReader(new File(filePath), sheetName)
                            .addHeaderAlias("资产IP", "ip")
                            .addHeaderAlias("资产名称", "sysname")
                            .addHeaderAlias("安全域", "realm");
                    List<Assets> flList = restoreData(reader.readAll(Assets.class));
                    Console.log("  -> sheetname:{} count:{}", sheetName, flList.size());
                    //cleanupName(flList);
                    dataHandler(flList, avMap, ahMap);
                });
            }
        });
        Console.log("The total time is {}s", timer.intervalSecond());
    }

    private static void dataHandler(List<Assets> flList, Map<String,List<Assets>> avMap, Map<String,List<Assets>> ahMap) {
        Console.log("flList:{},avList:{},ahList:{}", flList.size(),avMap.size(),ahMap.size());
    }


    private static List<Assets> restoreData(List<Assets> list) {
        if(CollUtil.isEmpty(list)){
            return Lists.newArrayList();
        }
        return list.stream().filter(e -> StrUtil.isNotBlank(e.getIp())) //&&StrUtil.isNotBlank(e.getSysname())
                .peek(e -> {
                    e.setIp(e.getIp().trim());
                    if (StrUtil.isNotBlank(e.getRealm())) {
                        e.setSysname(splitName(e.getRealm()));
                    }
                    if (StrUtil.isBlank(e.getSysname())) {
                        e.setSysname(list.get(0).getSysname());
                    }
                    e.setSysname(e.getSysname().trim());
                }).collect(Collectors.toList());
    }

    private static String splitName(String str) {
        if(StrUtil.isBlank(str)){
            return "";
        }
        if(str.startsWith("/")){
            return str.split("/")[3];
        }
        return str.split("-")[0];
    }

    private static Map<String, List<Assets>> toMapBySysname(List<Assets> list) {
        return list.stream().collect(Collectors.groupingBy(
                Assets::getSysname, Collectors.toList()));
    }

    private static void filterSheetName(List<String> list) {
        CollUtil.removeAny(list, "help", "格式");
        list.removeIf(str -> str.startsWith("Sht"));
    }


    private static List<Assets> antiVirusRead(String dirPath) {
        Map<String, String> fileMap = handleExcelFiles(dirPath);
        List<Assets> allList = Lists.newArrayList();
        fileMap.forEach((name, path) -> {
            ExcelReader reader = ExcelUtil.getReader(path)
                    .addHeaderAlias("名称", "ip")
                    .addHeaderAlias("信息系统", "sysname");
            List<Assets> list = restoreData(reader.readAll(Assets.class));
            Console.log("name:{} count:{}", name, list.size());
            allList.addAll(list);
        });
        return allList;
    }

    private static List<Assets> auditHostRead(String dirPath) {
        Map<String, String> fileMap = handleExcelFiles(dirPath);
        List<Assets> allList = Lists.newArrayList();
        fileMap.forEach((name, path) -> {
            ExcelReader reader;
            if(path.contains("安恒运维审计")){
                 reader = ExcelUtil.getReader(path)
                        .addHeaderAlias("#主机IP", "ip")
                        .addHeaderAlias("主机组名称", "sysname");
            }else{
                 reader = ExcelUtil.getReader(path)
                        .addHeaderAlias("IP地址/域名(必填),多个用“;”拆分", "ip")
                        .addHeaderAlias("资源组(必填,支持中英文,数字,下划线,中划线,小数点,圆括号,中括号,空格)", "sysname");
            }
            List<Assets> list = restoreData(reader.readAll(Assets.class));
            Console.log("name:{} count:{}", name, list.size());
            allList.addAll(list);
        });
        return allList;
    }

    private static Map<String, SysMapping> sysMappingRead(String filePath) {
        ExcelReader reader = ExcelUtil.getReader(filePath)
                    .addHeaderAlias("一线系统", "frontLine")
                    .addHeaderAlias("防病毒系统", "antiVirus")
                    .addHeaderAlias("堡垒机系统", "auditHost");
        List<SysMapping> list = reader.readAll(SysMapping.class);
        Console.log("filePath:{} count:{}", filePath, list.size());
        Map<String, SysMapping> map = new HashMap<>();
        for (SysMapping mapping : list) {
            if(StrUtil.isBlank(mapping.getFrontLine())){
                continue;
            }
            mapping.setAntiVirusArr(mapping.getAntiVirus()!=null?mapping.getAntiVirus().split("\\|"):new String[]{});
            mapping.setAuditHostArr(mapping.getAuditHost()!=null?mapping.getAuditHost().split("\\|"):new String[]{});
            map.put(mapping.getFrontLine(),mapping);
        }
        return map;
    }

    public static Map<String, String> handleExcelFiles(String dirPath) {
        Map<String, String> fileMap = MapUtil.newHashMap();
        List<File> files = FileUtil.loopFiles(dirPath);
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && isExcel(file.getAbsolutePath())) {
                    fileMap.put(file.getName(), file.getAbsolutePath());
                }
            }
        }
        /*for (Map.Entry<String, String> entry : fileMap.entrySet()) {
            System.out.println("file name: " + entry.getKey() + ", path: " + entry.getValue());
        }*/
        return fileMap;
    }

    public static boolean isExcel(String filePath) {
        String extName = FileUtil.extName(filePath);
        return StrUtil.equalsIgnoreCase(extName, "xls") || StrUtil.equalsIgnoreCase(extName, "xlsx");
    }

    @Data
    @Builder
    static class Assets{
        private String ip; //IP
        private String sysname; //资产名称
        private String realm; //安全域
    }

    @Data
    @Builder
    static class SysMapping{
        private String frontLine; //一线系统
        private String antiVirus = ""; //防病毒系统
        private String auditHost = ""; //堡垒机系统
        private String[] antiVirusArr;
        private String[] auditHostArr;
    }

}
