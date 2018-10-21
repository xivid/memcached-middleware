package ch.ethz.asltest;

class ThreadLog {

    private long numSets;
    private long numGets;
    private long numMultigets;

    private long[] numGetsPerServer;
    private long[] numGetShardsPerServer;
    private long[] numGetKeysPerServer;

    private double tpSet;
    private double tpGet;
    private double tpMultiget;

    private long sumSetWaiting;
    private long sumGetWaiting;
    private long sumMultigetWaiting;

    private long sumSetService;
    private long sumGetService;
    private long sumMultigetService;

    private long sumSetResponse;
    private long sumGetResponse;
    private long sumMultigetResponse;
    
    private long sumSetProcess;
    private long sumGetProcess;
    private long sumMultigetProcess;

    private double avgQueueLength;

    ThreadLog(long numSets, double tpSet, long sumSetWaiting, long sumSetService, long sumSetResponse, long sumSetProcess,
              long numGets, double tpGet, long sumGetWaiting, long sumGetService, long sumGetResponse, long sumGetProcess,
              long numMultigets, double tpMultiget, long sumMultigetWaiting, long sumMultigetService, long sumMultigetResponse, long sumMultigetProcess,
              double avgQueueLength,
              long[] numGetsPerServer, long[] numGetShardsPerServer, long[] numGetKeysPerServer) {
        this.numSets = numSets;
        this.tpSet = tpSet;
        this.sumSetWaiting = sumSetWaiting;
        this.sumSetService = sumSetService;
        this.sumSetResponse = sumSetResponse;
        this.sumSetProcess = sumSetProcess;

        this.numGets = numGets;
        this.tpGet = tpGet;
        this.sumGetWaiting = sumGetWaiting;
        this.sumGetService = sumGetService;
        this.sumGetResponse = sumGetResponse;
        this.sumGetProcess = sumGetProcess;

        this.numMultigets = numMultigets;
        this.tpMultiget = tpMultiget;
        this.sumMultigetWaiting = sumMultigetWaiting;
        this.sumMultigetService = sumMultigetService;
        this.sumMultigetResponse = sumMultigetResponse;
        this.sumMultigetProcess = sumMultigetProcess;

        this.avgQueueLength = avgQueueLength;

        this.numGetsPerServer = numGetsPerServer;
        this.numGetShardsPerServer = numGetShardsPerServer;
        this.numGetKeysPerServer = numGetKeysPerServer;
    }


    long getNumSets() {
        return numSets;
    }

    long getNumGets() {
        return numGets;
    }

    long getNumMultigets() {
        return numMultigets;
    }

    long[] getNumGetsPerServer() {
        return numGetsPerServer;
    }

    long[] getNumGetShardsPerServer() {
        return numGetShardsPerServer;
    }

    long[] getNumGetKeysPerServer() {
        return numGetKeysPerServer;
    }

    double getTpSet() {
        return tpSet;
    }

    double getTpGet() {
        return tpGet;
    }

    double getTpMultiget() {
        return tpMultiget;
    }

    double getSumSetWaiting() {
        return sumSetWaiting;
    }

    double getSumGetWaiting() {
        return sumGetWaiting;
    }

    double getSumMultigetWaiting() {
        return sumMultigetWaiting;
    }

    double getSumSetService() {
        return sumSetService;
    }

    double getSumGetService() {
        return sumGetService;
    }

    double getSumMultigetService() {
        return sumMultigetService;
    }

    double getSumSetResponse() {
        return sumSetResponse;
    }

    double getSumGetResponse() {
        return sumGetResponse;
    }

    double getSumMultigetResponse() {
        return sumMultigetResponse;
    }

    double getSumSetProcess() {
        return sumSetProcess;
    }

    double getSumGetProcess() {
        return sumGetProcess;
    }

    double getSumMultigetProcess() {
        return sumMultigetProcess;
    }

    double getAvgQueueLength() {
        return avgQueueLength;
    }
    
