package com.bin.registry.server.template;

import com.bin.registry.server.core.NodeManager;
import com.bin.registry.server.model.Node;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RegistryTemplate {


    private Template nodesTemplate;

    public RegistryTemplate() {

        Configuration configuration = new Configuration(Configuration.getVersion());
        try {
            configuration.setDirectoryForTemplateLoading(new File(System.getProperty("user.dir") + "/core/src/main/resources/template"));
            configuration.setDefaultEncoding("utf-8");
            nodesTemplate = configuration.getTemplate("registry.ftl");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public  String getTemplate(List<Node> nodes) {
        List<Map<String, Object>> map = new LinkedList<>();
        for (Node node : nodes) {
            Map<String, Object> item= new HashMap<>();
            item.put("clusterName", node.getClusterName());
            item.put("serviceName", node.getServiceName());
            item.put("ip", node.getIp());
            item.put("running", node.getRunning() ? "运行中" : "停止");
            item.put("container", node.getContainer());
            item.put("systemEnv", node.getSystemEnv());
            item.put("port", node.getPort());
            item.put("pid", node.getPid());
            item.put("weight", node.getWeight());
            item.put("onLineTime", node.getOnLineTime());
            item.put("heartbeatTime", new Date(node.getHeartbeatTime()));
            item.put("tags", node.getTags());
            item.put("metadata", node.getMetadata());
            map.add(item);
        }
        Map<String,Object> data = new LinkedHashMap<>();

        data.put("nodes", map);
        StringWriter stringWriter = new StringWriter();

        try {
            if (nodesTemplate != null)
                    nodesTemplate.process(data, stringWriter);
        } catch (TemplateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }
}
