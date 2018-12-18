package ch.ethz.asltest;

import org.apache.logging.log4j.*;

import java.io.IOException;
import java.net.*;


/**
 * NonSharded Request Processor
 *
 * Processes single-key and multi-key gets and sets in the normal (non-sharded) way.
 */
public class NonShardedRequestProcessor extends RequestProcessor {


    NonShardedRequestProcessor(Statistics statistics, Socket[] servers, Logger threadLogger) {
        super(statistics, servers, threadLogger);
    }


    @Override
    public void processMultiGet(Request request) throws IOException {
        processGetImpl(request);
        statistics.addNumMissesMultiget(numMisses);
    }

}


