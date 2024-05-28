package com;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import com.cctv.security.YunjingHostQuery;
import java.awt.SystemTray;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author jiqq
 * @date 2024/4/11
 * @description
 */
public class Test {

    public static void main(String[] args) {
        TimeInterval timer = DateUtil.timer();
        List<Integer> itemList = new ArrayList<>();
        for (int i = 0; i <= 10000; i++) {
            itemList.add(i);
        }

       // List<Integer> all = new CopyOnWriteArrayList<>();

        List<Integer> all = Collections.synchronizedList(new ArrayList<>());

        List<List<Integer>> partition = ListUtil.partition(itemList, 20);
        for (List<Integer>  list: partition) {
            list.stream().forEach(item->{
                System.out.println(item);
                if(item/100==0){
                    int i = 1 / 0;
                    //ThreadUtil.sleep(10);
                }
                all.add(item);
            });
        }
        System.out.println(all.size());
        Console.log("全部处理完成,耗时:{}秒", timer.intervalSecond());
    }

}
