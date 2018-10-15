package ch.ethz.asltest;

import org.apache.logging.log4j.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * TODO: write this document
 *
 */
public class NetThread extends Thread {

    // Logger
    private static Logger logger = LogManager.getLogger("net");

    // Config
    private String myIp;
    private int myPort;
    private static final int MAX_REQUEST_SIZE = 5000;  // max key size: 250B, max value size: 4096B, max #keys in a request: 10

    // System-wide shared data
    private final RequestQueue requestQueue;
    private LinkedList<String> exceptionLogs;

    NetThread(String ip, int port, RequestQueue requests, LinkedList<String> elogs) {
        super();

        myIp = ip;
        myPort = port;
        requestQueue = requests;
        exceptionLogs = elogs;
    }


    @Override
    public void run() {
        logger.trace("NetThread instance starting on " + myIp + ":" + myPort + ".");

        try (
            Selector selector = Selector.open();
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()
        ) {
            // bind to port
            serverSocketChannel.socket().bind(new InetSocketAddress(myIp, myPort));
            logger.trace("NetThread listening on " + myIp + ":" + myPort);

            // register the serverSocketChannel
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            // wait for any interested ready event
            while (!currentThread().isInterrupted()) {
                selectChannels(selector);
            }

        } catch (IOException e) {
            logger.trace("Caught an IOException: " + e);
            exceptionLogs.add(String.format("[%s] %s", currentThread().getName(), e.toString()));
            System.exit(-1);
        } catch (InterruptedException e) {
            logger.trace("Interrupted");
            exceptionLogs.add(String.format("[%s] %s", currentThread().getName(), e.toString()));
        }

        // connections will be closed automatically
        logger.trace("NetThread terminated.");
    }


    private void selectChannels(Selector selector) throws IOException, InterruptedException {
        logger.trace("selecting from clients READ or new client ACCEPT...");
        selector.select();

        logger.trace("processing selected keys");
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

        while(keyIterator.hasNext()) {
            SelectionKey key = keyIterator.next();
            if(key.isAcceptable()) {
                // accept a new client connection and register with selector
                logger.trace("Got a new incoming connection.");

                ServerSocketChannel server = (ServerSocketChannel)(key.channel());
                SocketChannel client = server.accept();
                if (client == null) {
                    throw new IOException("Cannot accept the incoming connection!");
                }

                client.configureBlocking(false);
                SelectionKey newKey = client.register(selector, SelectionKey.OP_READ);
                newKey.attach(ByteBuffer.allocate(MAX_REQUEST_SIZE));

                logger.trace("Accepted a connection from " + client.getRemoteAddress());
            } else if (key.isReadable()) {
                // a channel is ready for reading
                logger.trace("Received a message.");

                SocketChannel client = (SocketChannel)key.channel();
                boolean status;
                try {
                    status = handleRead(client, (ByteBuffer)key.attachment());
                } catch (IOException e) {
                    key.cancel();
                    client.close();
                    logger.trace("Caught an IOException: [" + e + "] in handleRead(), de-registered this channel.");
                    exceptionLogs.add(String.format("[%s] %s in handleRead(), de-registered this channel.", currentThread().getName(), e.toString()));
                    continue;
                }

                if (!status) {
                    logger.trace("Client " + client.getRemoteAddress() + " closed connection.");
                    key.cancel();
                    client.close();
                } else {
                    logger.trace("Received request(s) from " + client.getRemoteAddress());
                }
            }

            keyIterator.remove();
        }
    }


    private boolean handleRead(SocketChannel channel, ByteBuffer buffer) throws IOException, InterruptedException {
        int bytesRead = channel.read(buffer);
        if (bytesRead == -1) {
            // connection closed by client
            return false;
        }

        int numRequests = extractRequests(channel, buffer);
        logger.trace("Extracted " + numRequests + " requests");

        return true;
    }


    private int extractRequests(SocketChannel channel, ByteBuffer buffer) throws IOException, InterruptedException {
        int numRequests = 0;

        while (true) {
            buffer.flip();

            // convert to String
            String msg = StandardCharsets.US_ASCII.decode(buffer).toString();

            // check for first line
            int eolIndex = msg.indexOf("\r\n");
            if (eolIndex < 0) {
                // not found
                break;
            }

            int expectedTotalSize, numKeys = 0;
            Request.RequestType type = Request.RequestType.OTHERS;

            // split first line by space
            // we only need to know the fifth value (SET data block size) or the number of keys (for GET/MULTIGET)
            String[] splits = msg.substring(0, eolIndex).split("\\s+");
            String[] tokens = null;

            // get data block for storage commands
            if (isStorageCommand(msg)) {
                if (splits.length < 5) {
                    throw new IOException("Wrong storage command format! msg:" + msg);
                }
                if (isSetCommand(msg)) {
                    type = Request.RequestType.SET;
                    numKeys = 1;
                }
                // (size of command line + sizeof("\r\n") + size of data block + sizeof("\r\n"))
                expectedTotalSize = eolIndex + 2 + Integer.parseInt(splits[4]) + 2;
            } else {
                // for retrieval commands or other commands
                if (isGetCommand(msg)){
                    if(splits.length > 2) {
                        type = Request.RequestType.MULTIGET;
                        numKeys = splits.length - 1;
                        tokens = splits;
                    } else {
                        type = Request.RequestType.GET;
                        numKeys = 1;
                    }
                }
                expectedTotalSize = eolIndex + 2;
            }

            // check if we have received enough bytes
            if (msg.length() < expectedTotalSize) {
                buffer.limit(buffer.capacity());
                break;
            }

            ++numRequests;
            String requestMsg = msg.substring(0, expectedTotalSize);
            // We don't check if requestMsg ends with "\r\n".
            // If not, the server will return an error, and we relay the error.
            Request request = new Request(requestMsg, type, numKeys, tokens, channel);
            enqueueRequest(request);

            if (msg.length() == expectedTotalSize) {
                buffer.clear();
                break;
            } else {
                // incomplete next request
                buffer.position(expectedTotalSize);
                buffer.compact();
            }
        }

        return numRequests;
    }


    /** Check if the type of the request indicates a two-line message
     * @param requestMsg request message content
     * @return true for two-line type, otherwise false
     */
    private static boolean isStorageCommand(String requestMsg) {
        return requestMsg.startsWith("set ") ||
                requestMsg.startsWith("add ") ||
                requestMsg.startsWith("replace ") ||
                requestMsg.startsWith("append ") ||
                requestMsg.startsWith("prepend ") ||
                requestMsg.startsWith("cas ");
    }


    private static boolean isGetCommand(String requestMsg) {
        return requestMsg.startsWith("get ") || requestMsg.startsWith("gets ");
    }


    private static boolean isSetCommand(String requestMsg) {
        return requestMsg.startsWith("set ");
    }


    private void enqueueRequest(Request request) throws InterruptedException {
        requestQueue.enqueue(request);
        logger.trace("Put in requestQueue: " + request);
    }
}
