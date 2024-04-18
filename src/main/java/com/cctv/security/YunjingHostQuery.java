package com.cctv.security;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;

/**
 * @author jiqq
 * @date 2024/4/11
 * @description
 */
public class YunjingHostQuery {
    //获取当前用户的执行路径
    public static final String ABSOLUTE_PATH = Paths.get("").toAbsolutePath()+File.separator;

    public static final String GENE_FILE_SUFFIX = "_"+DateUtil.today()+".xlsx";

    public static Dict LEVEL_DICT = Dict.create().set("0","提示").set("1", "低危").set("2", "中危").set("3", "高危").set("4","严重");

    public static String COOKIE, XCSRFCODE,DOMAIN = "http://oapi.tcep-bj.cloud.cctv.com";

    public static class URL{
        public static final String HOST_LIST = "/capi/v3?i=ocwp/DescribeBaselineHostDetectList";
        public static final String ITEM_RULE = "/capi/v3?i=ocwp/DescribeBaselineItemList";
    }

    public static void main(String[] args) throws Exception {
        COOKIE = "tce_language=zh-CN; req_session_id=90776c99-18df-4f5b-856c-d55fed1730e0; pageSeesionToken=71df2c42559f970c4d1251739b8be337; user_uin=909622298; appId=1; user_skey=fb175d1foYc/NY9AXNN/0MDQvUCdHsD6lbI1vGm2snoQL9ns7O4Jruy595SIjelimVaG1g; user_id=Security; owner_uin=909619400";
        XCSRFCODE  = "1097979825";
        main("host.xlsx",DOMAIN);
    }

    public static void main(String sourceFileName, String domain) {
        try {
            if(StrUtil.isBlank(domain)){
                Console.log("未指定domain");
                return;
            }
            DOMAIN = domain;
            if(StrUtil.isBlank(sourceFileName)){
                Console.log("未指定加载的文件");
                return;
            }

            TimeInterval timer = DateUtil.timer();
            writeExcel(FileNameUtil.mainName(sourceFileName) + GENE_FILE_SUFFIX, transform());
            Console.log("全部处理完成,耗时:{}秒",timer.intervalSecond());
        }catch (Exception ex){
            Console.error("失败-> 【{}】解析错误！",sourceFileName);
            ex.printStackTrace();
        }finally {
            ThreadUtil.sleep(Long.MAX_VALUE);
        }
    }

    private static List<Item> transform(){
        if(StrUtil.isBlank(COOKIE)||StrUtil.isBlank(XCSRFCODE)){
            Console.log("未指定cookis/xcsrfcode");
            return null;
        }

        String repsBody =  HttpRequest.post(DOMAIN + URL.HOST_LIST)
                .header("Cookie",COOKIE)
                .header("X-Csrfcode",XCSRFCODE)
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body("{\"region\":\"bjprivate\",\"serviceType\":\"ocwp\",\"action\":\"DescribeBaselineHostDetectList\""
                              + ",\"data\":{\"Version\":\"2021-08-30\",\"Language\":\"zh-CN\",\"Order\":\"DESC\",\"By\":\"ItemCount\""
                              + ",\"Offset\":0,\"Limit\":10000,\"Filters\":[{\"Name\":\"DetectStatus\",\"Values\":[0],\"ExactMatch\":false}]}}")
                .execute().body();
        Console.log("query api:{} \nwaiting...", URL.HOST_LIST);
        Object error = JSONUtil.getByPath(JSONUtil.parse(repsBody), "data.Response.Error");
        if(ObjectUtil.isNotNull(error)){
            Console.log("接口调用错误错误！msg:{}",repsBody);
            return null;
        }
        List<Item> itemList = new ArrayList<>();
        JSONArray jsonArray = (JSONArray)JSONUtil.getByPath(JSONUtil.parse(repsBody), "data.Response.List");
        for (Object obj : jsonArray) {
            JSONObject jsonObject = JSONUtil.parseObj(obj);
            String hostId = jsonObject.get("HostId")+"";
            String hostIp = jsonObject.get("HostIp")+"";
            String hostName = jsonObject.get("HostName")+"";
            itemList.add(Item.builder().hostId(hostId).hostIp(hostIp).hostName(hostName).build());
        }
        List<Item> itemList2 = new CopyOnWriteArrayList<>();
        List<List<Item>> partition = ListUtil.partition(itemList, 20);
        for (List<Item>  list: partition) {
            list.stream().parallel().forEach(item->{
                itemList2.addAll(Objects.requireNonNull(detail(item)));
            });
        }
        return itemList2;
    }