    @Override
    public String toString() {
        double avgSetWaiting = numSets == 0 ? 0.0 : (1.0 * sumSetWaiting / numSets);
        double avgSetService = numSets == 0 ? 0.0 : (1.0 * sumSetService / numSets);
        double avgSetResponse = numSets == 0 ? 0.0 : (1.0 * sumSetResponse / numSets);
        double avgSetProcess = numSets == 0 ? 0.0 : (1.0 * sumSetProcess / numSets);

        double avgGetWaiting = numGets == 0 ? 0.0 : (1.0 * sumGetWaiting / numGets);
        double avgGetService = numGets == 0 ? 0.0 : (1.0 * sumGetService / numGets);
        double avgGetResponse = numGets == 0 ? 0.0 : (1.0 * sumGetResponse / numGets);
        double avgGetProcess = numSets == 0 ? 0.0 : (1.0 * sumGetProcess / numSets);

        double avgMultigetWaiting = numMultigets == 0 ? 0.0 : (1.0 * sumMultigetWaiting / numMultigets);
        double avgMultigetService = numMultigets == 0 ? 0.0 : (1.0 * sumMultigetService / numMultigets);
        double avgMultigetResponse = numMultigets == 0 ? 0.0 : (1.0 * sumMultigetResponse / numMultigets);
        double avgMultigetProcess = numSets == 0 ? 0.0 : (1.0 * sumMultigetProcess / numSets);

        long totalOps = numSets + numGets + numMultigets;
        double avgWaiting = totalOps == 0 ? 0.0 :
                (1.0 * (sumSetWaiting + sumGetWaiting + sumMultigetWaiting) / totalOps);
        double avgService = totalOps == 0 ? 0.0 :
                (1.0 * (sumSetService + sumGetService + sumMultigetService) / totalOps);
        double avgResponse = totalOps == 0 ? 0.0 :
                (1.0 * (sumSetResponse + sumGetResponse + sumMultigetResponse) / totalOps);
        double avgProcess = totalOps == 0 ? 0.0 :
                (1.0 * (sumSetProcess + sumGetProcess + sumMultigetProcess) / totalOps);

        return String.format("(avg queue length %.2f)\n" +
                "======================================================================================================================================\n" +
                "%-10s%8s%23s%23s%23s%23s%23s\n" +
                "--------------------------------------------------------------------------------------------------------------------------------------\n" +
                "%-10s%8d%23.2f%23.2f%23.2f%23.2f%23.2f\n" +
                "%-10s%8d%23.2f%23.2f%23.2f%23.2f%23.2f\n" +
                "%-10s%8d%23.2f%23.2f%23.2f%23.2f%23.2f\n" +
                "%-10s%8d%23.2f%23.2f%23.2f%23.2f%23.2f\n" +
                "======================================================================================================================================\n",
                avgQueueLength,
                "Type", "Ops", "Throughput (ops/sec)", "Avg Waiting Time (us)", "Avg Service Time (us)", "Avg Response Time (us)", "Avg Process Time (us)",
                "Sets", numSets, tpSet, avgSetWaiting, avgSetService, avgSetResponse, avgSetProcess,
                "Gets", numGets, tpGet, avgGetWaiting, avgGetService, avgGetResponse, avgGetProcess,
                "Multi-gets", numMultigets, tpMultiget, avgMultigetWaiting, avgMultigetService, avgMultigetResponse, avgMultigetProcess,
                "Totals", totalOps, tpSet + tpGet + tpMultiget, avgWaiting, avgService, avgResponse, avgProcess
        );
    }

    /**
     * Per server statistics
     * @return beautifully printable string
     */
    String toServerString() {
        StringBuilder sb = new StringBuilder(String.format("\n" +
                "==================================================\n" +
                "%-8s%14s%10s%17s\n" +
                "--------------------------------------------------\n",
                "Server", "NonSharded", "Shards", "GET&MGET keys"));

        for (int i = 0; i < numGetKeysPerServer.length; ++i) {
            sb.append(String.format("%-8d%14d%10d%17d\n",
                    i, numGetsPerServer[i], numGetShardsPerServer[i], numGetKeysPerServer[i]));
        }

        sb.append("==================================================\n");
        return sb.toString();
    }
}
