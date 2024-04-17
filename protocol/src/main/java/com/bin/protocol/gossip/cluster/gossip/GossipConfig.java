package com.bin.protocol.gossip.cluster.gossip;

import com.bin.protocol.gossip.common.Exceptions;

import java.util.StringJoiner;

public final class GossipConfig{

  // Default settings for LAN cluster
  public static final long DEFAULT_GOSSIP_INTERVAL = 200;
  public static final int DEFAULT_GOSSIP_FANOUT = 3;
  public static final int DEFAULT_GOSSIP_REPEAT_MULT = 3;
  public static final int GOSSIP_SEGMENTATION_THRESHOLD = 1000;

  // Default settings for WAN cluster (overrides default/LAN settings)
  public static final int DEFAULT_WAN_GOSSIP_FANOUT = 4;

  // Default settings for local cluster working via loopback interface (overrides default/LAN
  // settings)
  public static final int DEFAULT_LOCAL_GOSSIP_REPEAT_MULT = 2;
  public static final int DEFAULT_LOCAL_GOSSIP_INTERVAL = 100;

  private int gossipFanout = DEFAULT_GOSSIP_FANOUT;
  private long gossipInterval = DEFAULT_GOSSIP_INTERVAL;
  private int gossipRepeatMult = DEFAULT_GOSSIP_REPEAT_MULT;
  private int gossipSegmentationThreshold = GOSSIP_SEGMENTATION_THRESHOLD;

  public GossipConfig() {}

  public static GossipConfig defaultConfig() {
    return new GossipConfig();
  }


  public static GossipConfig defaultLanConfig() {
    return defaultConfig();
  }


  public static GossipConfig defaultWanConfig() {
    return defaultConfig().gossipFanout(DEFAULT_WAN_GOSSIP_FANOUT);
  }


  public static GossipConfig defaultLocalConfig() {
    return defaultConfig()
        .gossipRepeatMult(DEFAULT_LOCAL_GOSSIP_REPEAT_MULT)
        .gossipInterval(DEFAULT_LOCAL_GOSSIP_INTERVAL);
  }


  public GossipConfig gossipFanout(int gossipFanout) {
    this.gossipFanout = gossipFanout;
    return this;
  }

  public int gossipFanout() {
    return gossipFanout;
  }


  public GossipConfig gossipInterval(long gossipInterval) {
    this.gossipInterval = gossipInterval;
    return this;
  }

  public long gossipInterval() {
    return gossipInterval;
  }


  public GossipConfig gossipRepeatMult(int gossipRepeatMult) {
    this.gossipRepeatMult = gossipRepeatMult;
    return this;
  }

  public int gossipRepeatMult() {
    return gossipRepeatMult;
  }


  public GossipConfig gossipSegmentationThreshold(int gossipSegmentationThreshold) {
    this.gossipSegmentationThreshold = gossipSegmentationThreshold;
    return this;
  }

  /**
   * A threshold for received gossip id intervals. If number of intervals is more than threshold
   * then warning will be raised, this mean that node losing network frequently for a long time.
   *
   * <p>For example if we received gossip with id 1,2 and 5 then we will have 2 intervals [1, 2],
   * [5, 5].
   *
   * @return gossip segmentation threshold
   */
  public int gossipSegmentationThreshold() {
    return gossipSegmentationThreshold;
  }


  @Override
  public String toString() {
    return new StringJoiner(", ", GossipConfig.class.getSimpleName() + "[", "]")
        .add("gossipFanout=" + gossipFanout)
        .add("gossipInterval=" + gossipInterval)
        .add("gossipRepeatMult=" + gossipRepeatMult)
        .add("gossipSegmentationThreshold=" + gossipSegmentationThreshold)
        .toString();
  }
}
