package com.bin.protocol.gossip.cluster.failurefetector;

public enum MemberStatus {

  /** Member is reachable and responding on messages. */
  ALIVE,

  /** Member can't be reached and marked as suspected to be failed.
   *  无法联系到成员并将其标记为疑似失败
   * */
  SUSPECT,

  /** Member want to leave cluster gracefully.
   * 成员希望优雅地离开群集
   *
   * */
  LEAVING,

  /**
   * Member declared as dead after being {@link #SUSPECT} for configured time or when node has been
   * gracefully shutdown and left cluster.
   */
  DEAD
}