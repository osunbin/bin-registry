package com.bin.registry.server.model;

import com.bin.registry.server.common.utils.JsonUtils;
import com.bin.registry.server.common.utils.MD5Utils;
import com.bin.registry.server.core.timer.WatchKey;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;

public class Node extends WatchKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long serviceId;

    /**
     * 注册名
     */
    private String serviceName;

    /**
     * ip
     */
    private String ip;



    /**
     * 运行状态 0:停止 1:运行中
     */
    private Boolean running;



    /**
     * 集群名
     */
    private String clusterName;

    /**
     * 服务器类型：vm、物理机、虚拟机
     */
    private String container;

    /**
     * 环境类型
     */
    private String systemEnv; // 15



    /**
     * 端口
     */
    private Integer port;

    /**
     * 进程pid
     */
    private Integer pid;

    /**
     * ip权重
     */
    private Short weight;

    /**
     * 上线时间：服务第一次发送心跳时间(晚于启动时间, 不等于服务可用时间)
     */
    private transient Date onLineTime;


    /**
     * 心跳时间
     */
    private transient long heartbeatTime;

    private transient Date createTime;

    /**
     * idc机房名字
     */
    private String zoneName;

    /**
     * 分组信息
     */
    private String groupArray;

    /**
     * 节点标签
     */
    private String tags;

    /**
     * 扩展字段
     */
    private String metadata;

    public boolean isActive() {
       return running;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Boolean getRunning() {
        return running;
    }

    public void setRunning(Boolean running) {
        this.running = running;
    }



    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }


    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getSystemEnv() {
        return systemEnv;
    }

    public void setSystemEnv(String systemEnv) {
        this.systemEnv = systemEnv;
    }



    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getPid() {
        return pid;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
    }

    public Short getWeight() {
        return weight;
    }

    public void setWeight(Short weight) {
        this.weight = weight;
    }


    public Date getOnLineTime() {
        return onLineTime;
    }

    public void setOnLineTime(Date onLineTime) {
        this.onLineTime = onLineTime;
    }


    public long getHeartbeatTime() {
        return heartbeatTime;
    }

    public void setHeartbeatTime(long heartbeatTime) {
        this.heartbeatTime = heartbeatTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getGroupArray() {
        return groupArray;
    }

    public void setGroupArray(String groupArray) {
        this.groupArray = groupArray;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return  Objects.equals(serviceName, node.serviceName) && Objects.equals(ip, node.ip)  && Objects.equals(port, node.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, ip,  port);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Node{");
        sb.append("serviceId=").append(serviceId);
        sb.append(", serviceName='").append(serviceName).append('\'');
        sb.append(", ip='").append(ip).append('\'');
        sb.append(", running=").append(running);
        sb.append(", clusterName='").append(clusterName).append('\'');
        sb.append(", container='").append(container).append('\'');
        sb.append(", systemEnv='").append(systemEnv).append('\'');
        sb.append(", port=").append(port);
        sb.append(", pid=").append(pid);
        sb.append(", weight=").append(weight);
        sb.append(", onLineTime=").append(onLineTime);
        sb.append(", heartbeatTime=").append(heartbeatTime);
        sb.append(", createTime=").append(createTime);
        sb.append(", zoneName='").append(zoneName).append('\'');
        sb.append(", groupArray='").append(groupArray).append('\'');
        sb.append(", tags='").append(tags).append('\'');
        sb.append(", metadata='").append(metadata).append('\'');
        sb.append('}');
        return sb.toString();
    }


    public String toJson() {
       return JsonUtils.toJson(this);
    }

}
