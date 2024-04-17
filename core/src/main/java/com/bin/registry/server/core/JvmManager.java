package com.bin.registry.server.core;

import com.bin.registry.server.common.utils.StringUtils;
import com.sun.management.HotSpotDiagnosticMXBean;


import javax.management.MBeanServer;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;



public class JvmManager {


    public static Map<String, Object> doDeadlockCheck() {
        String appname = "";
        try {
            ThreadMXBean tBean = ManagementFactory.getThreadMXBean();
            Map<String, Object> json = new HashMap<>();
            long[] dTh = tBean.findDeadlockedThreads();
            if (dTh != null) {
                ThreadInfo[] threadInfo = tBean.getThreadInfo(dTh, Integer.MAX_VALUE);
                StringBuffer sb = new StringBuffer();
                for (ThreadInfo info : threadInfo) {
                    sb.append("\n").append(info);
                }
                json.put("hasdeadlock", true);
                json.put("info", sb);
                return json;
            }
            json.put("hasdeadlock", false);
            return json;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    public static String doDumpThread(String threadId) {
        try {
            ThreadMXBean tBean = ManagementFactory.getThreadMXBean();
            String dumpThread = null;
            if (StringUtils.isNotEmpty(threadId)) {
                Long id = Long.valueOf(threadId);
                ThreadInfo threadInfo = tBean.getThreadInfo(id, Integer.MAX_VALUE);
                dumpThread = threadInfo.toString();
            } else {
                ThreadInfo[] dumpAllThreads = tBean.dumpAllThreads(false, false);
                StringBuffer info = new StringBuffer();
                for (ThreadInfo threadInfo : dumpAllThreads) {
                    info.append("\n").append(threadInfo);
                }
                dumpThread = info.toString();
            }
            return dumpThread;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    public static Integer getPid() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        return Integer.parseInt(runtimeMXBean.getName().split("@")[0]);
    }


    public static String doGC() {
        try {
            ManagementFactory.getMemoryMXBean().gc();
            return "GC Success";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void doHeapDump(String appname) {
        try {
            // 20240405 09:40:26 -> 2024_4_5____9_39_26
            DateFormat fmt = DateFormat.getDateTimeInstance();
            String date = fmt.format(new Date()).replaceAll("\\D", "_");

            String path = System.getProperty("user.dir") + File.separator + "dump";
            File root = new File(path);
            if (!root.exists()) {
                root.mkdirs();
            }
            String dumpPath = path + File.separator +
                    String.format("%s_%s_heap.hprof", appname, date);
            File file = new File(dumpPath);
            String dumpFile = file.getAbsolutePath();

            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(
                    server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
            mxBean.dumpHeap(dumpFile, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
