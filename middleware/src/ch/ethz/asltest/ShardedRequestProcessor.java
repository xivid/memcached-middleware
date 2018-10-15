package ch.ethz.asltest;

import org.apache.logging.log4j.*;

import java.io.BufferedReader;
import java.net.*;
import java.io.IOException;


/**
 * TODO: write this document
 *
 */
public class ShardedRequestProcessor extends RequestProcessor {

    ShardedRequestProcessor(Statistics stats, Socket[] servers, Logger threadLogger) {
        super(stats, servers, threadLogger);
    }


    @Override
    public void processMultiGet(Request request) throws IOException {
        // get out the keys and calculate metadata
        String[] tokens = request.getTokens();
        int numKeys = tokens.length - 1;
        int numShards = numKeys >= serverSockets.length ? serverSockets.length : numKeys;

        logger.trace("Multi-get numKeys: " + numKeys + ", tokens: ");
        for (String token : tokens) {
            logger.trace("[" + token + "]");
        }
        logger.trace("Dividing into " + numShards + " shards");

        // send shards to servers in a round-robin manners
        int shardSize = numKeys / numShards;
        int remaining = numKeys % numShards;
        int pos = 1;
        int serverIndex = nextServerIndex;  // send and receive from this index on
        for (int i = 0; i < numShards; ++i) {
            if (i < remaining) {
                sendMultiGetShardRequest(tokens, pos, pos + shardSize + 1);
                pos += shardSize + 1;
            } else {
                sendMultiGetShardRequest(tokens, pos, pos + shardSize);
                pos += shardSize;
            }
        }
        request.setTimeSentToServers();

        // wait for all responses
        numMisses = numKeys;
        StringBuilder responseBuilder = new StringBuilder();
        for (int i = 0; i < numShards; ++i) {
            // receive from serverIndex
            responseBuilder.append(readMultiGetShardResponse(serverIndex));
            // update receive server index
            serverIndex = (serverIndex + 1) % numServers;
        }
        responseBuilder.append("END\r\n");
        request.setTimeServersResponded();

        // send final response to the client
        String finalResponse = responseBuilder.toString();
        sendToClient(request.getChannel(), finalResponse);
        request.setTimeSentToClient();

        if (numMisses < 0) {
            throw new IOException("Received even more items than keys required!");
        }
        statistics.addNumMissesMultiget(numMisses);
        logger.trace("Completed request: " + request + ", response: [" + finalResponse + "]");
    }


    /**
     * send a multi-get command with tokens[fromIndex, toIndex) as keys
     * @param fromIndex low endpoint (inclusive) of tokens
     * @param toIndex high endpoint (exclusive) of tokens
     */
    private void sendMultiGetShardRequest(String[] tokens, int fromIndex, int toIndex) {
        StringBuilder requestBuilder = new StringBuilder(tokens[0]);
        for (int i = fromIndex; i < toIndex; ++i) {
            requestBuilder.append(" ");
            requestBuilder.append(tokens[i]);
        }
        requestBuilder.append("\r\n");

        // send req to server
        sendToServer(getAndIncrementNextServerIndex(), requestBuilder.toString());
    }


    /**
     * Similar to RequestProcessor.readGetResponse(),
     * but ignores the last "END\r\n" line,
     * and returns the StringBuilder instead of the String,
     * to make it simpler to assemble the responses for a sharded multi-get request.
     *
     * response format:
     *     0 or more items, then "END\r\n" (which is discarded in this method)
     * item format:
     *     VALUE <key> <flags> <bytes> [<cas unique>]\r\n
     *     <data block>\r\n
     * @param serverIndex index of the server to read from
     * @return StringBuilder for the response
     * @throws IOException unable to read the whole response
     */
    private StringBuilder readMultiGetShardResponse(int serverIndex) throws IOException {
        logger.trace("Receiving multi-get shard response from server " + serverSockets[serverIndex].getRemoteSocketAddress());
        BufferedReader reader = serverReaders[serverIndex];
        StringBuilder responseBuilder = new StringBuilder();
        String line;

        // read first line (can be either an "END", or the text line of an item)
        line = reader.readLine();
        if (line == null) { throw new IOException("the end of the stream has been reached"); }

        // if this line is a text line, expect an item
        while (!line.equals("END")) {
            responseBuilder.append(line);
            responseBuilder.append("\r\n");
            // parse the text line to get data block size
            String[] tokens = line.split("\\s+");
            if (tokens.length < 4 || !tokens[0].equals("VALUE")) { throw new IOException("unexpected item text line format: " + line); }
            int dataSize = Integer.valueOf(tokens[3]) + 2;

            // read data block
            while (dataSize-- > 0) {
                responseBuilder.append((char)reader.read());
            }

            // got an item, decrease the misses count
            --numMisses;

            // read next line (can be either the text line of the next item, or an "END")
            line = reader.readLine();
            if (line == null) { throw new IOException("the end of the stream has been reached"); }
        }

        return responseBuilder;
    }
}
