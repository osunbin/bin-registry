package com.bin.registry.server.core.timer;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.bin.registry.server.core.timer.TimerTaskList.TimerTaskEntry;
/*
 * Hierarchical Timing Wheels
 *
 * A simple timing wheel is a circular list of buckets of timer tasks. Let u be the time unit.
 * A timing wheel with size n has n buckets and can hold timer tasks in n * u time interval.
 * Each bucket holds timer tasks that fall into the corresponding time range. At the beginning,
 * the first bucket holds tasks for [0, u), the second bucket holds tasks for [u, 2u), …,
 * the n-th bucket for [u * (n -1), u * n). Every interval of time unit u, the timer ticks and
 * moved to the next bucket then expire all timer tasks in it. So, the timer never insert a task
 * into the bucket for the current time since it is already expired. The timer immediately runs
 * the expired task. The emptied bucket is then available for the next round, so if the current
 * bucket is for the time t, it becomes the bucket for [t + u * n, t + (n + 1) * u) after a tick.
 * A timing wheel has O(1) cost for insert/delete (start-timer/stop-timer) whereas priority queue
 * based timers, such as java.util.concurrent.DelayQueue and java.util.Timer, have O(log n)
 * insert/delete cost.
 *
 * A major drawback of a simple timing wheel is that it assumes that a timer request is within
 * the time interval of n * u from the current time. If a timer request is out of this interval,
 * it is an overflow. A hierarchical timing wheel deals with such overflows. It is a hierarchically
 * organized timing wheels. The lowest level has the finest time resolution. As moving up the
 * hierarchy, time resolutions become coarser. If the resolution of a wheel at one level is u and
 * the size is n, the resolution of the next level should be n * u. At each level overflows are
 * delegated to the wheel in one level higher. When the wheel in the higher level ticks, it reinsert
 * timer tasks to the lower level. An overflow wheel can be created on-demand. When a bucket in an
 * overflow bucket expires, all tasks in it are reinserted into the timer recursively. The tasks
 * are then moved to the finer grain wheels or be executed. The insert (start-timer) cost is O(m)
 * where m is the number of wheels, which is usually very small compared to the number of requests
 * in the system, and the delete (stop-timer) cost is still O(1).
 *
 * Example
 * Let's say that u is 1 and n is 3. If the start time is c,
 * then the buckets at different levels are:
 *
 * level    buckets
 * 1        [c,c]   [c+1,c+1]  [c+2,c+2]
 * 2        [c,c+2] [c+3,c+5]  [c+6,c+8]
 * 3        [c,c+8] [c+9,c+17] [c+18,c+26]
 *
 * The bucket expiration is at the time of bucket beginning.
 * So at time = c+1, buckets [c,c], [c,c+2] and [c,c+8] are expired.
 * Level 1's clock moves to c+1, and [c+3,c+3] is created.
 * Level 2 and level3's clock stay at c since their clocks move in unit of 3 and 9, respectively.
 * So, no new buckets are created in level 2 and 3.
 *
 * Note that bucket [c,c+2] in level 2 won't receive any task since that range is already covered in level 1.
 * The same is true for the bucket [c,c+8] in level 3 since its range is covered in level 2.
 * This is a bit wasteful, but simplifies the implementation.
 *
 * 1        [c+1,c+1]  [c+2,c+2]  [c+3,c+3]
 * 2        [c,c+2]    [c+3,c+5]  [c+6,c+8]
 * 3        [c,c+8]    [c+9,c+17] [c+18,c+26]
 *
 * At time = c+2, [c+1,c+1] is newly expired.
 * Level 1 moves to c+2, and [c+4,c+4] is created,
 *
 * 1        [c+2,c+2]  [c+3,c+3]  [c+4,c+4]
 * 2        [c,c+2]    [c+3,c+5]  [c+6,c+8]
 * 3        [c,c+8]    [c+9,c+17] [c+18,c+18]
 *
 * At time = c+3, [c+2,c+2] is newly expired.
 * Level 2 moves to c+3, and [c+5,c+5] and [c+9,c+11] are created.
 * Level 3 stay at c.
 *
 * 1        [c+3,c+3]  [c+4,c+4]  [c+5,c+5]
 * 2        [c+3,c+5]  [c+6,c+8]  [c+9,c+11]
 * 3        [c,c+8]    [c+9,c+17] [c+8,c+11]
 *
 * The hierarchical timing wheels works especially well when operations are completed before they time out.
 * Even when everything times out, it still has advantageous when there are many items in the timer.
 * Its insert cost (including reinsert) and delete cost are O(m) and O(1), respectively while priority
 * queue based timers takes O(log N) for both insert and delete where N is the number of items in the queue.
 *
 * This class is not thread-safe. There should not be any add calls while advanceClock is executing.
 * It is caller's responsibility to enforce it. Simultaneous add calls are thread-safe.
 */