    private static List<Item> detail(Item host) {
        String repsBody =  HttpRequest.post(DOMAIN + URL.ITEM_RULE)
                .header("Cookie",COOKIE)
                .header("X-Csrfcode",XCSRFCODE)
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(StrUtil.format("{\"region\":\"bjprivate\",\"serviceType\":\"ocwp\",\"action\":\"DescribeBaselineItemList\""
                                             + ",\"data\":{\"Version\":\"2021-08-30\",\"Language\":\"zh-CN\",\"Order\":\"DESC\",\"By\":\"LastTime\",\"Offset\":0,\"Limit\":10000"
                                             + ",\"Filters\":[{\"Name\":\"DetectStatus\",\"Values\":[0],\"ExactMatch\":false},{\"Name\":\"HostId\",\"Values\":[\"{}\"]}]}}",host.hostId))
                .execute().body();
        Object error = JSONUtil.getByPath(JSONUtil.parse(repsBody), "data.Response.Error");
        if(ObjectUtil.isNotNull(error)){
            Console.log("接口调用错误错误！msg:{}",repsBody);
            return null;
        }
        Console.log("ip:{},query api:{}",host.hostIp, URL.ITEM_RULE);
        List<Item> itemList = new CopyOnWriteArrayList<>();

        JSONArray jsonArray = (JSONArray)JSONUtil.getByPath(JSONUtil.parse(repsBody), "data.Response.List");
        for (Object obj : jsonArray) {
            JSONObject json = JSONUtil.parseObj(obj);
            itemList.add(Item.builder().hostId(host.getHostId())
                                 .hostIp(host.getHostIp())
                                 .hostName(host.getHostName())
                                 .itemName(json.get("ItemName")+"")
                                 .itemDesc(json.get("ItemDesc")+"")
                                 .fixMethod(json.get("FixMethod")+"")
                                 .detectStatus("未通过")
                                 .level(LEVEL_DICT.get(json.get("Level")+"")+"")
                                 .build());
        }
        return itemList;
    }

    private static void writeExcel(String geneFileName,List<Item> itemList) {
        if(CollectionUtil.isEmpty(itemList)){
            Console.error("生成文件失败，数据为空");
            return;
        }
        File file = new File(ABSOLUTE_PATH+ geneFileName);
        FileUtil.del(file);
        ExcelWriter writer = ExcelUtil.getWriter(file)
                .addHeaderAlias("hostId", "主机ID")
                .addHeaderAlias("hostIp", "主机IP")
                .addHeaderAlias("hostName", "主机名字")
                .addHeaderAlias("detectStatus", "检测状态")
                .addHeaderAlias("itemName", "检测项名称")
                .addHeaderAlias("itemDesc", "检测项描述")
                .addHeaderAlias("level", "威胁等级")
                .addHeaderAlias("fixMethod", "修复建议");
        writer.setOnlyAlias(true);
        writer.getStyleSet().setAlign(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        // 一次性写出内容，使用默认样式，强制输出标题
        writer.write(itemList, true);
        // 关闭writer，释放内存
        writer.close();
        Console.log("成功->生成文件：{},行数：{}",geneFileName, itemList.size());
    }

    @Data
    @Builder
    static class Item {
        private String hostId; //主机ID
        private String hostIp; //主机IP
        private String hostName;//主机名称
        private String detectStatus;//检测状态
        private String itemName;//检测项名称
        private String itemDesc;//检测项描述
        private String fixMethod;//修复建议
        private String level;//威胁等级
    }


}
