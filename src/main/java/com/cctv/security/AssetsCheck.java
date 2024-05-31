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
import cn.hutool.poi.excel.ExcelWriter;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.compress.utils.Lists;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;

/**
 * @author jiqq
 */
public class AssetsCheck {

    static Map<String, SysMapping>  sysMappingMap ;
    static Map<String,List<Assets>> avMap;
    static Map<String,List<Assets>> ahMap;
    static Map<String, String> fileMap;
    static List<Assets> allFlList = Lists.newArrayList();

    static {
        String antiVirusPath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\防病毒";
        avMap = toMapBySysname(antiVirusRead(antiVirusPath));

        String auditHostPath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\堡垒机";
        ahMap = toMapBySysname(auditHostRead(auditHostPath));

        String frontLinePath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\一线资产";
        fileMap = handleExcelFiles(frontLinePath);

        String sysMappingPath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\系统名整理.xlsx";
        sysMappingMap = sysMappingRead(sysMappingPath);
    }

    public static void main(String[] args) {
        TimeInterval timer = DateUtil.timer();
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
                    dataHandler(flList);
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
                dataHandler(flList);
            } else {
                sheetNameList.forEach(sheetName -> {
                    ExcelReader reader = ExcelUtil.getReader(new File(filePath), sheetName)
                            .addHeaderAlias("资产IP", "ip")
                            .addHeaderAlias("资产名称", "sysname")
                            .addHeaderAlias("安全域", "realm");
                    List<Assets> flList = restoreData(reader.readAll(Assets.class));
                    Console.log("  -> sheetname:{} count:{}", sheetName, flList.size());
                    dataHandler(flList);
                });
            }
        });
        noMatchHandler();
        Console.log("The total time is {}s", timer.intervalSecond());
    }


    private static void noMatchHandler() {
        String avFilePath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\gene\\未匹配【防病毒】.xlsx";
        List<Assets> antiVirusList = avMap.values().stream().flatMap(List::stream)
                .collect(Collectors.toList());
        writeFile(avFilePath,antiVirusList);

        String ahFilePath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\gene\\未匹配【堡垒机】.xlsx";
        List<Assets> auditHostList = ahMap.values().stream().flatMap(List::stream)
                .collect(Collectors.toList());
        writeFile(ahFilePath,auditHostList);
        // ip rematching
        reMatchSysname(antiVirusList,auditHostList);
    }

    private static void reMatchSysname(List<Assets> avList,List<Assets> ahList) {
        Console.log("allFlList count:{}", allFlList.size());

        String avFilePath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\gene\\IP匹配【防病毒】.xlsx";
        writeFile2(avFilePath,toAssets2(avList));

        String ahFilePath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\gene\\IP匹配【堡垒机】.xlsx";
        writeFile2(ahFilePath,toAssets2(ahList));
    }

    private static List<Assets2> toAssets2(List<Assets> assetsList) {
        List<Assets2> assets2List = Lists.newArrayList();
        for (Assets assets : assetsList) {
            for (Assets fl : allFlList) {
                if (assets.getIp().equals(fl.getIp())) {
                    assets2List.add(Assets2.builder().sysname1(fl.getSysname()).sysname2(assets.getSysname()).ip(fl.getIp()).build());
                }
            }
        }
        return assets2List;
    }

    private static void writeFile2(String filePath,List<Assets2> list) {
        File file = new File(filePath);
        FileUtil.del(file);
        ExcelWriter writer = ExcelUtil.getWriter(file)
                .addHeaderAlias("sysname1", "一线系统")
                .addHeaderAlias("sysname2", "系统")
                .addHeaderAlias("ip", "IP");
        writer.setOnlyAlias(true);
        writer.setColumnWidth(0,30).setColumnWidth(1,20);
        writer.getStyleSet().setBorder(BorderStyle.NONE, IndexedColors.AUTOMATIC)
                .setAlign(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        writer.write(list, true);
        writer.close();
    }

    private static void dataHandler(List<Assets> flList) {
        Console.log("flList:{},avList:{},ahList:{}", flList.size(),avMap.size(),ahMap.size());
        String sysname = flList.get(0).getSysname();
        SysMapping mapping = sysMappingMap.get(sysname);
        if(Objects.isNull(mapping)){
            Console.log("The sysname mapping is null", sysname);
            return;
        }
        String[] antiVirusArr = mapping.getAntiVirusArr();
        String[] auditHostArr = mapping.getAuditHostArr();

        flList.add(0,Assets.builder().sysname("【一线系统】").build());
        flList.add(Assets.builder().sysname("【防病毒系统】").build());
        for (String antiVirus : antiVirusArr) {
            flList.addAll(Optional.ofNullable(avMap.get(antiVirus)).orElse(Lists.newArrayList()));
            avMap.remove(antiVirus);
        }
        flList.add(Assets.builder().sysname("【堡垒机系统】").build());
        for (String auditHost : auditHostArr) {
            flList.addAll(Optional.ofNullable(ahMap.get(auditHost)).orElse(Lists.newArrayList()));
            ahMap.remove(auditHost);
        }
        String filePath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\gene\\"+sysname+".xlsx";
        writeFile(filePath,flList);
        allFlList.addAll(flList);
    }

    private static void writeFile(String filePath,List<Assets> list) {
        File file = new File(filePath);
        FileUtil.del(file);
        ExcelWriter writer = ExcelUtil.getWriter(file)
                .addHeaderAlias("sysname", "系统名")
                .addHeaderAlias("ip", "IP");
        writer.setOnlyAlias(true);
        writer.setColumnWidth(0,30).setColumnWidth(1,20);
        writer.getStyleSet().setBorder(BorderStyle.NONE, IndexedColors.AUTOMATIC)
                .setAlign(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        writer.write(list, true);
        writer.close();
    }

    private static List<Assets> restoreData(List<Assets> list) {
        if(CollUtil.isEmpty(list)){
            return Lists.newArrayList();
        }
        return list.stream().filter(e -> StrUtil.isNotBlank(e.getIp())) //&&StrUtil.isNotBlank(e.getSysname())
                .peek(e -> {
                    e.setIp(extractIP(e.getIp().trim()));
                    if (StrUtil.isNotBlank(e.getRealm())) {
                        e.setSysname(extractName(e.getRealm()));
                    }
                    if (StrUtil.isBlank(e.getSysname())) {
                        e.setSysname(list.get(0).getSysname());
                    }
                    e.setSysname(e.getSysname().trim());
                }).collect(Collectors.toList());
    }

    public static String extractIP(String str) {
        Pattern pattern = Pattern.compile("\\b(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\b");
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private static String extractName(String str) {
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
                    .addHeaderAlias("防病毒", "antiVirus")
                    .addHeaderAlias("堡垒机", "auditHost");
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
    static class Assets2{
        private String ip;
        private String sysname1;
        private String sysname2;
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
