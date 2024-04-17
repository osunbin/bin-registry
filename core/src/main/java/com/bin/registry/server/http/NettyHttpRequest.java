package com.bin.registry.server.http;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.bin.registry.server.common.utils.JsonUtils;
import com.bin.registry.server.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

public class NettyHttpRequest implements FullHttpRequest {
    public static Logger logger = LoggerFactory.getLogger(NettyHttpRequest.class);

    private FullHttpRequest realRequest;

    public NettyHttpRequest(FullHttpRequest request) {
        this.realRequest = request;
    }

    public String getPostContent() {
        return content().toString(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getParameterMap() {
        Map<String, Object> params = new HashMap<>();

        String uriWithParam = realRequest.uri();
        /**
         * parse post parameter
         */
        if (content().isReadable()) {
            String data = content().toString(StandardCharsets.UTF_8);
            try {
                params.putAll(JsonUtils.jsonToMap(data));
            } catch (Exception e) {
                // 表示不是json传参
                if (data.contains("=")) {
                    uriWithParam = uriWithParam + "&" + data;
                } else if (!StringUtils.isEmpty(data)) {
                    logger.error("desc=parse_param data={}", data);
                    throw e;
                }
            }
        }
        /**
         * parse uri parameter
         */
        QueryStringDecoder getDecoder = new QueryStringDecoder(uriWithParam, CharsetUtil.UTF_8, true, uriWithParam.length());
        Map<String, List<String>> getParams = getDecoder.parameters();
        // 对List进行拆解，大部分参数不是list类型
        getParams.forEach((k, v) -> params.put(k, v.size() > 1 ? v : v.get(0)));
        return params;
    }

    public long getLongPathValue(int index) {
        String[] paths = getUriPath().split("/");
        return Long.parseLong(paths[index]);
    }

    public String getStringPathValue(int index) {
        String[] paths = getUriPath().split("/");
        return paths[index];
    }

    public int getIntPathValue(int index) {
        String[] paths = getUriPath().split("/");
        return Integer.parseInt(paths[index]);
    }

    public boolean isAllowed(String[] method) {
        return Arrays.stream(method).anyMatch(x -> x.equalsIgnoreCase(getMethod().name().trim()));
// return getMethod().name().equalsIgnoreCase(method);
    }

    public boolean matched(String path, boolean equal) {
        String uri = getUriPath().toLowerCase();
        return equal ? Objects.equals(path, uri) : uri.startsWith(path);
    }

    public List<Map.Entry<String, String>> getAllHeaders() {
        return headers().entries();
    }

    public Set<Map.Entry<String, String>> getCookies() {
        Optional<Map.Entry<String, String>> cookie = headers().entries().stream().filter(kv -> kv.getKey().equalsIgnoreCase("cookie")).findFirst();

        return cookie.map(stringEntry -> Arrays.stream(stringEntry.getValue().split(";")).map(x -> x.split("=")).filter(x -> x.length == 2).map(x -> Arrays.stream(x).map(String::trim).collect(Collectors.toList())).collect(Collectors.toMap(x -> x.get(0), x -> x.get(1))).entrySet()).orElse(null);
    }

    public String getHeader(String name) {
        return headers().get(name);
    }

    public String getUriPath() {
        return uri().split("\\?")[0];
    }

    @Override
    public ByteBuf content() {

        return realRequest.content();
    }

    @Override
    public HttpHeaders trailingHeaders() {
        return realRequest.trailingHeaders();
    }

    @Override
    public FullHttpRequest copy() {
        return realRequest.copy();
    }

    @Override
    public FullHttpRequest duplicate() {
        return realRequest.duplicate();
    }

    @Override
    public FullHttpRequest retainedDuplicate() {
        return realRequest.retainedDuplicate();
    }

    @Override
    public FullHttpRequest replace(ByteBuf byteBuf) {
        return realRequest.replace(byteBuf);
    }

    @Override
    public FullHttpRequest retain(int i) {
        return realRequest.retain(i);
    }

    @Override
    public int refCnt() {
        return realRequest.refCnt();
    }

    @Override
    public FullHttpRequest retain() {
        return realRequest.retain();
    }

    @Override
    public FullHttpRequest touch() {
        return realRequest.touch();
    }

    @Override
    public FullHttpRequest touch(Object o) {
        return realRequest.touch(o);
    }

    @Override
    public boolean release() {
        return realRequest.release();
    }

    @Override
    public boolean release(int i) {
        return realRequest.release(i);
    }

    @Override
    public HttpVersion getProtocolVersion() {
        return realRequest.protocolVersion();
    }

    @Override
    public HttpVersion protocolVersion() {
        return realRequest.protocolVersion();
    }

    @Override
    public FullHttpRequest setProtocolVersion(HttpVersion httpVersion) {
        return realRequest.setProtocolVersion(httpVersion);
    }

    @Override
    public HttpHeaders headers() {
        return realRequest.headers();
    }

    @Override
    public HttpMethod getMethod() {
        return realRequest.method();
    }

    @Override
    public HttpMethod method() {
        return realRequest.method();
    }

    @Override
    public FullHttpRequest setMethod(HttpMethod httpMethod) {
        return realRequest.setMethod(httpMethod);
    }

    @Override
    public String getUri() {
        return realRequest.uri();
    }

    @Override
    public String uri() {
        return realRequest.uri();
    }

    @Override
    public FullHttpRequest setUri(String s) {
        return realRequest.setUri(s);
    }

    @Override
    public DecoderResult getDecoderResult() {
        return realRequest.decoderResult();
    }

    @Override
    public DecoderResult decoderResult() {
        return realRequest.decoderResult();
    }

    @Override
    public void setDecoderResult(DecoderResult decoderResult) {
        realRequest.setDecoderResult(decoderResult);
    }
}
