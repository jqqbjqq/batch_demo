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
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import lombok.Builder;
import lombok.Data;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;

/**
 * @author jiqq
 * @date 2024/4/11
 * @description
 */
public class BaselineRuleList {
    //获取当前用户的执行路径
    public static final String ABSOLUTE_PATH = Paths.get("").toAbsolutePath()+File.separator;

    public static final String GENE_FILE_SUFFIX = "_"+DateUtil.today()+".xlsx";

    public static String COOKIE, XCSRFCODE,REGION,DOMAIN = "http://oapi.tcep-bj.cloud.cctv.com";

    public static Boolean IS_INPUT = true;

    public static class URL{
        public static final String HOST_LIST = "/capi/v3?i=ocwp/DescribeBaselineHostDetectList";
        public static final String ITEM_RULE = "/capi/v3?i=ocwp/DescribeBaselineItemList";
    }

    public static void main(String[] args) throws Exception {
        COOKIE = "tce_language=zh-CN; req_session_id=8b1e9aff-9074-48bc-b313-2c311a285923; user_uin=909622312; appId=1; user_id=Security; owner_uin=909619400; pageSeesionToken=dc61f68812bca26b86a7c8f2b0b022e7; user_skey=4a838fe7kJVdYk3E2YDUvYtO+uvt8qgN8ZSNTtk6Qu+9wNtdzYbAfX3dpUits9Or6LbxFQ";
        XCSRFCODE  = "366452760";
        REGION = "bjdedicate";
        DOMAIN = "http://oapi.tced-bj.cloud.cctv.com";
        IS_INPUT = false;
        main("基线检测设置.xlsx",DOMAIN,REGION);
    }

    public static void main(String sourceFileName, String domain ,String region) {
        try {
            if(StrUtil.isBlank(domain)){
                Console.log("未指定domain");
                return;
            }
            DOMAIN = domain;
            if(StrUtil.isBlank(region)){
                Console.log("未指定region");
                return;
            }
            REGION = region;
            if(StrUtil.isBlank(sourceFileName)){
                Console.log("未指定加载的文件");
                return;
            }
            if(IS_INPUT) {
                Console.log("请使用浏览器成功登录云镜系统后，按F12进入调试模式，点击Network选项找到任意链接，查看右侧Header项的cookie和X-Csrfcode");
                Scanner scanner = new Scanner(System.in);
                Console.log("请输入X-Csrfcode");
                XCSRFCODE = scanner.nextLine();
                Console.log("请输入Cookie");
                COOKIE = scanner.nextLine();
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
                .body("{\"region\":\""+REGION+"\",\"serviceType\":\"ocwp\",\"action\":\"DescribeBaselineRuleList\""
                              + ",\"data\":{\"Version\":\"2021-08-30\",\"Language\":\"zh-CN\",\"Offset\":0,\"Limit\":10000}}")
                .execute().body();
        Console.log("query api:{}\nbody:{}\nwaiting...", URL.HOST_LIST, repsBody);
        Object error = JSONUtil.getByPath(JSONUtil.parse(repsBody), "data.Response.Error");
        if(ObjectUtil.isNotNull(error)){
            return null;
        }
        List<String> ruleIdList = new ArrayList<>();
        JSONArray jsonArray = (JSONArray)JSONUtil.getByPath(JSONUtil.parse(repsBody), "data.Response.List");
        for (Object obj : jsonArray) {
            JSONObject jsonObject = JSONUtil.parseObj(obj);
            ruleIdList.add(jsonObject.get("RuleId")+"");
        }
        List<Item> itemList2 = Collections.synchronizedList(new ArrayList<>());
        List<List<String>> partition = ListUtil.partition(ruleIdList, 20);
        for (List<String>  ruleId: partition) {
            ruleId.stream().parallel().forEach(item->{
                itemList2.addAll(Optional.ofNullable(detail(item)).orElse(new ArrayList<>()));
            });
        }
        return itemList2;
    }

    private static List<Item> detail(String ruleId) {
        String repsBody =  HttpRequest.post(DOMAIN + URL.ITEM_RULE)
                .header("Cookie",COOKIE)
                .header("X-Csrfcode",XCSRFCODE)
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .timeout(5000)
                .body(StrUtil.format("{\"region\":\""+REGION+"\",\"serviceType\":\"ocwp\",\"action\":\"DescribeBaselineItemInfo\""
                                             + ",\"data\":{\"Version\":\"2021-08-30\",\"Language\":\"zh-CN\",\"Offset\":0,\"Limit\":10000"
                                             + ",\"Filters\":[{\"Name\":\"RuleId\",\"Values\":[\"{}\"],\"ExactMatch\":false}]}}",ruleId))
                .execute().body();
        Object error = JSONUtil.getByPath(JSONUtil.parse(repsBody), "data.Response.Error");
        if(ObjectUtil.isNotNull(error)){
            Console.log("{ruleId}:接口调用错误错误！msg:{}",ruleId,repsBody);
            return null;
        }
        Console.log("ruleId:{},query api:{}",ruleId, URL.ITEM_RULE);
        List<Item> itemList = new ArrayList<>();
        JSONArray jsonArray = (JSONArray)JSONUtil.getByPath(JSONUtil.parse(repsBody), "data.Response.List");
        if(ObjectUtil.isNull(jsonArray)){
            Console.log("ruleId:{} data.Response.List is null",ruleId);
            return itemList;
        }
        for (Object obj : jsonArray) {
            JSONObject json = JSONUtil.parseObj(obj);
            itemList.add(Item.builder()
                             .ruleId(json.get("RuleId")+"")
                             .ruleName(json.get("RuleName")+"")
                             .itemName(json.get("ItemName")+"")
                             .itemDesc(json.get("ItemDesc")+"")
                             .fixMethod(json.get("FixMethod")+"")
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
                .addHeaderAlias("ruleName", "检测规则名称")
                .addHeaderAlias("itemName", "检测项名称")
                .addHeaderAlias("itemDesc", "检测项说明")
                .addHeaderAlias("fixMethod", "修复建议");
        writer.setOnlyAlias(true);
        writer.getStyleSet().setBorder(BorderStyle.NONE, IndexedColors.AUTOMATIC)
                .setAlign(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        // 一次性写出内容，使用默认样式，强制输出标题
        writer.write(itemList, true);
        // 关闭writer，释放内存
        writer.close();
        Console.log("成功->生成文件：{},行数：{}",geneFileName, itemList.size());
    }

    @Data
    @Builder
    static class Item {
        private String ruleId;//检测规则ID
        private String ruleName;//检测规则名称
        private String itemName; //检测项名称
        private String itemDesc;//检测项说明
        private String fixMethod;//修复建议
    }

}
