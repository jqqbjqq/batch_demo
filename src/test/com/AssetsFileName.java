package com;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.compress.utils.Lists;

/**
 * @author jiqq
 */
public class AssetsFileName {

    public static void main(String[] args) {
        TimeInterval timer = DateUtil.timer();

        // Console.log("---------AntiVirus----------");
        // String antiVirusPath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\防病毒";
        // List<Base> avList = antiVirusRead(antiVirusPath);
        // printSysname(avList);
        //
        // Console.log("---------AuditHost----------");
        // String auditHostPath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\堡垒机";
        // List<Base> ahList = auditHostRead(auditHostPath);
        // printSysname(ahList);

        Console.log("---------FrontLine----------");
        String frontLinePath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\一线资产";
        Map<String, String> fileMap = handleExcelFiles(frontLinePath);

        //many task
        fileMap.forEach((fileName, filePath) -> {
            //Console.log("load filePath:{} ",filePath);
            List<String> sheetNameList = ExcelUtil.getReader(filePath).getSheetNames();
            filterSheetName(sheetNameList);
            if(sheetNameList.size()==1&&sheetNameList.get(0).startsWith("Sheet")){
                List<Base> flList = Lists.newArrayList();
                sheetNameList.forEach(sheetName -> {
                    ExcelReader reader = ExcelUtil.getReader(new File(filePath), sheetName)
                            .addHeaderAlias("内网IP地址/云内地址", "ip")
                            .addHeaderAlias("信息系统名称", "sysname");
                    List<Base> list = restoreData(reader.readAll(Base.class));
                    //Console.log("  -> sheetname:{} count:{}", sheetName, flList.size());
                    flList.addAll(list);
                });
                Map<String, List<Base>> groupList = flList.stream().collect(Collectors.groupingBy(
                        Base::getSysname, Collectors.toList()));
                groupList.forEach((sysname,list)-> {
                    //dataHandler(flList, avList, ahList);
                    printSysname(list);
                });
                return;
            }
            if (StrUtil.contains(filePath, "近期确认的资产")) {
                List<Base> flList = Lists.newArrayList();
                sheetNameList.forEach(sheetName -> {
                    ExcelReader reader = ExcelUtil.getReader(new File(filePath), sheetName)
                            .addHeaderAlias("资产IP", "ip")
                            .addHeaderAlias("资产名称", "sysname");
                    List<Base> list = restoreData(reader.readAll(Base.class));
                    //Console.log("  -> sheetname:{} count:{}", sheetName, flList.size());
                    flList.addAll(list);
                });
                flList.forEach(e -> {
                    e.setSysname(FileNameUtil.getPrefix(fileName));
                });
                printSysname(flList);
                //dataHandler(flList, avList, ahList);
            } else {
                sheetNameList.forEach(sheetName -> {
                    ExcelReader reader = ExcelUtil.getReader(new File(filePath), sheetName)
                            .addHeaderAlias("资产IP", "ip")
                            .addHeaderAlias("资产名称", "sysname")
                            .addHeaderAlias("安全域", "realm");
                    List<Base> flList = restoreData(reader.readAll(Base.class));
                    //Console.log("  -> sheetname:{} count:{}", sheetName, flList.size());
                    cleanupName(flList);
                    printSysname(flList);
                });
            }
        });
        //Console.log("The total time is {}s", timer.intervalSecond());
    }


    private static void printSysname(List<Base> list){
        //Console.log("------------{}------------",list.get(0).getClass().getName());
        Set<String> set = list.stream().map(Base::getSysname).filter(StrUtil::isNotBlank).collect(Collectors.toCollection(TreeSet::new));
        set.forEach(System.out::println);
    }

    private static List<Base> restoreData(List<Base> flList) {
        if(CollUtil.isEmpty(flList)){
            return Lists.newArrayList();
        }
        return flList.stream().filter(e -> StrUtil.isNotBlank(e.getIp())) //&&StrUtil.isNotBlank(e.getSysname())
                .peek(e -> {
                    e.setIp(e.getIp().trim());
                    if (StrUtil.isNotBlank(e.getSysname())) {
                        e.setSysname(e.getSysname().trim());
                    }
                }).collect(Collectors.toList());
    }

    private static void cleanupName(List<Base> flList) {
        if(CollUtil.isEmpty(flList)){
            return;
        }
        flList.forEach(e -> {
            //e.setSysname(getSysname(flList.get(0).getRealm()));
            e.setSysname(flList.get(0).getRealm());
        });
    }

    private static String getSysname(String str) {
        if(StrUtil.isBlank(str)){
            return "";
        }
        if(str.startsWith("/")){
            return str.split("/")[3];
        }
        return str.split("-")[0];
    }

    private static void filterSheetName(List<String> list) {
        CollUtil.removeAny(list, "help", "格式");
        list.removeIf(str -> str.startsWith("Sht"));
    }

    private static List<Base> antiVirusRead(String directoryPath) {
        Map<String, String> fileMap = handleExcelFiles(directoryPath);
        List<Base> allList = Lists.newArrayList();
        fileMap.forEach((name, path) -> {
            ExcelReader reader = ExcelUtil.getReader(path)
                    .addHeaderAlias("名称", "ip")
                    .addHeaderAlias("信息系统", "sysname");
            List<Base> list = restoreData(reader.readAll(Base.class));
            Console.log("name:{} count:{}", name, list.size());
            allList.addAll(list);
        });
        return allList;
    }

    private static List<Base> auditHostRead(String directoryPath) {
        Map<String, String> fileMap = handleExcelFiles(directoryPath);
        List<Base> allList = Lists.newArrayList();
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
            List<Base> list = restoreData(reader.readAll(Base.class));
            //Console.log("name:{} count:{}", name, list.size());
            allList.addAll(list);
        });
        return allList;
    }

    public static Map<String, String> handleExcelFiles(String directoryPath) {
        Map<String, String> fileMap = MapUtil.newHashMap();
        List<File> files = FileUtil.loopFiles(directoryPath);
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
    static class Base{
        String ip; //IP
        String sysname; //资产名称
        private String realm; //安全域
    }

}
