package ch.ethz.asl;


/**
 * Per-thread statistics data.
 * Every worker thread holds a instance and update the numbers in it.
 * The stat thread grabs the statistics instances of all threads every a few seconds (at most 5 seconds).
 */
class Statistics {

    Statistics() {
        setHistogram = new Histogram();
        getHistogram = new Histogram();
        multigetHistogram = new Histogram();

        timeLastDequeue = System.nanoTime() / 1000;  // in us
    }

    // histograms for all three types respectively
    Histogram setHistogram;
    Histogram getHistogram;
    Histogram multigetHistogram;

    // number of operations of each type separately
    private long numSets = 0;
    private long numSetsPrev = 0;
    private long numGets = 0;
    private long numGetsPrev = 0;
    private long numMultigets = 0;
    private long numMultigetsPrev = 0;

    void incNumSets() {
        ++numSets;
    }

    void incNumGets() {
        ++numGets;
    }

    void incNumMultigets() {
        ++numMultigets;
    }

    // sum of all requests' waiting time (us) in queue by type
    private long sumSetWaitingTime = 0;
    private long sumSetWaitingTimePrev = 0;
    private long sumGetWaitingTime = 0;
    private long sumGetWaitingTimePrev = 0;
    private long sumMultigetWaitingTime = 0;
    private long sumMultigetWaitingTimePrev = 0;

    void addSetWaitingTime(long t) {
        sumSetWaitingTime += t;
    }

    void addGetWaitingTime(long t) {
        sumGetWaitingTime += t;
    }

    void addMultigetWaitingTime(long t) {
        sumMultigetWaitingTime += t;
    }

    // sum of all requests' service time (us) of memcached servers by type
    private long sumSetServiceTime = 0;
    private long sumSetServiceTimePrev = 0;
    private long sumGetServiceTime = 0;
    private long sumGetServiceTimePrev = 0;
    private long sumMultigetServiceTime = 0;
    private long sumMultigetServiceTimePrev = 0;
    
    void addSetServiceTime(long t) {
        sumSetServiceTime += t;
    }
    
    void addGetServiceTime(long t) {
        sumGetServiceTime += t;
    }
    
    void addMultigetServiceTime(long t) {
        sumMultigetServiceTime += t;
    }

    // sum of all requests' response time (us) of middleware by type
    private long sumSetResponseTime = 0;
    private long sumSetResponseTimePrev = 0;
    private long sumGetResponseTime = 0;
    private long sumGetResponseTimePrev = 0;
    private long sumMultigetResponseTime = 0;
    private long sumMultigetResponseTimePrev = 0;

    void addSetResponseTime(long t) {
        sumSetResponseTime += t;
    }

    void addGetResponseTime(long t) {
        sumGetResponseTime += t;
    }

    void addMultigetResponseTime(long t) {
        sumMultigetResponseTime += t;
    }
    
    // total number of keys and cache misses for get/multi-get ops (counted by key, not by op)
    private long numKeysMultiget = 0;
    private long numMissesGet = 0;
    private long numMissesMultiget = 0;

    long getNumMissesGet() {
        return numMissesGet;
    }

    void incNumMissesGet() {
        ++numMissesGet;
    }

    long getNumKeysMultiget() {
        return numKeysMultiget;
    }

    void addNumKeysMultiget(long val) {
        numKeysMultiget += val;
    }

    long getNumMissesMultiget() {
        return numMissesMultiget;
    }

    void addNumMissesMultiget(long val) {
        numMissesMultiget += val;
    }

    // queue length aggregated
    private long sumLengthTime = 0;
    private long sumLengthTimePrev = 0;
    private long timeLastDequeue;

    void markQueueLength(int length) {
        long now = System.nanoTime() / 1000;
        sumLengthTime += length * (now - timeLastDequeue);
        timeLastDequeue = now;
    }

    private void mark() {
        numSetsPrev = numSets;
        numGetsPrev = numGets;
        numMultigetsPrev = numMultigets;

        sumSetWaitingTimePrev = sumSetWaitingTime;
        sumGetWaitingTimePrev = sumGetWaitingTime;
        sumMultigetWaitingTimePrev = sumMultigetWaitingTime;

        sumSetServiceTimePrev = sumSetServiceTime;
        sumGetServiceTimePrev = sumGetServiceTime;
        sumMultigetServiceTimePrev = sumMultigetServiceTime;
        
        sumSetResponseTimePrev = sumSetResponseTime;
        sumGetResponseTimePrev = sumGetResponseTime;
        sumMultigetResponseTimePrev = sumMultigetResponseTime;

        sumLengthTimePrev = sumLengthTime;
    }

    /**
     * calculate based on current values and previously mark()'ed values
     * @param duration time length of this period, in seconds
     * @return calculated results, packed in a ThreadLog
     */
    ThreadLog getThreadLog(double duration) {
        // calculate number of requests added
        long numSetsAdded = numSets - numSetsPrev;
        long numGetsAdded = numGets - numGetsPrev;
        long numMultigetsAdded = numMultigets - numMultigetsPrev;

        // calculate per-type throughput values
        double tpSet = (duration > 0) ? (numSetsAdded / duration) : 0;
        double tpGet = (duration > 0) ? (numGetsAdded / duration) : 0;
        double tpMultiget = (duration > 0) ? (numMultigetsAdded / duration) : 0;

        // calculate waiting time added
        long sumSetWaiting = sumSetWaitingTime - sumSetWaitingTimePrev;
        long sumGetWaiting = sumGetWaitingTime - sumGetWaitingTimePrev;
        long sumMultigetWaiting = sumMultigetWaitingTime - sumMultigetWaitingTimePrev;

        // calculate service time added
        long sumSetService = sumSetServiceTime - sumSetServiceTimePrev;
        long sumGetService = sumGetServiceTime - sumGetServiceTimePrev;
        long sumMultigetService = sumMultigetServiceTime - sumMultigetServiceTimePrev;

        // calculate response time added
        long sumSetResponse = sumSetResponseTime - sumSetResponseTimePrev;
        long sumGetResponse = sumGetResponseTime - sumGetResponseTimePrev;
        long sumMultigetResponse = sumMultigetResponseTime - sumMultigetResponseTimePrev;
        
        // calculate average queue length
        double avgQueueLength = (duration > 0) ? (double)(sumLengthTime - sumLengthTimePrev) / 1000000 / duration : 0.0;

        mark();

        return new ThreadLog(
                numSetsAdded,      tpSet,      sumSetWaiting,      sumSetService,      sumSetResponse,
                numGetsAdded,      tpGet,      sumGetWaiting,      sumGetService,      sumGetResponse,
                numMultigetsAdded, tpMultiget, sumMultigetWaiting, sumMultigetService, sumMultigetResponse,
                avgQueueLength);
    }
}
