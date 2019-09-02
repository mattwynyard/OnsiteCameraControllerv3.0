package TCPConnection;

import java.io.*;
import java.net.*;

import Bluetooth.SPPServer;

public class TCPServer {

    private InputStream in;
    private ServerSocket server;
    private ServerSocket photoServer;
    private Socket client; //access socket
    private Socket photoClient; //access socket
    private Thread mReadThread;
    private PrintWriter writer; //writes to DB
    //private PrintWriter photoWriter; //writes to DB]
    //private OutputStream photoOut;
    private SPPServer mPhoneServer;

    /**
     * TCPServer constructor which creates a new server socket on local host port 38200 then calls start() method
     */
    public TCPServer(int port) {
        try {
            // create the main server socket
            server = new ServerSocket(port, 0, InetAddress.getByName(null));

//            photoServer = new ServerSocket(38300, 0, InetAddress.getByName(null));
//            if (photoServer != null) {
//                System.out.println("Server socket listening on: "+ photoServer.getLocalSocketAddress());
//            }
        } catch (IOException e) {
            System.out.println("Error: " + e);
            return;
        }
        start();
    }

    public TCPServer start() {


        try {
            // wait for a connection
            client = server.accept();
            //photoClient = photoServer.accept();
            System.out.println("\nAccepted TCP connection from " + client.getRemoteSocketAddress());
            //System.out.println("Accepted TCP connection from " + photoClient.getRemoteSocketAddress() + "\n");
            writer = new PrintWriter(client.getOutputStream());
            //photoOut = photoClient.getOutputStream();
            mReadThread = new Thread(readFromClient);
            mReadThread.setPriority(Thread.MAX_PRIORITY);
            mReadThread.start();

        } catch (IOException e) {
            System.out.println("Error: " + e);
            sendDataDB(e.toString());
        }
        return this;
    }

    public void setPhoneServer (SPPServer server) {
        mPhoneServer = server;
    }

    /** Sends a message to the client, in this case VBA via C# .dll
     *
     * @param message - the message to be sent.
     */
    public synchronized void sendDataDB(String message) {
        System.out.println(message);
        writer.print(message);
        writer.flush();
    }

//    /** Sends a message to the client, in this case VBA via C# .dll
//     *
//     * @param bytes - photo byte array.
//     */
//    public synchronized void sendPhotoDB(byte[] bytes) {
//
//        try {
//            //photoOut.write(bytes);
//            //photoOut.flush();
//            System.out.println("J: Photo bytes " + bytes.length);
//        } catch(IOException e) {
//            System.out.println("socket error");
//            e.printStackTrace();
//        }
//    }


    public void sendDataAndroid(String message) {

        mPhoneServer.sendCommand(message);

    }

    /**
     * Called from shutdown hookup to fail gracefully
     */
    public void closeAll() {
        try {
            writer.close();
            in.close();
            server.close();
            client.close();
            writer = null;
            mReadThread = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * To run in the background,  reads incoming data
     * from the client - access
     */
    private Runnable readFromClient = new Runnable() {

        @Override
        public void run() {
            System.out.println("Intialised TCP Read thread");
            int length;
            byte[] buffer = new byte[1024];
            try {
                in = client.getInputStream();
                while ((length = in.read(buffer)) != -1) {
                    String line = new String(buffer, 0, length);
                    System.out.println(line);
                    if (line.equals("Start")) {
                        sendDataAndroid(line);
                        //System.out.println(line);
                    } else if (line.equals("Stop")) {
                        sendDataAndroid(line);
                        //System.out.println(line);
                    } else if (line.contains("Time")){
                        sendDataAndroid(line);
                        //System.out.println(line);
                    }
                }
                in.close();

            }
            catch (IOException e1) {
                // TODO Auto-generated catch block
                //e1.printStackTrace();
                System.out.println("Socket Shutdown");
                System.exit(0);

            }
        }
    };
}

