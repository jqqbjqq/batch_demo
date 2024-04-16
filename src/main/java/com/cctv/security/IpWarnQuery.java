package com.cctv.security;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;

/**
 * @author jiqq
 * @date 2024/4/11
 * @description
 */
public class IpWarnQuery {
    //获取当前用户的执行路径
    public static final String ABSOLUTE_PATH = Paths.get("").toAbsolutePath()+File.separator;

    public static final String GENE_FILE_SUFFIX = "_new.xlsx";

    public static String DOMAIN = "";

    public static class URL{
        public static final String LOGIN = "/api/sapi/Login?Action=Login";
        public static final String QUERY_WARN = "/api/sapi/DescribeSingeTiInfo?Action=DescribeSingeTiInfo";
    }

    public static void main(String[] args) {
        System.out.println(JSONUtil.createObj().set("QueryKey", "xx").set("Action", "DescribeSingeTilnfo").toString());
    }

    public static void main(String sourceFileName, String domain,String auth) {
        try {
            if(StrUtil.isBlank(domain)){
                Console.log("未指定domain");
                return;
            }
            DOMAIN = domain;
            if(StrUtil.isBlank(auth)){
                Console.log("未指定授权信息");
                return;
            }
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
            writeExcel(FileNameUtil.mainName(sourceFileName) + GENE_FILE_SUFFIX, transform(login(auth),readExcel(sourceFileName)));
            Console.log("全部处理完成,耗时:{}秒",timer.intervalSecond());
        }catch (Exception ex){
            Console.error("失败-> 【{}】解析错误！",sourceFileName);
            ex.printStackTrace();
        }finally {
            ThreadUtil.sleep(Long.MAX_VALUE);
        }
    }


    private static List<IpWarn> transform(String token,List<String> ipList){
        if(StrUtil.isBlank(token)|| CollectionUtil.isEmpty(ipList)){
            return null;
        }
        Console.log("Authorization:{}",token);
        List<IpWarn> ipWarnList = new CopyOnWriteArrayList<>();
        List<List<String>> partition = ListUtil.partition(ipList, 20);
        Console.log("query api:{} \nwaiting...", URL.QUERY_WARN);
        for (List<String>  list: partition) {
            list.stream().parallel().forEach(ip->{
                String repsBody =  HttpRequest.post(DOMAIN +URL.QUERY_WARN)
                        .header(Header.AUTHORIZATION, token)
                        .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                        .body(JSONUtil.createObj().set("QueryKey", ip).set("Action", "DescribeSingeTiInfo").toString())
                        .execute().body();
                JSON json = JSONUtil.parse(repsBody);
                String threatType = JSONUtil.getByPath(json, "Response.Data[0].ThreatType")+"";
                String tags = JSONUtil.getByPath(json, "Response.Data[0].Tags")+"";
                //Console.log("ip:{},threatType:{},tags:{}",ip,threatType,tags);
                ipWarnList.add(IpWarn.builder().queryIp(ip).threatType(threatType).tags(tags).build());
            });
        }
        return ipWarnList;
    }

    private static List<String> readExcel(String sourceFileName) {
        ExcelReader reader = ExcelUtil.getReader(ABSOLUTE_PATH+sourceFileName)
                .addHeaderAlias("外网IP", "queryIp");
        List<IpWarn> ipWarnList = reader.readAll(IpWarn.class);
        Console.log("总行数：{}",ipWarnList.size());
        return ipWarnList.stream().map(IpWarn::getQueryIp).collect(Collectors.toList());
    }

    private static void writeExcel(String geneFileName,List<IpWarn> ipWarnList) {
        File file = new File(ABSOLUTE_PATH+ geneFileName);
        FileUtil.del(file);
        ExcelWriter writer = ExcelUtil.getWriter(file)
                .addHeaderAlias("queryIp", "外网IP")
                .addHeaderAlias("threatType", "威胁类型")
                .addHeaderAlias("tags", "情报标签");
        writer.setOnlyAlias(true);
        writer.getStyleSet().setAlign(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        // 一次性写出内容，使用默认样式，强制输出标题
        writer.write(ipWarnList, true);
        // 关闭writer，释放内存
        writer.close();
        Console.log("成功->生成文件：{},行数：{}",geneFileName, ipWarnList.size());
    }

    private static String login(String auth) {
        String userName = auth.split(":")[0];
        String password = auth.split(":")[1];
        String repsBody =  HttpRequest.post(DOMAIN + URL.LOGIN)
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.createObj().set("Action", "Login").set("UserName", userName).set("Password", password).toString())
                .execute().body();
        Console.log("login api:{}\nbody:{}",URL.LOGIN,repsBody);
        JSON json = JSONUtil.parse(repsBody);
        Object error = JSONUtil.getByPath(json, "Response.Error");
        if(ObjectUtil.isNotNull(error)){
            Console.log("账号密码错误！");
            return null;
        }
        return JSONUtil.getByPath(json, "Response.Data.Token")+"";
    }


    @Data
    @Builder
    static class IpWarn {
        private String queryIp; //查询ip
        private String threatType; //威胁类型
        private String tags;//情报标签
    }


}
