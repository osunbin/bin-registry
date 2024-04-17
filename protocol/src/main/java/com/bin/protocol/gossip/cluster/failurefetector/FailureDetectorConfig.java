package com.bin.protocol.gossip.cluster.failurefetector;

import com.bin.protocol.gossip.common.Exceptions;

import java.util.StringJoiner;

public final class FailureDetectorConfig {

  // Default settings for LAN cluster
  public static final int DEFAULT_PING_INTERVAL = 1_000;
  public static final int DEFAULT_PING_TIMEOUT = 500;
  public static final int DEFAULT_PING_REQ_MEMBERS = 3;

  // Default settings for WAN cluster (overrides default/LAN settings)
  public static final int DEFAULT_WAN_PING_TIMEOUT = 3_000;
  public static final int DEFAULT_WAN_PING_INTERVAL = 5_000;

  // Default settings for local cluster working via loopback interface (overrides default/LAN
  // settings)
  public static final int DEFAULT_LOCAL_PING_TIMEOUT = 200;
  public static final int DEFAULT_LOCAL_PING_INTERVAL = 1_000;
  public static final int DEFAULT_LOCAL_PING_REQ_MEMBERS = 1;

  private int pingInterval = DEFAULT_PING_INTERVAL;
  private int pingTimeout = DEFAULT_PING_TIMEOUT;
  private int pingReqMembers = DEFAULT_PING_REQ_MEMBERS;

  public FailureDetectorConfig() {}

  public static FailureDetectorConfig defaultConfig() {
    return new FailureDetectorConfig();
  }


  public static FailureDetectorConfig defaultLanConfig() {
    return defaultConfig();
  }


  public static FailureDetectorConfig defaultWanConfig() {
    return defaultConfig()
        .pingTimeout(DEFAULT_WAN_PING_TIMEOUT)
        .pingInterval(DEFAULT_WAN_PING_INTERVAL);
  }


  public static FailureDetectorConfig defaultLocalConfig() {
    return defaultConfig()
        .pingTimeout(DEFAULT_LOCAL_PING_TIMEOUT)
        .pingInterval(DEFAULT_LOCAL_PING_INTERVAL)
        .pingReqMembers(DEFAULT_LOCAL_PING_REQ_MEMBERS);
  }


  public FailureDetectorConfig pingInterval(int pingInterval) {
    this.pingInterval = pingInterval;
    return this;
  }

  public int pingInterval() {
    return pingInterval;
  }


  public FailureDetectorConfig pingTimeout(int pingTimeout) {
    this.pingTimeout = pingTimeout;
    return this;
  }

  public int pingTimeout() {
    return pingTimeout;
  }


  public FailureDetectorConfig pingReqMembers(int pingReqMembers) {
    this.pingReqMembers = pingReqMembers;
    return this;
  }

  public int pingReqMembers() {
    return pingReqMembers;
  }


  @Override
  public String toString() {
    return new StringJoiner(", ", FailureDetectorConfig.class.getSimpleName() + "[", "]")
        .add("pingInterval=" + pingInterval)
        .add("pingTimeout=" + pingTimeout)
        .add("pingReqMembers=" + pingReqMembers)
        .toString();
  }
}
