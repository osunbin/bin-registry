package com.bin.registry.server.http;

import com.bin.registry.server.common.utils.StringUtils;
import com.bin.registry.server.core.NodeManager;
import com.bin.registry.server.core.RegistryCenterApis;
import com.bin.registry.server.model.HttpPath;
import com.bin.registry.server.model.JsonResult;
import com.bin.registry.server.template.RegistryTemplate;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static com.bin.registry.server.model.HttpPath.NODE_LISTS;


@ChannelHandler.Sharable
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static Logger logger = LoggerFactory.getLogger(HttpServerHandler.class);

    RegistryCenterApis apis;


    RegistryTemplate registryTemplate;

    public HttpServerHandler() {
        apis = new RegistryCenterApis();
        registryTemplate = new RegistryTemplate();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws Exception {

        String uri = httpRequest.uri(); // 带参数 ? &
        // TODO
        if ("/favicon.ico".equals(uri))
            return;

        HttpMethod httpMethod = httpRequest.method();
        FullHttpRequest copyRequest = httpRequest.copy();
        NettyHttpRequest nettyHttpRequest = new NettyHttpRequest(copyRequest);
        Channel channel = ctx.channel();



        String uriPath = nettyHttpRequest.getUriPath(); // 去除参数

        HttpPath httpPath = HttpPath.getHttpPath(uriPath);
        if (httpPath == null) {
            man(uri, channel);
            return;
        }
        if (httpPath == NODE_LISTS) {
            FullHttpResponse response = NettyHttpResponse.ok(registryTemplate.getTemplate(NodeManager.fetchAllNode()));
            response.headers()
                    .set("content-type", "text/html;charset=utf-8");
            ctx.writeAndFlush(response);
            return;
        }


        JsonResult<?> jsonResult = null;
        if (httpMethod == HttpMethod.GET) {
            Map<String, Object> parameterMap = nettyHttpRequest.getParameterMap();

            String serviceName = (String) parameterMap.get("serviceName");

            String ip = (String) parameterMap.get("ip");

            switch (httpPath) {
                case NODES_SERVER:


                    jsonResult = JsonResult.
                            ok("success", NodeManager.fetchServerNode(serviceName,ip));
                    break;
                case NODE_CALLER:

                    if (StringUtils.isEmpty(serviceName)) {
                        jsonResult = JsonResult.failed("caller is null",null);
                        break;
                    }

                    jsonResult = JsonResult.
                            ok("success", NodeManager.fetchCallerNode(serviceName));
                    break;
                case NODE_OPEN:

                    NodeManager.openNode(serviceName,ip);
                    break;
                case NODE_CLOSE:

                    NodeManager.closeNode(serviceName,ip);
                    break;
                case NODE_DELETE:

                    NodeManager.deleteNode(serviceName,ip);
                    break;
            }

        } else if (httpMethod == HttpMethod.POST) {


            switch (httpPath) {
                case REGISTRY_PATH:

                    jsonResult = apis.registry(channel, nettyHttpRequest);
                    break;
                case DISCOVERY_PATH:

                    jsonResult = apis.discovery(channel, nettyHttpRequest);
                    break;
            }

        } else {
            if (logger.isInfoEnabled()) {
                logger.info("only support get and post " + httpMethod.name() + "-" + uri);
            }
        }

        if (jsonResult == null)
            jsonResult = JsonResult.ok();

        channel.writeAndFlush(NettyHttpResponse.ok(jsonResult.toJson()));

    }

    private void man(String uri, Channel channel) {
        String s = JsonResult.failed(uri + " - 该路径目前没有功能", HttpPath.fetchAllDoc()).toJson();
        logger.info("respone={}", s);
        channel.writeAndFlush(NettyHttpResponse.ok
                (s));
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            logger.error("IOException caught for channel {} , {}", ctx.channel(), cause.getMessage());
        } else {
            logger.error("Exception caught for channel {}, {}", ctx.channel(), cause.getMessage(), cause);
        }
    }
}
