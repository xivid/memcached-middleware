package ch.ethz.asl;

import org.apache.logging.log4j.*;

import java.util.LinkedList;
import java.util.concurrent.ScheduledExecutorService;


/**
 * TODO: write this document
 *
 */
public class ExitHandler extends Thread {

    // Logger
    private static Logger exitLogger = LogManager.getLogger("exit");
    private static Logger statLogger = LogManager.getLogger("stat");

    // Threads
    private NetThread netThread;
    private WorkerThread[] workerThreads;
    private ScheduledExecutorService statExecutor;

    // Shared statistics
    private Statistics[] workerStatistics;
    private LinkedList<PeriodLog> periodLogs;
    private LinkedList<String> exceptionLogs;

    ExitHandler(NetThread net, WorkerThread[] workers, ScheduledExecutorService executor,
                Statistics[] stats, LinkedList<PeriodLog> plogs, LinkedList<String> elogs) {
        netThread = net;
        workerThreads = workers;
        statExecutor = executor;
        workerStatistics = stats;
        periodLogs = plogs;
        exceptionLogs = elogs;
    }

    @Override
    public void run() {
        exitLogger.info("Exiting...");
        exitLogger.info("Terminating all threads...");
        try {
            // stop net thread
            netThread.interrupt();
            netThread.join();

            // stop worker threads
            for (WorkerThread worker : workerThreads) {
                worker.interrupt();
            }
            for (WorkerThread worker : workerThreads) {
                worker.join();
            }

            // stop stat thread
            statExecutor.shutdownNow();
        } catch (InterruptedException e) {
            exitLogger.info("InterruptedException while joining threads: " + e);
        }
        exitLogger.info("Finished.");

        exitLogger.info("Dumping logs...");
        dumpFinalStats();
        exitLogger.info("Log dump finished. Good-bye!");

        LogManager.shutdown();
    }


    /**
     * First, print smaller resolution logs collected from all worker threads.
     * Then, prints the following statistics merged from all worker threads:
     *   Histogram of the response times in 100us steps
     *   Cache miss ratio
     *   Any error message or exception occurred during the experiment
     */
    private void dumpFinalStats() {
        statLogger.info("\n\n# Worker logs by 1 second period\n");
        for (PeriodLog log : periodLogs) {
            statLogger.info(log.toString());
        }

        statLogger.info("\n\n# Total stats\n");
        statLogger.info(getTotalStats());

        statLogger.info("\n\n# Response Times Histograms\n");
        Histogram setHistogram = new Histogram();
        Histogram getHistogram = new Histogram();
        Histogram multigetHistogram = new Histogram();

        for (Statistics stat: workerStatistics) {
            setHistogram.merge(stat.setHistogram);
            getHistogram.merge(stat.getHistogram);
            multigetHistogram.merge(stat.multigetHistogram);
        }

        statLogger.info("SET Histogram");
        statLogger.info(setHistogram.finalString());
        statLogger.info("GET Histogram");
        statLogger.info(getHistogram.finalString());
        statLogger.info("MULTI-GET Histogram");
        statLogger.info(multigetHistogram.finalString());

        statLogger.info("\n\n# Errors and exceptions\n");
        if (exceptionLogs.size() == 0) {
            statLogger.info("None.\n\n");
        } else {
            for (String ex : exceptionLogs) {
                statLogger.info(ex);
            }
            statLogger.info("\n");
        }
    }

