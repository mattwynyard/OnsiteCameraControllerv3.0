package Bluetooth;

import TCPConnection.CameraApp;
import TCPConnection.TCPServer;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;

import javax.bluetooth.*;
import javax.microedition.io.*;


/**
 * Class that implements an SPP Server which accepts single line of
 * message from an SPP client and sends a single line of response to the client.
 */
public class SPPServer extends Thread {

    private LocalDevice mLocalDevice;
    private StreamConnection connection;
    private ArrayList<Client> threadPool;
    private ArrayList<PrintWriter> writerPool;
    private OutputStream out; //Android out
    private PrintWriter writer; //Android writer
    private TCPServer mTCP;

    public SPPServer() {

    }

    public void setTCPServer(TCPServer server) {
        mTCP = server;
    }

    //start server
    public void run() {

        try {
            //System.out.println(LocalDevice.getProperty("bluetooth.l2cap.receiveMTU.max"));
            LocalDevice.getLocalDevice().setDiscoverable(DiscoveryAgent.GIAC);
            //Create a UUID for SPP
            UUID uuid = new UUID("0003000000001000800000805F9B34FB", false);
            //Create the servicve url
            String connectionString = "btspp://localhost:" + uuid + ";name=OnsiteBTServer";

            //open server url
            StreamConnectionNotifier serverConnection = (StreamConnectionNotifier) Connector.open(connectionString);

            System.out.println("\nServer Started. Waiting for clients to connect...");
            threadPool = new ArrayList();
            writerPool = new ArrayList();
            int count = 0;
            while (count < CameraApp.cameras) {
                connection = serverConnection.acceptAndOpen();
                out = connection.openOutputStream();
                writer = new PrintWriter(new OutputStreamWriter(out));
                writerPool.add(writer);

                System.out.println("Received SPP connection " + (++count) + " of " + CameraApp.cameras);
                RemoteDevice device = RemoteDevice.getRemoteDevice(connection);
                System.out.println("Remote device address: " + device.getBluetoothAddress());
                String deviceName = device.getFriendlyName(true);
                System.out.println("Remote device name: " + deviceName);
                //Runnable r = new ReadFromClient(deviceName);
                Client client = new Client(deviceName, connection, mTCP);
                Thread t = new Thread(client);

                t.start();
                threadPool.add(client);
                sendCommand("ACK");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        //sendCommand("ACK");
        //threadPool.get(0).
    }

    public ArrayList<Client> getThreadPool() {
        return threadPool;
    }


    //TODO move to client class
    /**
     * Sends command to Android phone via bluetooth connection
     *
     * @param command - the command to send
     */
    public void sendCommand(String command) {
        for (PrintWriter writer : writerPool) {
            writer.println(command);
            writer.flush();
        }
        try {
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] clearBuffer() {
        return new byte[1024];
    }

    private int decodeInteger(byte[] buffer, int offset) {
        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        temp.write(buffer, offset, 4);
        int value = new BigInteger(temp.toByteArray()).intValue();
        try {
            temp.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return value;

    }

    private String decodeString(byte[] buffer, int offset, int length) {
        String message = null;
        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        temp.write(buffer, offset, length);
        try {
            message = new String(temp.toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            temp.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return message;
    }

//    private class ReadFromClient implements Runnable {
//
//        String camera;
//        RemoteDevice device;
//        String deviceName;
//        String macAddress;
//        InputStream in; //input stream from android
//        ByteArrayOutputStream mMessageOut;
//        ByteArrayOutputStream mPhotoOut;
//        ByteArrayOutputStream byteBuffer;
//        String recording;
//        int battery;
//        int error;
//        String message;
//        String messageSend;
//
//        public ReadFromClient(Object device) {
//            this.deviceName = (String)device;
//            //macAddress = this.device.getBluetoothAddress();
//
//        }
//        //private Runnable readFromClient = new Runnable() {
//
//        @Override
//        public void run() {
//            System.out.println("Reading From Client: ");
//            mMessageOut = new ByteArrayOutputStream();
//            mPhotoOut = new ByteArrayOutputStream();
//            byte[] buffer = new byte[1024];
//            byteBuffer = new ByteArrayOutputStream();
//            int len;
//
//            String photoName;
//            boolean metadata = true;
//            int payloadSize = 0;
//            int messageSize = 0;
//            int photoSize = 0;
//            try {
//                in = connection.openInputStream();
//                while ((len = in.read(buffer)) != -1) {
//                    byteBuffer.write(buffer, 0, len); //read in total buffer from socket
//
//                    if (metadata == true) {
//                        payloadSize = decodeInteger(Arrays.copyOfRange(byteBuffer.toByteArray(), 0, 4), 0);
//                        System.out.println("payload length: " + payloadSize);
//                    }
//                    metadata = false;
//
//                    if (byteBuffer.size() >= payloadSize) {
//                        messageSize = decodeInteger(Arrays.copyOfRange(byteBuffer.toByteArray(), 4, 8), 0);
//                        photoSize = decodeInteger(Arrays.copyOfRange(byteBuffer.toByteArray(), 8, 12), 0);
//                        //System.out.println("payload length: " + payloadSize);
//                        //System.out.println("message length: " + messageSize);
//                        //System.out.println("photo length: " + photoSize);
//                        mMessageOut.write(byteBuffer.toByteArray(), 12, 1);
//                        recording = new String(mMessageOut.toByteArray(), "UTF-8");
//                        mMessageOut.reset();
//
//                        mMessageOut.write(byteBuffer.toByteArray(), 13, 4);
//                        battery = decodeInteger(mMessageOut.toByteArray(), 0);
//                        mMessageOut.reset();
//                        //mTCP.sendDataDB("B:" + Integer.toString(battery) + ",");
//                        mMessageOut.write(byteBuffer.toByteArray(), 17, 4);
//                        error = decodeInteger(mMessageOut.toByteArray(), 0);
//                        mMessageOut.reset();
//                        //mTCP.sendDataDB("E:" + Integer.toString(error) + ",");
//
//                        mMessageOut.write(byteBuffer.toByteArray(), 21, messageSize);
//                        message = new String(mMessageOut.toByteArray(), "UTF-8");
//                        //String photoName = message.substring(22, 43);
//                        mMessageOut.reset();
//                        if (message.contains("CAMERA")) {
//                            camera = message.substring(6, 9);
//
//                            System.out.println("Inital Message: " + message);
//                            message = message.substring(9, messageSize);
//                            System.out.println("Reading From Client: " + camera);
//                            //
//                        }
//                        if (message.contains("NOTCONNECTED")) {
//                            System.out.println("Client: " + camera + " closed connection");
//                            closeAll();
//                            //
//                        }
//                        if (recording.equals("N")) {
//                            recording = "NOTRECORDING";
//                            //mTCP.sendDataDB("NOTRECORDING,");
//                            //System.out.println("NOTRECORDING,");
//                        } else {
//                            recording = "RECORDING";
//                            //mTCP.sendDataDB("RECORDING,");
//                            //System.out.println("RECORDING,");
//                        }
//                        messageSend = camera + "," + "B:" + battery + "," + "E:" + error + "," + recording
//                                + "," + message + ",";
//                        //System.out.println("java: " + messageSend);
//                        mTCP.sendDataDB(messageSend);
//
//                        try {
//                            if (byteBuffer.size() > payloadSize) {
//                                if (photoSize != 0) {
//                                    mPhotoOut.write(byteBuffer.toByteArray(), 21 + messageSize, photoSize);
//                                    //System.out.println("photoOut: " + mPhotoOut.size());
//                                    //photoName = message.substring(22, 43);
//                                    photoName = message.substring(29, 53);
//                                    CameraApp.setIcon(mPhotoOut.toByteArray(), photoName);
//                                    mTCP.sendPhotoDB(mPhotoOut.toByteArray());
//                                }
//                                ByteArrayOutputStream tempBuffer = new ByteArrayOutputStream();
//                                tempBuffer.write(byteBuffer.toByteArray(), payloadSize, byteBuffer.size() - payloadSize);
//                                byteBuffer.reset();
//                                byteBuffer.write(tempBuffer.toByteArray());
//                                metadata = true;
//                                tempBuffer.close();
//                            } else {
//                                if (photoSize != 0) {
//                                    mPhotoOut.write(byteBuffer.toByteArray(), 21 + messageSize, photoSize);
//                                    //System.out.println("photoOut: " + mPhotoOut.size());
//                                    //photoName = message.substring(22, 43);
//                                    photoName = message.substring(26, 53);
//                                    CameraApp.setIcon(mPhotoOut.toByteArray(), photoName);
//                                    mTCP.sendPhotoDB(mPhotoOut.toByteArray());
//                                    byteBuffer.reset();
//                                    mPhotoOut.reset();
//                                    metadata = true;
//                                } else {
//                                    byteBuffer.reset();
//                                    metadata = true;
//                                }
//                            }
//                        } catch (OutOfMemoryError e) {
//                            e.printStackTrace();
//                            System.out.println("byte buffer length: " + byteBuffer.size());
//                            System.out.println("buffer length: " + len);
//                            System.out.println("payload length: " + payloadSize);
//                            System.out.println("message length: " + messageSize);
//                            System.out.println("photo length: " + photoSize);
//                            System.out.println("meta data: " + metadata);
//                            metadata = true;
//                            byteBuffer.reset();
//                            mPhotoOut.reset();
//                            mMessageOut.reset();
//                            //mTCP.sendDataAndroid("Stop");
//                            mTCP.sendDataDB("NOTRECORDING" + ",");
//
//                        } catch (IndexOutOfBoundsException e) {
//                            e.printStackTrace();
//                            System.out.println("byte buffer length: " + byteBuffer.size());
//                            System.out.println("buffer length: " + len);
//                            System.out.println("payload length: " + payloadSize);
//                            System.out.println("message length: " + messageSize);
//                            System.out.println("photo length: " + photoSize);
//                            System.out.println("meta data: " + metadata);
//                            metadata = true;
//                            byteBuffer.reset();
//                            mPhotoOut.reset();
//                            mMessageOut.reset();
//                            buffer = clearBuffer();
//                            //mTCP.sendDataAndroid("Stop");
//                            mTCP.sendDataDB("NOTRECORDING" + ",");
//                        }
//                    }
//                }
//                messageSend = camera + "," + "NOTCONNECTED" + ",";
//                mTCP.sendDataDB(messageSend);
//
//                closeAll();
//                try {
//                    Thread.currentThread().join();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        public void closeAll() {
//            try {
//                in.close();
//                mMessageOut.close();
//                mPhotoOut.close();
//                byteBuffer.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
}




