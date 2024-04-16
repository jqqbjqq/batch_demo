package com.cctv.security;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Console;
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
import java.util.Comparator;
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
public class AlarmRuleQuery {
    //获取当前用户的执行路径
    public static final String ABSOLUTE_PATH = Paths.get("").toAbsolutePath()+File.separator;

    public static final String GENE_FILE_SUFFIX = "_"+DateUtil.today()+".xlsx";

    public static String DOMAIN = "https://10.232.1.8";

    public static class URL{
        public static final String LOGIN = "/api/AppUserLogin/user/custom_login";
        public static final String QUERY_RULE = "/api/AppAlarmStrategyManager/rule/getlist";
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
            writeExcel(FileNameUtil.mainName(sourceFileName) + GENE_FILE_SUFFIX, transform(login(auth)));
            Console.log("全部处理完成");
        }catch (Exception ex){
            Console.error("失败-> 【{}】解析错误！",sourceFileName);
            ex.printStackTrace();
        }finally {
            ThreadUtil.sleep(Long.MAX_VALUE);
        }
    }

    private static List<Rule> transform(String[] auth){
        if(ObjectUtil.isNull(auth)){
            return null;
        }
        List<Rule> ruleList = new CopyOnWriteArrayList<>();
        String repsBody =  HttpRequest.post(DOMAIN + URL.QUERY_RULE)
                .header(Header.AUTHORIZATION, auth[0])
                .header("SESSION_ID","SESSION_ID="+auth[1])
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(" {\"search\":\"\",\"pageSize\":10000,\"page\":1,\"dir\":\"desc\",\"sort\":\"Fid\""
                              + ",\"filterObj\":{\"Fseverity\":[],\"Fcategory\":[],\"Fsubcategory\":[],\"Fkillchain\":[],\"Fresult\":[],\"Frule_action\":[]}"
                              + ",\"dirObj\":{},\"mustObj\":{}}")
                .execute().body();
        Console.log("login api:{} waiting... \n", URL.QUERY_RULE);
        JSONArray jsonArray = (JSONArray)JSONUtil.getByPath(JSONUtil.parse(repsBody), "data.list");
        for (Object obj : jsonArray) {
            JSONObject jsonObject = JSONUtil.parseObj(obj);
            String fid = jsonObject.get("Fid")+"";
            String fname = jsonObject.get("Fname")+"";
            String fcategory = jsonObject.get("Fcategory")+"";
            String fkillchain = jsonObject.get("Fkillchain")+"";
            String fsubcategory = jsonObject.get("Fsubcategory")+"";
            String fpattern = jsonObject.get("Fpattern")+"";
            String conditions = JSONUtil.getByPath(JSONUtil.parse(fpattern), "conditions[0].match")+"";
            ruleList.add(Rule.builder().fid(fid).fname(fname).fcategory(fcategory)
                                 .fkillchain(fkillchain).conditions(conditions)
                                 .fsubcategory(fsubcategory).build());
        }
        return ruleList.stream().peek(e->e.setSortId(Integer.valueOf(e.getFid())))
                .sorted(Comparator.comparing(Rule::getSortId)).collect(Collectors.toList());
    }

    private static void writeExcel(String geneFileName,List<Rule> ruleList) {
        if(CollectionUtil.isEmpty(ruleList)){
            Console.error("生成文件失败，数据为空");
            return;
        }
        File file = new File(ABSOLUTE_PATH+ geneFileName);
        FileUtil.del(file);
        ExcelWriter writer = ExcelUtil.getWriter(file)
                .addHeaderAlias("fid", "策略ID")
                .addHeaderAlias("fname", "策略名称")
                .addHeaderAlias("fcategory", "告警分类")
                .addHeaderAlias("fsubcategory", "告警子类别")
                .addHeaderAlias("conditions", "条件预览")
                .addHeaderAlias("fkillchain", "攻击阶段");
        writer.setOnlyAlias(true);
        writer.getStyleSet().setAlign(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        // 一次性写出内容，使用默认样式，强制输出标题
        writer.write(ruleList, true);
        // 关闭writer，释放内存
        writer.close();
        Console.log("成功->生成文件：{},行数：{}",geneFileName, ruleList.size());
    }

    private static String[] login(String auth) {
        String userName = auth.split(":")[0];
        String password = auth.split(":")[1];
        HttpResponse reps =  HttpRequest.post(DOMAIN + URL.LOGIN)
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.createObj().set("type", "account").set("pass", password).set("user", userName).toString())
                .execute();
        String sessionId = reps.getCookie("SESSION_ID")+"";
        String repsBody = reps.body();
        Console.log("login api:{}\nsessionId:{};body:{}", URL.LOGIN, sessionId,repsBody);
        JSON json = JSONUtil.parse(repsBody);

        Object error = JSONUtil.getByPath(json, "returnCode");
        if(ObjectUtil.isNotNull(error)&&Integer.parseInt(error.toString())==-1){
            Console.log("账号密码错误！");
            return null;
        }
        return new String[]{JSONUtil.getByPath(json, "token")+"",sessionId};
    }


    @Data
    @Builder
    static class Rule {
        private String fid; //策略ID
        private String fname; //策略名称
        private String fcategory;//告警分类
        private String fsubcategory;//告警子类别
        private String conditions;//条件预览
        private String fkillchain; // 攻击阶段
        private Integer sortId; //排序ID
    }










}
