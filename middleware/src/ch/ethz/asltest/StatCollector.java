package ch.ethz.asltest;

import java.util.LinkedList;

public class StatCollector implements Runnable {


    // shared objects
    private Statistics[] statistics;
    private RequestQueue requestQueue;
    private LinkedList<PeriodLog> periodLogs;

    // parameters
    private long timeStarted;
    private double period; // in seconds
    private int numServers;

    /**
     *
     * @param stats statistics data from each of the worker threads
     * @param p time period for collecting stats, in milliseconds
     */
    StatCollector(Statistics[] stats, LinkedList<PeriodLog> logs, RequestQueue queue, long p, int numServers) {
        super();

        statistics = stats;
        periodLogs = logs;
        requestQueue = queue;
        period = ((double)p) / 1000;
        this.numServers = numServers;
        timeStarted = System.currentTimeMillis();
    }


    @Override
    public void run() {
        long currentSecond = (System.currentTimeMillis() - timeStarted) / 1000;

        PeriodLog periodLog = new PeriodLog(currentSecond, statistics.length, numServers);
        for (int i = 0; i < statistics.length; ++i) {
            periodLog.setThreadLog(i, statistics[i].getThreadLog(period));
        }
        periodLog.setArrivalRate(requestQueue.getArrivalRate(period));
        periodLogs.add(periodLog);
    }
}
