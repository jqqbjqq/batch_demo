package com;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.sun.media.jfxmedia.logging.Logger;
import java.io.File;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.compress.utils.Lists;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;

public class ExcelTest {

    public static void main(String[] args) {
        //File file = new File("D:\\IdeaProjects\\batch_demo\\host.xlsx");
        // FileUtil.del(file);
        // List<Item> list  = Lists.newArrayList();
        // list.add(Item.builder().key("aaaaa").value("bbbbb").build());
        // list.add(Item.builder().key("ccccc").value("ddddd").build());
        // list.add(Item.builder().key("eeeee").value("fffff").build());
        writeExcel2();
    }

    private static void writeExcel2() {
        ExcelReader reader = ExcelUtil.getReader("D:\\IdeaProjects\\batch_demo\\host.xlsx")
                .addHeaderAlias("外网IP", "queryIp");
        final List<List<Object>> read = reader.read();
        List<Obj> objList = Lists.newArrayList();
        for (int i = 0; i < read.size(); i++) {
            for (int j = 0; j < read.get(i).size(); j++) {
                String cell = String.valueOf(read.get(i).get(j));
                if(cell.endsWith("_")){
                    //Console.log("[{}:{}:{}]",i,j,cell);
                    objList.add(Obj.builder().x(i).y(j).v(cell).build());
                }
            }
        }
        reader.close();

        ExcelWriter writer = ExcelUtil.getWriter("D:\\IdeaProjects\\batch_demo\\host.xlsx");
        //----------------------
        // 设置第二行第二列的背景色为红色
        CellStyle cellStyle = writer.getStyleSet()
                .setBorder(BorderStyle.NONE, IndexedColors.AUTOMATIC)
                .setAlign(HorizontalAlignment.LEFT, VerticalAlignment.CENTER)
                .getCellStyle();
        cellStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        objList.forEach(e->{
            Console.log("[{}:{}:{}]",e.getX(), e.getY(),e.getV());
            writer.writeCellValue(e.getY(),e.getX(), e.getV());
            writer.getOrCreateCell(e.getY(),e.getX()).setCellStyle(cellStyle);
        });
        // 关闭writer，释放内存
        writer.flush();
        writer.close();

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
        writer.getCell(1, 1).setCellValue("xx");
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

    @Data
    @Builder
    static class Obj{
        private int x;
        private int y;
        private String v;
    }
}
