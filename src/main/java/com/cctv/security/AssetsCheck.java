package com.cctv.security;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        /*
        String antiVirusPath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\防病毒";
        List<Base> avList = antiVirusRead(antiVirusPath);
        printSysname(avList);

        String auditHostPath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\堡垒机";
        List<Base> ahList = auditHostRead(auditHostPath);
        printSysname(ahList);
        */
        String frontLinePath = "E:\\cctv-assets\\一线资产与堡垒机防病毒资产\\data\\一线资产";
        Map<String, String> fileMap = handleExcelFiles(frontLinePath);

        //many task
        fileMap.entrySet().forEach(file -> { //parallelStream().
            String fileName = file.getKey(),filePath = file.getValue();
            List<String> sheetNameList = ExcelUtil.getReader(filePath).getSheetNames();
            CollUtil.removeAny(sheetNameList, "help", "格式");
            if(StrUtil.contains(filePath,"近期确认的资产")){
                List<Base> flList = Lists.newArrayList();
                sheetNameList.forEach(sheetName->{
                    ExcelReader reader = ExcelUtil.getReader(new File(filePath),sheetName)
                            .addHeaderAlias("资产IP", "ip")
                            .addHeaderAlias("资产名称", "sysname");
                    List<Base> list = reader.readAll(Base.class);
                    Console.log("filename:{} sheetname:{} count:{}", fileName, sheetName, flList.size());
                    flList.addAll(list);
                });
                flList.forEach(e->{
                    e.setSysname(fileName);
                });
                //printSysname(flList);
                //dataHandler(flList, avList, ahList);
            }else{
                sheetNameList.forEach(sheetName->{
                    ExcelReader reader = ExcelUtil.getReader(new File(filePath),sheetName)
                            .addHeaderAlias("资产IP", "ip")
                            .addHeaderAlias("资产名称", "sysname")
                            .addHeaderAlias("安全域", "realm");
                    List<Base> flList = reader.readAll(Base.class);
                    Console.log("filename:{} sheetname:{} count:{}", fileName, sheetName, flList.size());
                    flList.forEach(e->{
                        e.setSysname(e.getRealm().split("-")[0]);
                    });
                    //printSysname(flList);
                    //dataHandler(flList, avList, ahList);
                });
            }
        });
        Console.log("The total time is {}s", timer.intervalSecond());
    }

    private static void printSysname(List<Base> list){
        Console.log("------------{}------------",list.get(0).getClass().getName());
        Set<String> set = list.stream().map(Base::getSysname).filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        set.forEach(System.out::println);
    }

    private static void dataHandler(List<Base> flList, List<Base> avList, List<Base> ahList) {
        Console.log("flList:{},avList:{},ahList:{}", flList.size(),avList.size(),ahList.size());
    }

    private static List<Base> antiVirusRead(String directoryPath) {
        Map<String, String> fileMap = handleExcelFiles(directoryPath);
        List<Base> allList = Lists.newArrayList();
        fileMap.forEach((name, path) -> {
            ExcelReader reader = ExcelUtil.getReader(path)
                    .addHeaderAlias("名称", "ip")
                    .addHeaderAlias("信息系统", "sysname");
            List<Base> list = reader.readAll(Base.class);
            Console.log("name:{} count:{}", name, list.size());
            allList.addAll(list);
        });
        return allList;
    }

    private static List<Base> auditHostRead(String directoryPath) {
        Map<String, String> fileMap = handleExcelFiles(directoryPath);
        List<Base> allList = Lists.newArrayList();
        fileMap.forEach((name, path) -> {
            ExcelReader reader = ExcelUtil.getReader(path)
                    .addHeaderAlias("#主机IP", "ip")
                    .addHeaderAlias("主机组名称", "sysname");
            List<Base> list = reader.readAll(Base.class);
            Console.log("name:{} count:{}", name, list.size());
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
