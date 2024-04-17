package com.bin.protocol.gossip.cluster;

import com.bin.protocol.gossip.common.Exceptions;
import com.bin.protocol.gossip.Member;
import com.bin.protocol.gossip.cluster.gossip.GossipConfig;
import com.bin.protocol.gossip.network.NetWorkConfig;
import com.bin.protocol.gossip.cluster.membership.MembershipConfig;
import com.bin.protocol.gossip.cluster.failurefetector.FailureDetectorConfig;

import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.UnaryOperator;

public final class ClusterConfig {

  // LAN cluster
  public static final int DEFAULT_METADATA_TIMEOUT = 3_000;

  // WAN cluster (overrides default/LAN settings)
  public static final int DEFAULT_WAN_METADATA_TIMEOUT = 10_000;

  // Local cluster working via loopback interface (overrides default/LAN settings)
  public static final int DEFAULT_LOCAL_METADATA_TIMEOUT = 1_000;



  private String memberId;
  private String externalHost;
  private Integer externalPort;

  private NetWorkConfig netWorkConfig = NetWorkConfig.defaultConfig();
  private FailureDetectorConfig failureDetectorConfig = FailureDetectorConfig.defaultConfig();
  private GossipConfig gossipConfig = GossipConfig.defaultConfig();
  private MembershipConfig membershipConfig = MembershipConfig.defaultConfig();

  public ClusterConfig() {}

  public static ClusterConfig defaultConfig() {
    return new ClusterConfig();
  }


  public static ClusterConfig defaultLanConfig() {
    return defaultConfig();
  }


  public static ClusterConfig defaultWanConfig() {
    return defaultConfig()
        .failureDetector(opts -> FailureDetectorConfig.defaultWanConfig())
        .gossip(opts -> GossipConfig.defaultWanConfig())
        .membership(opts -> MembershipConfig.defaultWanConfig())
        .transport(opts -> NetWorkConfig.defaultWanConfig());
  }


  public static ClusterConfig defaultLocalConfig() {
    return defaultConfig()
        .failureDetector(opts -> FailureDetectorConfig.defaultLocalConfig())
        .gossip(opts -> GossipConfig.defaultLocalConfig())
        .membership(opts -> MembershipConfig.defaultLocalConfig())
        .transport(opts -> NetWorkConfig.defaultLocalConfig());
  }


  /**
   * Returns externalHost. {@code externalHost} is a config property for container environments,
   * it's being set for advertising to scalecube cluster some connectable hostname which maps to
   * scalecube transport's hostname on which scalecube transport is listening.
   *
   * @return external host
   */
  public String externalHost() {
    return externalHost;
  }

  /**
   * Setter for externalHost. {@code externalHost} is a config property for container environments,
   * it's being set for advertising to scalecube cluster some connectable hostname which maps to
   * scalecube transport's hostname on which scalecube transport is listening.
   *
   * @param externalHost external host
   * @return new {@code ClusterConfig} instance
   */
  public ClusterConfig externalHost(String externalHost) {
    this.externalHost = externalHost;
    return this;
  }

  /**
   * Returns ID to use for the local member. If {@code null}, the ID will be generated
   * automatically.
   *
   * @return local member ID.
   */
  public String memberId() {
    return memberId;
  }

  /**
   * Sets ID to use for the local member. If {@code null}, the ID will be generated automatically.
   *
   * @param memberId local member ID
   * @return new {@code ClusterConfig} instance
   */
  public ClusterConfig memberId(String memberId) {
    this.memberId = memberId;
    return this;
  }



  /**
   * Returns externalPort. {@code externalPort} is a config property for container environments,
   * it's being set for advertising to scalecube cluster a port which mapped to scalecube
   * transport's listening port.
   *
   * @return external port
   */
  public Integer externalPort() {
    return externalPort;
  }

  /**
   * Setter for externalPort. {@code externalPort} is a config property for container environments,
   * it's being set for advertising to scalecube cluster a port which mapped to scalecube
   * transport's listening port.
   *
   * @param externalPort external port
   * @return new {@code ClusterConfig} instance
   */
  public ClusterConfig externalPort(Integer externalPort) {
    this.externalPort = externalPort;
    return this;
  }

  /**
   * Applies {@link NetWorkConfig} settings.
   *
   * @param op operator to apply {@link NetWorkConfig} settings
   * @return new {@code ClusterConfig} instance
   */
  public ClusterConfig transport(UnaryOperator<NetWorkConfig> op) {
    this.netWorkConfig = op.apply(netWorkConfig);
    return this;
  }

  public NetWorkConfig transportConfig() {
    return netWorkConfig;
  }

  /**
   * Applies {@link FailureDetectorConfig} settings.
   *
   * @param op operator to apply {@link FailureDetectorConfig} settings
   * @return new {@code ClusterConfig} instance
   */
  public ClusterConfig failureDetector(UnaryOperator<FailureDetectorConfig> op) {
    this.failureDetectorConfig = op.apply(failureDetectorConfig);
    return this;
  }

  public FailureDetectorConfig failureDetectorConfig() {
    return failureDetectorConfig;
  }

  /**
   * Applies {@link GossipConfig} settings.
   *
   * @param op operator to apply {@link GossipConfig} settings
   * @return new {@code ClusterConfig} instance
   */
  public ClusterConfig gossip(UnaryOperator<GossipConfig> op) {
    this.gossipConfig = op.apply(gossipConfig);
    return this;
  }

  public GossipConfig gossipConfig() {
    return gossipConfig;
  }

  /**
   * Applies {@link MembershipConfig} settings.
   *
   * @param op operator to apply {@link MembershipConfig} settings
   * @return new {@code ClusterConfig} instance
   */
  public ClusterConfig membership(UnaryOperator<MembershipConfig> op) {
    this.membershipConfig = op.apply(membershipConfig);
    return this;
  }

  public MembershipConfig membershipConfig() {
    return membershipConfig;
  }



  @Override
  public String toString() {
    return new StringJoiner(", ", ClusterConfig.class.getSimpleName() + "[", "]")
        .add("memberId='" + memberId + "'")
        .add("externalHost='" + externalHost + "'")
        .add("externalPort=" + externalPort)
        .add("transportConfig=" + netWorkConfig)
        .add("failureDetectorConfig=" + failureDetectorConfig)
        .add("gossipConfig=" + gossipConfig)
        .add("membershipConfig=" + membershipConfig)
        .toString();
  }

}
