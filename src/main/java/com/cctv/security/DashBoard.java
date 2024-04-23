package com.cctv.security;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Random;

/**
 * @author jiqq
 * @date 2024/4/23
 * @description
 */
public class DashBoard {

    public static void main(String[] args) {
        // 创建日期格式器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        // 获取当前日期并计算一个月前的日期
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();

        // 逐天打印从一个月前到今天的日期
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1; // 加1包括今天
        for (long i = 0; i < daysBetween; i++) {
            System.out.println("\""+startDate.plusDays(i).format(formatter)+"\",");
        }
        System.out.println("------------------------------");
        // 创建随机数生成器
        Random random = new Random();
        // 生成随机数并计算总和
        int[] randomNumbers = new int[30];
        int totalSum = 0;
        for (int i = 0; i < 30; i++) {
            randomNumbers[i] = random.nextInt(50) + 1; // 生成1到50之间的随机数
            totalSum += randomNumbers[i];
            System.out.println(randomNumbers[i]+",");
        }
        // 输出总和
        System.out.println("总和: " + totalSum);
    }

}
