package com.bin.protocol.gossip.cluster;

public class ClusterMath {

    private ClusterMath(){}


    /**
     *  gossipConvergencePercent
     */
    public static double gossipConvergencePercent(
            int fanout, int repeatMult, int clusterSize, double lossPercent) {
        double msgLossProb = lossPercent / 100.0;
        return gossipConvergenceProbability(fanout, repeatMult, clusterSize, msgLossProb) * 100;
    }


    /**
     *  gossipConvergenceProbability
     */
    public static double gossipConvergenceProbability(
            int fanout, int repeatMult, int clusterSize, double loss) {
        double fanoutWithLoss = (1.0 - loss) * fanout;
        double spreadSize = clusterSize - Math.pow(clusterSize, -(fanoutWithLoss * repeatMult - 2));
        return spreadSize / clusterSize;
    }

    /**
     *  maxMessagesPerGossipTotal
     */
    public static int maxMessagesPerGossipTotal(int fanout, int repeatMult, int clusterSize) {
        return clusterSize * maxMessagesPerGossipPerNode(fanout, repeatMult, clusterSize);
    }

    /**
     *  maxMessagesPerGossipPerNode
     */
    public static int maxMessagesPerGossipPerNode(int fanout, int repeatMult, int clusterSize) {
        return fanout * repeatMult * ceilLog2(clusterSize);
    }


    /**
     * gossipDisseminationTime
     */
    public static long gossipDisseminationTime(int repeatMult, int clusterSize, long gossipInterval) {
        return gossipPeriodsToSpread(repeatMult, clusterSize) * gossipInterval;
    }

    /**
     * gossipTimeoutToSweep
     */
    public static long gossipTimeoutToSweep(int repeatMult, int clusterSize, long gossipInterval) {
        return gossipPeriodsToSweep(repeatMult, clusterSize) * gossipInterval;
    }


    /**
     * gossipPeriodsToSweep
     */
    public static int gossipPeriodsToSweep(int repeatMult, int clusterSize) {
        int periodsToSpread = gossipPeriodsToSpread(repeatMult, clusterSize);
        return 2 * (periodsToSpread + 1);
    }

    /**
     * gossipPeriodsToSpread
     */
    public static int gossipPeriodsToSpread(int repeatMult, int clusterSize) {
        return repeatMult * ceilLog2(clusterSize);
    }

    /**
     * suspicionTimeout
     */
    public static long suspicionTimeout(int suspicionMult, int clusterSize, long pingInterval) {
        return suspicionMult * ceilLog2(clusterSize) * pingInterval;
    }

    /**
     * ceilLog2
     */
    private static int ceilLog2(int num) {
        return 32 - Integer.numberOfLeadingZeros(num);
    }

}
