package ch.ethz.asl;
import org.apache.logging.log4j.*;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static java.lang.Math.min;


/**
 * TODO: write this document
 *
 */
abstract class RequestProcessor {

    // shared objects
    Logger logger;
    final Socket[] serverSockets;
    Statistics statistics;

    int numServers;
    int nextServerIndex;

    BufferedReader[] serverReaders;  // read responses from servers with BufferedReaders for input streams
    private PrintWriter[] serverWriters;  // send requests to servers with serverWriters for output streams

    private ByteBuffer clientSendBuffer;  // send responses to clients with clientSendBuffer

    private String[] serverSetResponses;

    int numMisses;  // number of missed keys, shared among methods when processing a get/multi-get

    private static final int MAX_RESPONSE_SIZE = 50000;  // max value size: 4096B, max #values in a response: 10

    RequestProcessor(Statistics stats, Socket[] servers, Logger threadLogger) {
        statistics = stats;
        logger = threadLogger;
        serverSockets = servers;

        // for round-robin
        numServers = serverSockets.length;
        nextServerIndex = 0;

        // for socket io
        serverReaders = new BufferedReader[numServers];
        serverWriters = new PrintWriter[numServers];

        clientSendBuffer = ByteBuffer.allocate(MAX_RESPONSE_SIZE);

        serverSetResponses = new String[serverSockets.length];
    }


    void initServers() throws IOException {
        for (int i = 0; i < serverReaders.length; ++i) {
            try {
                serverReaders[i] = new BufferedReader(new InputStreamReader(serverSockets[i].getInputStream()));
            } catch (IOException e) {
                logger.trace("Unable to create a new BufferedReader for server: " + serverSockets[i].getRemoteSocketAddress());
                throw new IOException("Unable to create a new BufferedReader for server: " + serverSockets[i].getRemoteSocketAddress());
            }
        }

        for (int i = 0; i < serverWriters.length; ++i) {
            try {
                serverWriters[i] = new PrintWriter(serverSockets[i].getOutputStream(), true);
            } catch (IOException e) {
                logger.trace("Unable to create a new BufferedReader for server: " + serverSockets[i].getRemoteSocketAddress());
                throw new IOException("Unable to create a new BufferedReader for server: " + serverSockets[i].getRemoteSocketAddress());
            }
        }
    }

    /**
     * Round-robin on GET command's processing server
     * @return index of next server to send to
     */
    int getAndIncrementNextServerIndex() {
        int ret = nextServerIndex;
        nextServerIndex = (nextServerIndex + 1) % numServers;
        return ret;
    }

    void processSet(Request request) throws IOException {
        // send request to all servers reusing the buffer
        for (int i = 0; i < serverSockets.length; ++i) {
            sendToServer(i, request.getMsg());
        }
        request.setTimeSentToServers();

        // wait for response
        for (int i = 0; i < serverSockets.length; ++i) {
            // get server attached buffer
            logger.trace("Receiving from server " + serverSockets[i].getRemoteSocketAddress());
            String response = serverReaders[i].readLine();
            if (response == null) {
                throw new SocketException("Server" + serverSockets[i].getRemoteSocketAddress() + " closed our connection.");
            }
            serverSetResponses[i] = response;
        }
        request.setTimeServersResponded();

        // parse response to see if an error has occurred, and decide the final response to client
        String finalResponse = serverSetResponses[0];
        for (String response : serverSetResponses) {
            if (!response.equals("STORED")) {
                /* Response may be: "STORED\r\n" "NOT_STORED\r\n" "EXISTS\r\n" "NOT_FOUND\r\n" */
                finalResponse = response;
                break;
            }
        }
        finalResponse += "\r\n";

        // send response to client
        sendToClient(request.getChannel(), finalResponse);
        request.setTimeSentToClient();

        logger.trace("Completed request: " + request + ", response: [" + finalResponse + "]");
    }



    void processGet(Request request) throws IOException {
        processGetImpl(request);
        if (numMisses > 0) {
            statistics.incNumMissesGet();
        }
    }


    abstract void processMultiGet(Request request) throws IOException;


    void processGetImpl(Request request) throws IOException {
        int serverIndex = getAndIncrementNextServerIndex();

        // send req to server
        sendToServer(serverIndex, request.getMsg());
        request.setTimeSentToServers();

        // wait for response
        numMisses = request.getNumKeys();
        String finalResponse = readGetResponse(serverIndex);
        request.setTimeServersResponded();

        // send response to client
        sendToClient(request.getChannel(), finalResponse);
        request.setTimeSentToClient();

        logger.trace("Completed request: " + request + ", response: [" + finalResponse + "]");
    }


    /* ---------------------- Utilities ---------------------- */

    /**
     * response format:
     *     0 or more items, then "END\r\n"
     * item format:
     *     VALUE <key> <flags> <bytes> [<cas unique>]\r\n
     *     <data block>\r\n
     * @param serverIndex index of the server to read from
     * @return Response String
     * @throws IOException unable to read the whole response
     */
    private String readGetResponse(int serverIndex) throws IOException {
        logger.trace("Receiving response from server " + serverSockets[serverIndex].getRemoteSocketAddress());
        BufferedReader reader = serverReaders[serverIndex];
        String line;
        StringBuilder responseBuilder = new StringBuilder();

        // read first line (can be either an "END", or the text line of an item)
        line = reader.readLine();
        if (line == null) {
            throw new SocketException("Server" + serverSockets[serverIndex].getRemoteSocketAddress() + " closed our connection.");
        }
        responseBuilder.append(line);
        responseBuilder.append("\r\n");

        // if this line is a text line, expect an item
        while (!line.equals("END")) {
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
            if (line == null) {
                throw new SocketException("Server" + serverSockets[serverIndex].getRemoteSocketAddress() + " closed our connection.");
            }
            responseBuilder.append(line);
            responseBuilder.append("\r\n");
        }

        return responseBuilder.toString();
    }


    void sendToServer(int serverIndex, String msg) {
        logger.trace("Sending request to server " + serverSockets[serverIndex].getRemoteSocketAddress());
        serverWriters[serverIndex].print(msg);
        serverWriters[serverIndex].flush();
    }


    void sendToClient(SocketChannel client, String msg) throws IOException {
        logger.trace("Sending to client " + client.getRemoteAddress());
        byte[] msgBytes = msg.getBytes();
        int msgLength = msgBytes.length;
        int msgRemaining = msgLength;

        int bufCapacity = clientSendBuffer.capacity();
        int bytesToWrite;
        while (msgRemaining > 0) {
            bytesToWrite = min(bufCapacity, msgRemaining);
            clientSendBuffer.clear();
            clientSendBuffer.put(msgBytes, msgLength - msgRemaining, bytesToWrite);
            clientSendBuffer.flip();
            while (clientSendBuffer.hasRemaining()) {
                client.write(clientSendBuffer);
            }
            msgRemaining -= bytesToWrite;
        }
    }
}
