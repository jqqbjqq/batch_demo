package com;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.hutool.poi.excel.StyleSet;
import cn.hutool.poi.excel.style.StyleUtil;
import java.io.File;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.compress.utils.Lists;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;


public class ExcelTest {

    public static void main(String[] args) {
        File file = new File("D:\\IdeaProjects\\batch_demo\\host.xlsx");
        FileUtil.del(file);
        List<Item> list  = Lists.newArrayList();
        list.add(Item.builder().key("aaaaa").value("bbbbb").build());
        list.add(Item.builder().key("ccccc").value("ddddd").build());
        list.add(Item.builder().key("eeeee").value("fffff").build());
        writeExcel(file,"xxx",list);
    }


    private static void writeExcel(File file,String sheetName, List<Item> itemList) {
        if(CollectionUtil.isEmpty(itemList)){
            Console.error("生成文件失败，数据为空");
            return;
        }
        ExcelWriter writer = ExcelUtil.getWriter(file)
                .addHeaderAlias("key", "资产名")
                .addHeaderAlias("value", "资产数量");


        //writer.setOnlyAlias(true).setSheet(sheetName);
        writer.getStyleSet().setBorder(BorderStyle.NONE, IndexedColors.AUTOMATIC)
                .setAlign(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);


        // 一次性写出内容，使用默认样式，强制输出标题
        writer.write(itemList, true);

        //---------------
        Font font = writer.createFont();
        font.setColor(Font.COLOR_RED);
        CellStyle cellStyle = writer.createCellStyle();
        cellStyle.setFont(font);
        writer.getCell(0, 1).setCellStyle(cellStyle);
        writer.getCell(1, 1).setCellStyle(cellStyle);
        //---------------

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
}
