package ch.ethz.asltest;

import org.apache.logging.log4j.*;

import java.io.IOException;
import java.net.*;
import java.util.*;


/**
 * TODO: write this document
 *
 */
public class WorkerThread extends Thread {

    // Logger
    private static Logger logger = LogManager.getLogger("worker");

    // shared objects
    private final RequestQueue queue;
    private List<String> mcAddresses;
    private Statistics statistics;
    private LinkedList<String> exceptionLogs;

    // parameters
    private final boolean readSharded;

    // request processor (dependent on sharded or not)
    private RequestProcessor requestProcessor;

    // connections to servers
    private Socket[] serverSockets;


    WorkerThread(Statistics stat, RequestQueue q, List<String> mcs, boolean sharded, LinkedList<String> elogs) {
        super();

        statistics = stat;
        queue = q;
        mcAddresses = mcs;
        readSharded = sharded;
        serverSockets = new Socket[mcAddresses.size()];
        exceptionLogs = elogs;
    }


    @Override
    public void run() {

        connectToServers();

        try {
            requestProcessor = RequestProcessorFactory.createRequestProcessor(readSharded, statistics, serverSockets, logger);
            requestProcessor.initServers();
        } catch (Exception e) {
            exceptionLogs.add(String.format("[%s] %s", currentThread().getName(), e.toString()));
            System.exit(-1);
        }

        while (!Thread.interrupted()) {
            Request request;

            // for estimating the average queue length
            statistics.markQueueLength(queue.getSize());

            try {
                request = queue.dequeue();
            } catch (InterruptedException e) {
                break;
            }

            if (request != null) {
                try {
                    processRequest(request);
                } catch (SocketException e) {
                    // this happens when a socket to a memcached server is broken, in this case we terminate the middleware.
                    logger.trace("Failed processing request: [" + request.getMsg() + "], " + e);
                    exceptionLogs.add(String.format("[%s] %s, failed to process request [%s]",
                            currentThread().getName(), e.toString(), request.getMsg()));
                    System.exit(-1);
                } catch (IOException e) {
                    logger.trace("Failed processing request: [" + request.getMsg() + "], " + e);
                    exceptionLogs.add(String.format("[%s] %s, failed to process request [%s]",
                            currentThread().getName(), e.toString(), request.getMsg()));
                }
            }
        }

        // connections will be closed automatically
        logger.trace("WorkerThread terminated.");
    }


    private void connectToServers() {

        try {
            // connect to all servers and register the socket channels to readSelector (interested in READ)
            int numServers = 0;
            for (String address : mcAddresses) {
                // connect to server
                if (address.indexOf(':') > -1) {
                    String[] split = address.split(":");
                    Socket newServer = new Socket(split[0], Integer.parseInt(split[1]));

                    // add to serverSockets for write
                    serverSockets[numServers++] = newServer;

                    logger.trace("Connected to server " + newServer.getRemoteSocketAddress());
                    logger.trace("Channel added to serverSockets (numServers: " + numServers + ")");
                } else {
                    throw new IllegalArgumentException("Illegal mcAddress " + address);
                }
            }
        } catch (IOException e) {
            logger.trace("Caught IOException: " + e);
            exceptionLogs.add(String.format("[%s] %s", currentThread().getName(), e.toString()));
            System.exit(-1);
        } catch (RuntimeException e) {
            // may be an IllegalArgumentException or a NumberFormatException
            logger.trace("Caught RuntimeException: " + e);
            exceptionLogs.add(String.format("[%s] %s", currentThread().getName(), e.toString()));
            System.exit(-1);
        }
    }


    private void processRequest(Request request) throws IOException {
        logger.trace("Processing request: " + request.toString());

        switch (request.getType()) {
            case SET: {
                requestProcessor.processSet(request);
                statistics.incNumSets();
                statistics.addSetWaitingTime(request.getWaitingTime());
                statistics.addSetServiceTime(request.getServiceTime());
                statistics.addSetResponseTime(request.getResponseTime());
                statistics.setHistogram.add(request.getResponseTime());
                break;
            }
            case GET: {
                requestProcessor.processGet(request);
                statistics.incNumGets();
                statistics.addGetWaitingTime(request.getWaitingTime());
                statistics.addGetServiceTime(request.getServiceTime());
                statistics.addGetResponseTime(request.getResponseTime());
                statistics.getHistogram.add(request.getResponseTime());
                break;
            }
            case MULTIGET: {
                requestProcessor.processMultiGet(request);
                statistics.incNumMultigets();
                statistics.addNumKeysMultiget(request.getNumKeys());
                statistics.addMultigetWaitingTime(request.getWaitingTime());
                statistics.addMultigetServiceTime(request.getServiceTime());
                statistics.addMultigetResponseTime(request.getResponseTime());
                statistics.multigetHistogram.add(request.getResponseTime());
                break;
            }
            default: {
                logger.trace("Unsupported request type: " + request);
                exceptionLogs.add(String.format("[%s] Unsupported request type: %s", currentThread().getName(), request));
                break;
            }
        }

    }


}
