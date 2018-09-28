package ch.ethz.asl;

import org.apache.logging.log4j.*;

import java.io.IOException;
import java.net.*;


/**
 * TODO: write this document
 *
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


