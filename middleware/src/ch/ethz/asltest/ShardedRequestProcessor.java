package ch.ethz.asltest;

import org.apache.logging.log4j.*;

import java.io.BufferedReader;
import java.net.*;
import java.io.IOException;
import java.util.Comparator;
import java.util.PriorityQueue;


/**
 * Sharded Request Processor
 *
 * Processes multigets in a sharded manner, and processes single-key gets and sets normally.
 */
public class ShardedRequestProcessor extends RequestProcessor {

    // load balancing instruments
    public class Server {
        int serverId;
        long keys;
        Server(int id, long initKeys) {
            serverId = id;
            keys = initKeys;
        }
    }
    private PriorityQueue<Server> serversLoadBalanced;
    private int[] serversSentTo;

    ShardedRequestProcessor(Statistics stats, Socket[] servers, Logger threadLogger) {
        super(stats, servers, threadLogger);
        Comparator<Server> comparator =  Comparator.comparing(o->o.keys);
        comparator = comparator.thenComparing(o->o.serverId);
        serversLoadBalanced = new PriorityQueue<Server>(servers.length, comparator);
        for (int i = 0; i < servers.length; ++i) {
            serversLoadBalanced.add(new Server(i, 0));
        }
        serversSentTo = new int[servers.length];
    }


    @Override
    public void processMultiGet(Request request) throws IOException {
        // get out the keys and calculate metadata
        String[] tokens = request.getTokens();
        int numKeys = tokens.length - 1;
        int numShards = numKeys >= serverSockets.length ? serverSockets.length : numKeys;

        // logger.trace("Multi-get numKeys: " + numKeys + ", tokens: ");
        // for (String token : tokens) {
            // logger.trace("[" + token + "]");
        // }
        // logger.trace("Dividing into " + numShards + " shards");

        // send shards to servers; large shards go first, so that they are sent to servers with lowest current load (see sendMultiGetShardRequest)
        int shardSize = numKeys / numShards;
        int remaining = numKeys % numShards;
        int pos = 1;
        for (int i = 0; i < numShards; ++i) {
            if (i < remaining) {
                serversSentTo[i] = sendMultiGetShardRequest(tokens, pos,  shardSize + 1);
                pos += shardSize + 1;
            } else {
                serversSentTo[i] = sendMultiGetShardRequest(tokens, pos, shardSize);
                pos += shardSize;
            }
        }
        request.setTimeSentToServers();

        // wait for all responses
        numMisses = numKeys;
        StringBuilder responseBuilder = new StringBuilder();
        for (int i = 0; i < numShards; ++i) {
            // receive in the same order as sending
            responseBuilder.append(readMultiGetShardResponse(serversSentTo[i]));
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
        // logger.trace("Completed request: " + request + ", response: [" + finalResponse + "]");
    }


    /**
     * send a multi-get command with tokens[fromIndex, toIndex) as keys
     * @param fromIndex starting index (inclusive) of the keys to send from tokens
     * @param size number of keys to send
     * @return server index sent to
     */
    private int sendMultiGetShardRequest(String[] tokens, int fromIndex, int size) throws IOException {
        StringBuilder requestBuilder = new StringBuilder(tokens[0]);
        for (int i = fromIndex; i < fromIndex + size; ++i) {
            requestBuilder.append(" ");
            requestBuilder.append(tokens[i]);
        }
        requestBuilder.append("\r\n");

        // select the server with current lowest load
        Server server = serversLoadBalanced.poll();
        if (server == null) {
            throw new IOException("Null Pointer Exception from serversLoadBalanced PriorityQueue!");
        }
        server.keys += size;
        serversLoadBalanced.add(server);

        // send req to server
        sendToServer(server.serverId, requestBuilder.toString());

        statistics.incNumGetsShardsServer(server.serverId);
        statistics.addGetKeysServer(server.serverId, size);
        return server.serverId;
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
        // logger.trace("Receiving multi-get shard response from server " + serverSockets[serverIndex].getRemoteSocketAddress());
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
