package com.bin.registry.server;



import com.bin.registry.server.common.utils.PropertiesUtils;
import com.bin.registry.server.http.NettyHttpServer;

import java.util.Properties;

/**
 *  健康检查
 *      上报心跳   临时实例使用心跳上报方式维持活性
 *                   5秒上报
 *                   15秒标记不健康
 *                   30秒剔除
 *      服务探测
 *  服务发现
 *     客户端,list(服务端需要的服务端)
 *
 */
public class RegistryCenter {


    public static final String PORT = "server.port";

    public static final String ENV = "server.env";

    public static final String CONTEXT_PATH = "server.contextPath";

    public static Properties global;

    public static void main(String[] args) {
        global = PropertiesUtils.getProperties("conf/application.properties");
        String port = global.getProperty("server.port");

        NettyHttpServer httpServer =  new NettyHttpServer(Integer.valueOf(port));
        httpServer.start();
    }


}
