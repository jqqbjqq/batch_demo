package com;

/**
 * @author jiqq
 * @date 2024/4/11
 * @description
 */
public class Test {

    public static void main(String[] args) {
        String str = "【中危】 SSL 证书不可信 （443 / tcp / www6443 / tcp / www）【中危】 TLS 1.0 版协议检测 （443 / tcp / www）【低危】 SSL/TLS Diffie-Hellman 模数 &= 1024 位 (Logjam) （8443 / tcp / www）【中危】 SSL 自签名证书 （9100 / tcp / www）【低危】 Nessus SYN 扫描器 （80 / tcp / www7000 / tcp / 8080 / tcp / www8081 / tcp / www8082 / tcp / www48002 / tcp / 48003 / tcp / 8082 / tcp / www8083 / tcp / www8088 / tcp / www8090 / tcp / www8443 / tcp / www8500 / tcp / www9090 / tcp / 9091 / tcp / 10082 / tcp / www）【低危】 超文本传输协议 (HTTP) 信息 （80 / tcp / www8080 / tcp / www8081 / tcp / www8082 / tcp / www）【低危】 服务检测 （8080 / tcp / www, 8081 / tcp / www, 8082 / tcp / www, 80 / tcp / www8443 / tcp / www, 443 / tcp / www8443 / tcp / www, 443 / tcp / www2873 / tcp / rsyncd）【低危】 HTTP 服务器类型和版本 （80 / tcp / www8080 / tcp / www, 8082 / tcp / www8081 / tcp / www）【低危】 服务检测（HELP 请求） （3306 / tcp / mysql）【低危】 nginx HTTP 服务器检测 （8081 / tcp / www443 / tcp / www800 / tcp / www8080 / tcp / www8088 / tcp / www10082 / tcp / www）【低危】 支持的 SSL / TLS 版本 （6443 / tcp / www, 443 / tcp / www）【低危】 HTTPS 服务器缺少 HSTS （6443 / tcp / www, 443 / tcp / www）【低危】 支持 SSL 密码块链密码套件 （443 / tcp / www6443 / tcp / www）【低危】 SSL 证书信息 （443 / tcp / www6443 / tcp / www）【低危】 支持 SSL 密码套件 （443 / tcp / www6443 / tcp / www）【低危】 TLS 1.1 版协议检测 （443 / tcp / www）【低危】 支持 SSL 完美前向保密密码套件 （443 / tcp / www6443 / tcp / www）【低危】 通用平台枚举 (CPE) （0 / tcp / ）【低危】 TLS 1.2 版协议检测 （6443 / tcp / www, 443 / tcp / www）【低危】 OpenSSL 检测 （443 / tcp / www）【低危】 Nessus 扫描信息 （0 / tcp / ）【低危】 SSL 证书链包含即将到期的证书 （443 / tcp / www）【低危】 操作系统识别失败 （0 / tcp / ）【低危】 SSL 证书链未排序 （443 / tcp / www）【低危】 SSL 证书链包含不必要的证书 （443 / tcp / www）【低危】 SSL 证书到期 - 未来到期 （443 / tcp / www）【低危】 SSL 根证书颁发机构证书信息 （443 / tcp / www）【低危】 使用弱散列算法（已知 CA）签名的 SSL 证书 （443 / tcp / www）【低危】 支持的 TLS Next 协议 （6443 / tcp / www）【低危】 TLS ALPN 支持的协议枚举 （6443 / tcp / www）【低危】 跟踪路由信息 （0 / udp / ）【低危】 TLS NPN 支持的协议枚举 （6443 / tcp / www）  ";
        String[] split = str.replaceAll("【","【www】【").split("【www】");
        for (String s : split) {
            System.out.println(s);
        }
    }

}
