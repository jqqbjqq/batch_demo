package com.cctv.security;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import java.io.File;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.compress.utils.Lists;

/**
 * @author jiqq
 */
public class AssetsCheck {

    public static void main(String[] args) {
        // resource path
        String firewallPath = "D:\\IdeaProjects\\batch_demo";
        String auditHostPath = "D:\\IdeaProjects\\batch_demo";

        List<Firewall> firewalls = firewallRead(firewallPath);
        List<AuditHost> auditHost = auditHostRead(auditHostPath);

    }

    private static List<Firewall> frontlineReadAndWirte(String filePath) {
        return null;
    }

    private static List<Firewall> firewallRead(String directoryPath) {
        Map<String, String> fileMap = handleExcelFiles(directoryPath);
        List<Firewall> allList = Lists.newArrayList();
        fileMap.forEach((name, path) -> {
            ExcelReader reader = ExcelUtil.getReader(path).addHeaderAlias("IP", "ip")
                    .addHeaderAlias("系统名称", "sysname");
            List<Firewall> list = reader.readAll(Firewall.class);
            Console.log("name:{} count:{}", name, list.size());
            allList.addAll(list);
        });
        return allList;
    }

    private static List<AuditHost> auditHostRead(String directoryPath) {
        Map<String, String> fileMap = handleExcelFiles(directoryPath);
        List<AuditHost> allList = Lists.newArrayList();
        fileMap.forEach((name, path) -> {
            ExcelReader reader = ExcelUtil.getReader(path).addHeaderAlias("IP", "ip")
                    .addHeaderAlias("系统名称", "sysname");
            List<AuditHost> list = reader.readAll(AuditHost.class);
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
    static class Firewall {

        private String ip; //IP
        private String sysname; //系统名称
    }

    @Data
    @Builder
    static class AuditHost {

        private String ip; //IP
        private String sysname; //系统名称
    }

}
