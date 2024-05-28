package com.cctv.security;

import cn.hutool.core.collection.CollectionUtil;
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
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
public class HostAssetQuery {
    //获取当前用户的执行路径
    public static final String ABSOLUTE_PATH = Paths.get("").toAbsolutePath()+File.separator;

    public static final String GENE_FILE_SUFFIX = "_"+DateUtil.today()+".xlsx";

    public static String COOKIE, XCSRFCODE,REGION,DOMAIN = "http://oapi.tcep-bj.cloud.cctv.com";

    public static class URL{
        public static final String HOST_LIST = "/capi/v3?i=ocwp/";
    }

    public static Boolean IS_INPUT = true;

    public static void main(String[] args) throws Exception {
        COOKIE = "tce_language=zh-CN; req_session_id=8b1e9aff-9074-48bc-b313-2c311a285923; user_uin=909622312; appId=1; user_id=Security; owner_uin=909619400; pageSeesionToken=dc61f68812bca26b86a7c8f2b0b022e7; user_skey=4a838fe7kJVdYk3E2YDUvYtO+uvt8qgN8ZSNTtk6Qu+9wNtdzYbAfX3dpUits9Or6LbxFQ";
        XCSRFCODE  = "366452760";
        REGION = "bjdedicate";
        DOMAIN = "http://oapi.tced-bj.cloud.cctv.com";
        IS_INPUT = false;
        main("资产指纹.xlsx",DOMAIN,REGION);
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
            List<AssetType> typeList = new ArrayList<>();

            typeList.add(AssetType.builder().api("DescribeAssetPortCount").name("端口").type("Ports").build());
            typeList.add(AssetType.builder().api("DescribeAssetAppCount").name("软件应用").type("Apps").build());
            typeList.add(AssetType.builder().api("DescribeAssetDatabaseCount").name("数据库").type("Databases").build());
            //typeList.add(AssetType.builder().api("DescribeAssetUserCount").name("账号").type("Users").build());
            //typeList.add(AssetType.builder().api("DescribeAssetProcessCount").name("进程").type("Process").build());
            typeList.add(AssetType.builder().api("DescribeAssetWebAppCount").name("web应用").type("WebApps").build());
            typeList.add(AssetType.builder().api("DescribeAssetWebServiceCount").name("web服务").type("WebServices").build());
            typeList.add(AssetType.builder().api("DescribeAssetWebFrameCount").name("web框架").type("WebFrames").build());
            typeList.add(AssetType.builder().api("DescribeAssetWebLocationCount").name("web站点").type("WebLocations").build());

            File file = new File(ABSOLUTE_PATH+ FileNameUtil.mainName(sourceFileName) + GENE_FILE_SUFFIX);
            FileUtil.del(file);

            typeList.forEach(type->{
                writeExcel(file,type.name, transform(type));
            });
            Console.log("全部处理完成,耗时:{}秒",timer.intervalSecond());
        }catch (Exception ex){
            Console.error("失败-> 【{}】解析错误！",sourceFileName);
            ex.printStackTrace();
        }finally {
            ThreadUtil.sleep(Long.MAX_VALUE);
        }
    }

    private static List<Item> transform(AssetType type){
        if(StrUtil.isBlank(COOKIE)||StrUtil.isBlank(XCSRFCODE)){
            Console.log("未指定cookis/xcsrfcode");
            return null;
        }
        String repsBody =  HttpRequest.post(DOMAIN + URL.HOST_LIST+type.getApi())
                .header("Cookie",COOKIE)
                .header("X-Csrfcode",XCSRFCODE)
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body("{\"region\":\""+REGION+"\",\"serviceType\":\"ocwp\",\"action\":\""+type.getApi()+"\""
                              + ",\"data\":{\"Version\":\"2021-08-30\",\"Language\":\"zh-CN\"}}")
                .execute().body();
        Console.log("query api:{}\nbody:{}\nwaiting...", URL.HOST_LIST, repsBody);
        Object error = JSONUtil.getByPath(JSONUtil.parse(repsBody), "data.Response.Error");
        List<Item> itemList = new ArrayList<>();
        if(ObjectUtil.isNotNull(error)){
            return itemList;
        }
        JSONArray jsonArray = (JSONArray)JSONUtil.getByPath(JSONUtil.parse(repsBody), "data.Response."+type.getType());
        for (Object obj : jsonArray) {
            JSONObject jsonObject = JSONUtil.parseObj(obj);
            String key = jsonObject.get("Key")+"";
            String value = jsonObject.get("Value")+"";
            itemList.add(Item.builder().key(key).value(value).build());
        }
        return itemList;
    }

    private static void writeExcel(File file,String sheetName,List<Item> itemList) {
        if(CollectionUtil.isEmpty(itemList)){
            Console.error("生成文件失败，数据为空");
            return;
        }
        //File file = new File(ABSOLUTE_PATH+ geneFileName);
        //FileUtil.del(file);
        ExcelWriter writer = ExcelUtil.getWriter(file)
                .addHeaderAlias("key", "资产名")
                .addHeaderAlias("value", "资产数量");

        writer.setOnlyAlias(true).setSheet(sheetName);
        writer.getStyleSet().setBorder(BorderStyle.NONE, IndexedColors.AUTOMATIC)
                .setAlign(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        // 一次性写出内容，使用默认样式，强制输出标题
        writer.write(itemList, true);
        // 关闭writer，释放内存
        writer.close();
        Console.log("成功->生成文件：{},行数：{}",file.getName(), itemList.size());
    }

    @Data
    @Builder
    static class Item {
        private String key; //key
        private String value; //value
    }

    @Data
    @Builder
    static class AssetType {
        private String api;
        private String name;
        private String type;
    }

}
