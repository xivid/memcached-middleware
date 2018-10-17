package ch.ethz.asltest;

class PeriodLog {
    private long logSecond;
    private ThreadLog[] threadLogs;
    private ThreadLog mergedLog;
    private double arrivalRate;

    PeriodLog(long currentSecond, int threads) {
        logSecond = currentSecond;
        threadLogs = new ThreadLog[threads];
        mergedLog = null;
    }

    void setThreadLog(int tid, ThreadLog log) {
        threadLogs[tid] = log;
    }

    void setArrivalRate(double val) {
        arrivalRate = val;
    }

    /** merge all threads' logs into final (global) values
     * This method must be called after all thread logs have been collected. It doesn't do any check.
     * @return ThreadLog, merged
     */
    ThreadLog getMergedLog() {
        if (mergedLog != null) {
            return mergedLog;
        }

        // calculate number of requests added
        long numSets = 0;
        long numGets = 0;
        long numMultigets = 0;

        // calculate per-type throughput values
        long tpSet = 0;
        long tpGet = 0;
        long tpMultiget = 0;

        // calculate sum waiting time
        long sumSetWaiting = 0;
        long sumGetWaiting = 0;
        long sumMultigetWaiting = 0;

        // calculate sum service time
        long sumSetService = 0;
        long sumGetService = 0;
        long sumMultigetService = 0;

        // calculate sum response time
        long sumSetResponse = 0;
        long sumGetResponse = 0;
        long sumMultigetResponse = 0;

        // calculate average queue length
        double avgQueueLength = 0;

        long[] numGetsServer = null;
        for (ThreadLog log: threadLogs) {
            long numSetsAdded = log.getNumSets();
            long numGetsAdded = log.getNumGets();
            long numMultigetsAdded = log.getNumMultigets();

            numSets += numSetsAdded;
            numGets += numGetsAdded;
            numMultigets += numMultigetsAdded;

            tpSet += log.getTpSet();
            tpGet += log.getTpGet();
            tpMultiget += log.getTpMultiget();

            sumSetWaiting += log.getSumSetWaiting();
            sumGetWaiting += log.getSumGetWaiting();
            sumMultigetWaiting += log.getSumMultigetWaiting();

            sumSetService += log.getSumSetService();
            sumGetService += log.getSumGetService();
            sumMultigetService += log.getSumMultigetService();

            sumSetResponse += log.getSumSetResponse();
            sumGetResponse += log.getSumGetResponse();
            sumMultigetResponse += log.getSumMultigetResponse();
            
            avgQueueLength += log.getAvgQueueLength();

            if (numGetsServer == null) {
                numGetsServer = new long[log.numGetsServer.length];
            }
            for (int i = 0; i < log.numGetsServer.length; ++i) {
                numGetsServer[i] += log.numGetsServer[i];
            }
        }

        avgQueueLength /= threadLogs.length;

        mergedLog = new ThreadLog(
                numSets, tpSet, sumSetWaiting, sumSetService, sumSetResponse,
                numGets, tpGet, sumGetWaiting, sumGetService, sumGetResponse,
                numMultigets, tpMultiget, sumMultigetWaiting, sumMultigetService, sumMultigetResponse,
                avgQueueLength, numGetsServer);

        return mergedLog;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%d secs] arrival rate %.2f ops/sec\n\n", logSecond, arrivalRate));
        ThreadLog mergedLog = getMergedLog();
        sb.append("ALL WORKERS STATS ").append(mergedLog.toString());
        sb.append("Gets distribution on servers: \n");
        for (int i = 0; i < mergedLog.numGetsServer.length; ++i) {
            sb.append("Server ");
            sb.append(i);
            sb.append(": ");
            sb.append(mergedLog.numGetsServer[i]);
            sb.append("\n");
        }
        sb.append("\n");
        for (int i = 0; i < threadLogs.length; ++i) {
            sb.append("WORKER ").append(i).append(" STATS ").append(threadLogs[i].toString()).append("\n");
        }
        return sb.toString();
    }
}