    private String getTotalStats() {
        long numSets = 0;
        long numGets = 0;
        long numMultigets = 0;
        long numOps;

        long sumSetResponse = 0;
        long sumGetResponse = 0;
        long sumMultigetResponse = 0;

        double avgSetResponse;
        double avgGetResponse;
        double avgMultigetResponse;
        double avgResponse;

        double maxTpSet = Double.MIN_VALUE;
        double maxTpGet = Double.MIN_VALUE;
        double maxTpMultiget = Double.MIN_VALUE;
        double maxTp = Double.MIN_VALUE;

        double avgSizeGet;
        double avgSizeMultiget;
        double avgSizeTotal;
        long numGetHits;
        long numMultigetHits;
        long numTotalHits;
        long numGetMisses = 0;
        long numMultigetMisses = 0;
        long numMultigetKeys = 0;
        long numTotalMisses;
        double getMissRatio;
        double multigetMissRatio;
        double totalMissRatio;

        for (PeriodLog log : periodLogs) {
            ThreadLog mergedLog = log.getMergedLog();

            numSets += mergedLog.getNumSets();
            numGets += mergedLog.getNumGets();
            numMultigets += mergedLog.getNumMultigets();

            sumSetResponse += mergedLog.getSumSetResponse();
            sumGetResponse += mergedLog.getSumGetResponse();
            sumMultigetResponse += mergedLog.getSumMultigetResponse();

            maxTpSet = Math.max(mergedLog.getTpSet(), maxTpSet);
            maxTpGet = Math.max(mergedLog.getTpGet(), maxTpGet);
            maxTpMultiget = Math.max(mergedLog.getTpMultiget(), maxTpMultiget);
            maxTp = Math.max(mergedLog.getTpSet() + mergedLog.getTpGet() + mergedLog.getTpMultiget(), maxTp);
        }

        numOps = numSets + numGets + numMultigets;

        avgSetResponse = numSets == 0 ? 0 : ((double)sumSetResponse) / numSets;
        avgGetResponse = numGets == 0 ? 0 : ((double)sumGetResponse) / numGets;
        avgMultigetResponse = numMultigets == 0 ? 0 : ((double)sumMultigetResponse) / numMultigets;
        avgResponse = numOps == 0 ? 0 : ((double)(sumSetResponse + sumGetResponse + sumMultigetResponse)) / numOps;

        for (Statistics stat : workerStatistics) {
            numMultigetKeys += stat.getNumKeysMultiget();
            numGetMisses += stat.getNumMissesGet();
            numMultigetMisses += stat.getNumMissesMultiget();
        }

        avgSizeGet = (numGets > 0) ? 1 : 0;
        avgSizeMultiget = numMultigets > 0 ? ((double)numMultigetKeys) / numMultigets : 0;
        avgSizeTotal = (numMultigets + numGets > 0) ? ((double)(numMultigetKeys + numGets)) / (numMultigets + numGets) : 0;
        numMultigetHits = numMultigetKeys - numMultigetMisses;
        numGetHits = numGets - numGetMisses;
        numTotalHits = numMultigetHits + numGetHits;
        numTotalMisses = numGetMisses + numMultigetMisses;

        getMissRatio = numGets > 0 ? ((double)numGetMisses) / numGets * 100 : 0;
        multigetMissRatio = numMultigetKeys > 0 ? ((double)numMultigetMisses) / numMultigetKeys * 100 : 0;
        totalMissRatio = (numTotalHits + numTotalMisses > 0) ? ((double)numTotalMisses) / (numTotalHits + numTotalMisses) * 100 : 0;

        return String.format(
            "=====================================================================================================================\n" +
            "%-10s%11s%25s%20s%15s%11s%11s%13s\n" +
            "---------------------------------------------------------------------------------------------------------------------\n" +
            "%-10s%11d%25.2f%20.2f%15s%11s%11s%13s\n" +
            "%-10s%11d%25.2f%20.2f%15.2f%11d%11d%12.2f%%\n" +
            "%-10s%11d%25.2f%20.2f%15.2f%11d%11d%12.2f%%\n" +
            "%-10s%11d%25.2f%20.2f%15.2f%11d%11d%12.2f%%\n" +
            "=====================================================================================================================\n",
            "Type",       "Ops",        "Avg Response Time (us)", "Max. TP (ops/sec)", "Avg Get Size",  "Hits",          "Misses",          "Miss Ratio",
            "Sets",       numSets,      avgSetResponse,      maxTpSet,            "---",           "---",           "---",             "---",
            "Gets",       numGets,      avgGetResponse,      maxTpGet,            avgSizeGet,      numGetHits,      numGetMisses,      getMissRatio,
            "Multi-gets", numMultigets, avgMultigetResponse, maxTpMultiget,       avgSizeMultiget, numMultigetHits, numMultigetMisses, multigetMissRatio,
            "Totals",     numOps,       avgResponse,         maxTp,               avgSizeTotal,    numTotalHits,    numTotalMisses,    totalMissRatio
        );
    }
}
