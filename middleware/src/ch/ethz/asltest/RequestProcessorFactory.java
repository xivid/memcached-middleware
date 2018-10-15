package ch.ethz.asltest;

import org.apache.logging.log4j.*;
import java.net.Socket;


/**
 * TODO: write this document
 *
 */
class RequestProcessorFactory {

    static RequestProcessor createRequestProcessor(boolean readSharded, Statistics stats,
                                                   Socket[] serverSockets, Logger logger) {
        if (readSharded) {
            return new ShardedRequestProcessor(stats, serverSockets, logger);
        } else {
            return new NonShardedRequestProcessor(stats, serverSockets, logger);
        }

    }
}
