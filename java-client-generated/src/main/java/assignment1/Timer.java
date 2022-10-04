package assignment1;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Class of timer
 */
public class Timer {
    public long startTime;
    public long endTime;

    public void startTimer() {
        this.startTime = System.nanoTime();
    }

    public void endTimer() {
        this.endTime = System.nanoTime();
    }

    public double getLatencyInMs() {
        return (endTime - startTime) / 1000000.0;
    }

    public void recordToQueue(LinkedBlockingQueue<LatencyInfo> latencies, LatencyInfo latencyInfo) {
        latencies.add(latencyInfo);
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }
}
