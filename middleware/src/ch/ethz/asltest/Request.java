package ch.ethz.asltest;

import java.nio.channels.*;


/**
 * TODO: write this document
 *
 */
public class Request {

    public enum RequestType {
        SET,
        GET,
        MULTIGET,
        OTHERS
    }

    // ------ Request content and connection to the client ------
    private String msg;
    private RequestType type;
    private int numKeys;
    private String[] tokens;  // only used for multi-get ops
    private SocketChannel channel;

    // ------ Measuring instruments ------
    /** relative microsecond (us) the request *completely* arrives at the middleware (i.e. the last socket message becomes readable) */
    private long timeArrived;

    /** relative microsecond (us) the request has been enqueued */
    private long timeEnqueued;

    /** relative microsecond (us) the request has been dequeued by a worker thread */
    private long timeDequeued;

    /** relative microsecond (us) the request has been parsed and sent to (all) server(s) */
    private long timeSentToServers;

    /** relative microsecond (us) the worker has received (all) response(s) from (all) server(s) */
    private long timeServersResponded;

    /** relative microsecond (us) the middleware has assembled and sent the response to client */
    private long timeSentToClient;

    Request(String m, RequestType t, int k, String[] s, SocketChannel c, long arrivalTime) {
        msg = m;
        type = t;
        numKeys = k;
        tokens = s;
        channel = c;
        timeArrived = arrivalTime;
    }

    String getMsg()
    {
        return msg;
    }

    RequestType getType() {
        return type;
    }

    int getNumKeys() {
        return numKeys;
    }

    String[] getTokens() {
        return tokens;
    }

    SocketChannel getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(msg = [").append(msg).append("], type: ").append(type)
                .append(", numKeys: ").append(numKeys).append(", channel: ");

        try {
            sb.append(channel.getRemoteAddress().toString());
        } catch (Exception e) {
            sb.append("error");
        }

        sb.append(", timeArrived: ").append(timeArrived)
                .append(", timeEnqueued: ").append(timeEnqueued).append(", timeDequeued: ").append(timeDequeued)
                .append(", timeSentToServers: ").append(timeSentToServers)
                .append(", timeServersResponded: ").append(timeServersResponded)
                .append(", timeSentToClient: ").append(timeSentToClient)
                .append(")");

        return sb.toString();
    }

    void setTimeArrived(long arrivalTime) {
        timeArrived = arrivalTime;
    }

    void setTimeEnqueued() {
        timeEnqueued = System.nanoTime() / 1000;
    }

    void setTimeDequeued() {
        timeDequeued = System.nanoTime() / 1000;
    }

    void setTimeSentToServers() {
        timeSentToServers = System.nanoTime() / 1000;
    }

    void setTimeServersResponded() {
        timeServersResponded = System.nanoTime() / 1000;
    }

    void setTimeSentToClient() {
        timeSentToClient = System.nanoTime() / 1000;
    }


    /**
     * waiting time in the middleware's queue
     * @return waiting time in the middleware's queue
     */
    long getWaitingTime() {
        return timeDequeued - timeEnqueued;
    }


    /**
     * service time of memcached server for this request, in microseconds.
     * @return service time of memcached server for this request, in microseconds.
     */
    long getServiceTime() {
        return timeServersResponded - timeSentToServers;
    }


    /**
     * Response time observed at middleware, i.e. time left middleware - time arrived at middleware
     * @return Response time observed at middleware
     */
    long getResponseTime() {
        return timeSentToClient - timeArrived;
    }

    /**
     * Internal processing time, e.g. request parsing, response assembling, ...
     * @return time spent in the internal processing of the middleware, excluding waiting time and service time
     */
    long getProcessingTime() {
        return getResponseTime() - getWaitingTime() - getServiceTime();
    }

}