public class TimingWheel {
    /**
     *  滴答一次的时长，类似于手表的例子中向前推进一格的时间。对于秒针而言，
     *  tickMs 就是 1 秒。同理，分针是 1 分，时针是 1 小时。在 Kafka 中，
     *  第 1 层时间轮的 tickMs 被固定为 1 毫秒，也就是说，向前推进一格 Bucket 的时长是 1 毫秒。
     */
    private long tickMs;
    /**
     *  每一层时间轮上的 Bucket 数量。第 1 层的 Bucket 数量是 20
     */
    private int wheelSize;

    /**
     *  这一层时间轮上的总定时任务数
     */
    private AtomicInteger taskCounter;
    /**
     *  将所有 Bucket 按照过期时间排序的延迟队列。
     *  随着时间不断向前推进，Kafka 需要依靠这个队列获取那些已过期的 Bucket，并清除它们
     */
    private DelayQueue<TimerTaskList> queue;

    /**
     *  这层时间轮总时长，等于滴答时长乘以 wheelSize。以第 1 层为例，
     *  interval 就是 20 毫秒。由于下一层时间轮的滴答时长就是上一层的总时长，
     *  因此，第 2 层的滴答时长就是 20 毫秒，总时长是 400 毫秒，以此类推。
     */
    private long interval;

    private TimerTaskList[] buckets;

    // rounding down to multiple of tickMs
    /**
     *  当前时间戳，只是源码对它进行了一些微调整，
     *  将它设置成小于当前时间的最大滴答时长的整数倍。
     *  举个例子，假设滴答时长是 20 毫秒，当前时间戳是 123 毫秒，
     *  那么，currentTime 会被调整为 120 毫秒。
     */
    private long currentTime;

    private TimingWheel overflowWheel;

    public TimingWheel(long tickMs, int wheelSize, long startMs, AtomicInteger taskCounter, DelayQueue<TimerTaskList> queue) {
        // 1
        this.tickMs = tickMs;
        // 20
        this.wheelSize = wheelSize;
        this.taskCounter = taskCounter;
        this.queue = queue;
        this.interval = tickMs * wheelSize;
        // startMs = 当前时间戳 毫秒
        this.currentTime = startMs - (startMs % tickMs);
        this.buckets = new TimerTaskList[wheelSize];
    }



    public synchronized void addOverflowWheel() {
        if (overflowWheel == null) {
            overflowWheel = new TimingWheel(
                     interval,
                     wheelSize,
                     currentTime,
                     taskCounter,
                    queue
            );
        }
    }


    public boolean add(TimerTaskList.TimerTaskEntry timerTaskEntry) {
        long expiration = timerTaskEntry.getExpirationMs();

        if (timerTaskEntry.cancelled()) {
            // Cancelled
            return false;
        } else if (expiration < currentTime + tickMs) {
            // Already expired
            return false;
        } else if (expiration < currentTime + interval) {
            // Put in its own bucket
            long virtualId = expiration / tickMs;
            TimerTaskList bucket = buckets[(int) (virtualId % wheelSize)];
            if (bucket == null)
                bucket = new TimerTaskList(taskCounter);
            bucket.add(timerTaskEntry);

            // Set the bucket expiration time
            if (bucket.setExpiration(virtualId * tickMs)) {
                // The bucket needs to be enqueued because it was an expired bucket
                // We only need to enqueue the bucket when its expiration time has changed, i.e. the wheel has advanced
                // and the previous buckets gets reused; further calls to set the expiration within the same wheel cycle
                // will pass in the same value and hence return false, thus the bucket with the same expiration will not
                // be enqueued multiple times.
                // 该存储桶需要排队，因为它是一个过期的存储桶。我们只需要在存储桶的过期时间发生变化时
                // （即车轮提前）将其排队并且先前的桶被重复使用；
                // 在同一轮周期内设置过期时间的进一步调用将以相同的值通过，
                // 因此返回false，因此具有相同过期时间的bucket将不会多次排队
                queue.offer(bucket);
            }
            return true;
        } else {
            // Out of the interval. Put it into the parent timer
            // 超出间隔。将其放入父计时器
            if (overflowWheel == null) addOverflowWheel();
           return overflowWheel.add(timerTaskEntry);
        }
    }

    public void advanceClock(long timeMs) {
        if (timeMs >= currentTime + tickMs) {
            currentTime = timeMs - (timeMs % tickMs);

            // Try to advance the clock of the overflow wheel if present
            if (overflowWheel != null) overflowWheel.advanceClock(currentTime);
        }
    }
}
