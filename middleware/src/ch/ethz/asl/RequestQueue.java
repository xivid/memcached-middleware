package ch.ethz.asl;

import java.util.concurrent.LinkedBlockingQueue;


/**
 * Wrapper of BlockingQueue. For the ease of measuring the arrival rate.
 *
 * Every time an enqueue() happens, {@code numArrived} is updated. By calling {@code getArrivalRate} with a time period,
 * we can get the average arrival rate in this period, and the current {@code numArrived} is automatically saved for
 * next average arrival rate computation.
 *
 */
class RequestQueue {

    private LinkedBlockingQueue<Request> queue;

    // num of enqueued requests
    private int numArrived = 0;

    RequestQueue() {
        queue = new LinkedBlockingQueue<>();
    }

    void enqueue(Request request) throws InterruptedException {
        ++numArrived;
        request.setTimeEnqueued();
        queue.put(request);
    }

    Request dequeue() throws InterruptedException {
        Request request = queue.take();
        request.setTimeDequeued();
        return request;
    }

    int getSize() {
        return queue.size();
    }


    /**
     * Marks the {@code numArrived} value at the start of the next counting period.
     */
    private int numArrivedPrev = 0;

    private void mark() {
        numArrivedPrev = numArrived;
    }

    /**
     * Calculates the arrival rate in the current counting period
     * @return arrival rate in ops/sec
     */
    double getArrivalRate(double duration) {
        double arrivalRate = (double)(numArrived - numArrivedPrev) / duration;
        mark();
        return arrivalRate;
    }

}
