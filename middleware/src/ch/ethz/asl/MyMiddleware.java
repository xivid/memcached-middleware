package ch.ethz.asl;

import java.util.*;
import java.util.concurrent.*;
import org.apache.logging.log4j.*;


/**
 * TODO: write this document
 *
 */
public class MyMiddleware {

    // Logger and logs
    private static Logger logger = LogManager.getLogger("main");
    private LinkedList<PeriodLog> periodLogs;
    private LinkedList<String> exceptionLogs;

    // Configuration
    private String myIp = null;
    private int myPort = 0;
    private List<String> mcAddresses = null;
    private int numThreadsPTP = -1;
    private boolean readSharded = false;
    private static final long period = 1000;  // in milliseconds (ms)

    // System shared
    private RequestQueue queue;
    private Statistics[] workerStatistics;

    // Threads & executors
    private WorkerThread[] workerThreads;
    private NetThread netThread;
    private ScheduledExecutorService statExecutor;
    class StatThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            return new Thread(r, "StatThread");
        }
    }

    public MyMiddleware(String _myIp, int _myPort, List<String> _mcAddresses, int _numThreadsPTP, boolean _readSharded)
    {
        myIp = _myIp;
        myPort = _myPort;
        mcAddresses = _mcAddresses;
        numThreadsPTP = _numThreadsPTP;
        readSharded = _readSharded;

        queue = new RequestQueue();
        workerThreads = new WorkerThread[numThreadsPTP];
        workerStatistics = new Statistics[numThreadsPTP];
        statExecutor = Executors.newScheduledThreadPool(1, new StatThreadFactory());

        periodLogs = new LinkedList<>();
        exceptionLogs = new LinkedList<>();
    }


    public void run() {
        logger.info("Running middleware with configuration:" + getConfigStr());

        startThreads();

        waitForExit();
    }


    private String getConfigStr() {
        return "myIp: " + myIp + ", myPort: " + myPort + ", mcAddresses: " + mcAddresses +
                ", numThreadsPTP: " + numThreadsPTP + ", readSharded: " + readSharded;
    }


    private void startThreads() {
        if (numThreadsPTP <= 0) {
            logger.fatal("Num of worker threads must be a positive integer!");
            System.exit(-1);
        }

        for (int i = 0; i < numThreadsPTP; ++i) {
            workerStatistics[i] = new Statistics();
            workerThreads[i] = new WorkerThread(workerStatistics[i], queue, mcAddresses, readSharded, exceptionLogs);
            workerThreads[i].setName("Worker " + i);
            workerThreads[i].start();
        }

        netThread = new NetThread(myIp, myPort, queue, exceptionLogs);
        netThread.setName("NetThread");
        netThread.start();

        // collect all worker threads' statistics every <period> seconds
        StatCollector statCollector = new StatCollector(workerStatistics, periodLogs, queue, period);
        statExecutor.scheduleAtFixedRate(statCollector, 0, period, TimeUnit.MILLISECONDS);
    }


    /**
     * Dump stats on exiting.
     * The middleware can be terminated by a ^C
     */
    private void waitForExit() {
        // register a shutdown hook
        ExitHandler exitHandler = new ExitHandler(netThread, workerThreads, statExecutor,
                workerStatistics, periodLogs, exceptionLogs);
        exitHandler.setName("ExitHandler");
        Runtime.getRuntime().addShutdownHook(exitHandler);
    }
}
