package com.bin.registry.server.common.utils;

import com.bin.registry.server.http.NettyHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IpUtils {
    private static Logger logger = LoggerFactory.getLogger(IpUtils.class);
    private static final String IP_UTILS_FLAG = ",";
    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IP = "0:0:0:0:0:0:0:1";
    private static final String LOCALHOST_IP1 = "127.0.0.1";

    private static final Pattern HOST_FORMAT = Pattern.compile("(\\S*)\\/{1}(\\S+)");

    private static final Pattern ADDRESS_FORMAT = Pattern.compile("(?<host>^.*):(?<port>\\d+$)");


    public static String resoleHost(SocketAddress socketAddress) {
        String socketAddr = socketAddress.toString();
        Matcher m = HOST_FORMAT.matcher(socketAddr);
        if (!m.matches()) {
            throw new IllegalArgumentException(String.format("unsupported address [%s]!", new Object[] {socketAddress}));
        }
        String hostAndPort = m.group(2);
        if (!StringUtils.isEmpty(hostAndPort)) {
            throw new NullPointerException("hostAndPort can't be blank !");
        }
        Matcher matcher = ADDRESS_FORMAT.matcher(hostAndPort);
        String host = matcher.group(1);
        if (!StringUtils.isEmpty(host)) {
            throw new NullPointerException("host can't be empty！");
        }
        host = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) ? getLocalIpAddress().getHostAddress() : host;
        return host;
    }

    public static InetAddress getLocalIpAddress() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }



    /**
     * 将 ip 字符串转换为 int 类型的数字
     * <p>
     * 思路就是将 ip 的每一段数字转为 8 位二进制数，并将它们放在结果的适当位置上
     *
     * @param ipString ip字符串，如 127.0.0.1
     * @return ip字符串对应的 int 值
     */
    public static int ip2Int(String ipString) {
        // 取 ip 的各段
        String[] ipSlices = ipString.split("\\.");
        int rs = 0;
        for (int i = 0; i < ipSlices.length; i++) {
            // 将 ip 的每一段解析为 int，并根据位置左移 8 位
            int intSlice = Integer.parseInt(ipSlices[i]) << 8 * i;
            // 或运算
            rs = rs | intSlice;
        }
        return rs;
    }






    private static final String[] headersToTry = {
            //在k8s中，将真实的客户端IP，放到了x-Original-Forwarded-For。而将WAF的回源地址放到了 x-Forwarded-For了。
            "X-Original-Forwarded-For",
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR",
            // 自定义请求头
            "X-Custom-Forwarded-For",
    };

    /**
     * 获取用户的真正IP地址
     *
     * @param request request对象
     * @return 返回用户的IP地址
     */
    public static String getIpAddr(NettyHttpRequest request,String remoteAddr) {
        String ip = null;
        for (String header : headersToTry) {
            ip = request.getHeader(header);
            if (StringUtils.isNotEmpty(ip) && !UNKNOWN.equalsIgnoreCase(ip)){
                logger.info("hit the target client ip -> 【{}】 by header --> 【{}】",ip,header);
                return ip;
            }
        }
        //兼容k8s集群获取ip
        if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = remoteAddr;
            if (LOCALHOST_IP1.equalsIgnoreCase(ip) || LOCALHOST_IP.equalsIgnoreCase(ip)) {
                //根据网卡取本机配置的IP
                InetAddress iNet = null;
                try {
                    iNet = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    logger.error("getIpAddr error: {}", e);
                }
                ip = iNet.getHostAddress();
            }
            logger.info("hit the target client ip -> 【{}】 by method 【getRemoteAddr】 ",ip);
        }

        //使用代理，则获取第一个IP地址
        if (StringUtils.isNotEmpty(ip) && ip.indexOf(IP_UTILS_FLAG) > 0) {
            ip = ip.substring(0, ip.indexOf(IP_UTILS_FLAG));
        }
        return ip;
    }


    public static void main(String[] args) {

        long l = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
       long m =  System.currentTimeMillis() / 1000;

        System.out.println(TimeUnit.MILLISECONDS.toMillis(System.currentTimeMillis()));

        System.out.println(l);
        System.out.println(m);

    }

}