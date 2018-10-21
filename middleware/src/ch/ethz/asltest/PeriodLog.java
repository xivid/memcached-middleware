package ch.ethz.asltest;

class PeriodLog {
    private long logSecond;
    private ThreadLog[] threadLogs;
    private ThreadLog mergedLog;
    private double arrivalRate;
    private int numServers;
    
    PeriodLog(long currentSecond, int threads, int numServers) {
        logSecond = currentSecond;
        this.numServers = numServers;
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

        long[] numGetsPerServer = new long[numServers];
        long[] numGetShardsPerServer = new long[numServers];
        long[] numGetKeysPerServer = new long[numServers];
        
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
        
        // calculate sum Process time
        long sumSetProcess = 0;
        long sumGetProcess = 0;
        long sumMultigetProcess = 0;

        // calculate average queue length
        double avgQueueLength = 0;

        for (ThreadLog log: threadLogs) {
            long numSetsAdded = log.getNumSets();
            long numGetsAdded = log.getNumGets();
            long numMultigetsAdded = log.getNumMultigets();

            numSets += numSetsAdded;
            numGets += numGetsAdded;
            numMultigets += numMultigetsAdded;
            
            for (int i = 0; i < numServers; ++i) {
                numGetsPerServer[i] += log.getNumGetsPerServer()[i];
                numGetShardsPerServer[i] += log.getNumGetShardsPerServer()[i];
                numGetKeysPerServer[i] += log.getNumGetKeysPerServer()[i];
            }

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

            sumSetProcess += log.getSumSetProcess();
            sumGetProcess += log.getSumGetProcess();
            sumMultigetProcess += log.getSumMultigetProcess();

            avgQueueLength += log.getAvgQueueLength();
        }

        avgQueueLength /= threadLogs.length;

        mergedLog = new ThreadLog(
                numSets, tpSet, sumSetWaiting, sumSetService, sumSetResponse, sumSetProcess,
                numGets, tpGet, sumGetWaiting, sumGetService, sumGetResponse, sumGetProcess,
                numMultigets, tpMultiget, sumMultigetWaiting, sumMultigetService, sumMultigetResponse, sumMultigetProcess,
                avgQueueLength,
                numGetsPerServer, numGetShardsPerServer, numGetKeysPerServer);

        return mergedLog;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%d secs] arrival rate %.2f ops/sec\n\n", logSecond, arrivalRate));
        sb.append("PER SERVER READ STATS ").append(getMergedLog().toServerString()).append("\n");
        sb.append("ALL WORKERS STATS ").append(getMergedLog().toString()).append("\n");
        for (int i = 0; i < threadLogs.length; ++i) {
            sb.append("WORKER ").append(i).append(" STATS ").append(threadLogs[i].toString()).append("\n");
        }
        return sb.toString();
    }
}
