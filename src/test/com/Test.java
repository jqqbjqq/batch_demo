package com;

import cn.hutool.core.util.ReUtil;


public class Test {

    public static void main(String[] args) {
        String str = "GgsKYYG-NO7zlpzDQWG_USA; session=bca97105-34bb-49cb-a701-c2d0f800219a" +
                ".H-jpphsm24nCOH91Nd2bBt0HjVc; csrf_access_token=9dc39b78-82cb-4d36-869e-888b1ca157a5; " +
                "csrf_refresh_token=defec910-6277-4300-9e94-42e47016b15f;xxx";
        String g1 = ReUtil.getGroup1("csrf_access_token=([^;]+)", str);
        String g2 = ReUtil.getGroup1("csrf_refresh_token=([^;]+)", str);
        System.out.println(g1);
        System.out.println(g2);
    }

}
