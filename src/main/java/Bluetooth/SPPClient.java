/**
 * Copyright 2018 - Onsite Developments
 * @author Matt Wynyard November 2018
 * @version 0.2
 */

package Bluetooth;

import java.io.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Scanner;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import TCPConnection.CameraApp;
import TCPConnection.TCPServer;


public class SPPClient extends Thread {

    private String connectionURL;
    private boolean connected = false;
    private StreamConnection mStreamConnection;
    private OutputStream out; //Android out
    private InputStream in; //Android in
    private PrintWriter writer; //Android writer
    private BufferedReader reader; //Android reader
    public TCPServer mTCP;
    private Thread mReadThread;
    private ByteArrayOutputStream mMessageOut;
    private ByteArrayOutputStream mPhotoOut;
    private int metaBytes;
    private FileWriter fw;
    private BufferedWriter bw;

    private boolean metadata = false; //receiving photo metadata
    private boolean photodata = false; //receiving photo metadata
    private int headerSize = 0;
    private int messageSize = 0;


    public SPPClient(String connectionURL) {
        this.connectionURL = connectionURL;
        try {
            mStreamConnection = (StreamConnection) Connector.open(connectionURL);
            if (mStreamConnection != null) {
                connected = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends command to Android phone via bluetooth connection
     *
     * @param command - the command to send
     */
    public void sendCommand(String command) {
        writer.println(command);
        writer.flush();
        try {
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        if (connected) {
            System.out.println("Connection succesful...");
        }
        try {
            // Only one usage of each socket address (protocol/network address/port) is normally permitted.
            //can cause null pointer exception in Thread-2 if instance of app already running
            out = mStreamConnection.openOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(out));
            mTCP = new TCPServer(this);
            mReadThread = new Thread(readFromServer);
            mReadThread.start();
        } catch (IOException e) {
            e.printStackTrace();
            mTCP.sendDataDB(e.getMessage());
        } catch (NullPointerException e1) {
            e1.printStackTrace();
            mTCP.sendDataDB(e1.getMessage());
            //System.exit(0);
        }
    }

    /**
     * Called from shutdown hookup to fail gracefully
     */
    public void closeAll() {
        try {
            out.close();
            in.close();
            writer = null;
            reader = null;
            mStreamConnection = null;
            mReadThread = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] clearBuffer() {
        return new byte[1024];
    }

    private void writeLog(String message, int bytesReceived, int totalBytes) {

        try {
            fw = new FileWriter("C:\\Road Inspection\\Log\\Log.txt", true);
            bw = new BufferedWriter(fw);
            bw.write(message + "|" + Integer.toString(bytesReceived) + "|" + Integer.toString(totalBytes));
            bw.newLine();
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeLog(Exception error) {
        try {
            fw = new FileWriter("C:\\Road Inspection\\Log\\Log.txt", true);
            bw = new BufferedWriter(fw);
            bw.write(error.getMessage() + "|" + error.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String decodeIntToString(byte[] buffer, int offset) {
        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        temp.write(buffer, offset, 4);
        int value = new BigInteger(temp.toByteArray()).intValue();
        try {
            temp.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return String.valueOf(value);
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
        //String message = null;
        try {
            message = new String(temp.toByteArray(), "UTF-8");
            //System.out.println("message: " + message);
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

    private void buildPhoto(int length, byte[] b) {

        mPhotoOut.write(b, 0, b.length);
        if (mPhotoOut.size() == length) {
            System.out.println("Building photo");
            byte photo[] = mPhotoOut.toByteArray();
            mPhotoOut.reset();
            //CameraApp.setIcon(photo, photoName);
            //writeLog(message, bytesReceived, totalBytes);
        } else {
            mPhotoOut.write(b, 0, b.length);
        }
    }

    private void sendMessage(byte[] buffer) {


        String recording = decodeString(buffer, 0, 1);
        String battery = decodeIntToString(buffer, 1);
        String error = decodeIntToString(buffer, 5);
        String message = decodeString(buffer, 9, messageSize);
        if (recording.equals("R")) {
            mTCP.sendDataDB("RECORDING,");
        } else {
            mTCP.sendDataDB("NOTRECORDING,");
        }

        mTCP.sendDataDB("B:" + battery);
        mTCP.sendDataDB("E:" + error);
        mTCP.sendDataDB(message);
        mMessageOut.reset();

    }

    private void buildMessage(int length, byte[] b) {
        int size = mMessageOut.size();
        System.out.println("mout size : " + b.length);
        if (length == size) {
            sendMessage(mMessageOut.toByteArray());
            mMessageOut.reset();
            metaBytes = 0;
            photodata = true;
            metadata = false;
        } else {
            mMessageOut.write(b, metaBytes, b.length);
            System.out.println("Building message");
//            System.out.println("length " + length);
//            System.out.println("b length: " + b.length);
//            System.out.println("mout size : " + b.length);
            size = mMessageOut.size();
            metaBytes += b.length;
            if (length == size) {
                //send data
                System.out.println("sending message..");
                sendMessage(mMessageOut.toByteArray());
                mMessageOut.reset();
                metaBytes = 0;
                photodata = true;
                metadata = false;
            }
        }
    }

    private Runnable readFromServer = new Runnable() {
        @Override
        public void run() {
            System.out.println("Reading From Server");
            try {
                in = mStreamConnection.openInputStream();


                byte[] buffer = new byte[1024];
                int offset = 0;
                mMessageOut = new ByteArrayOutputStream();
                mPhotoOut = new ByteArrayOutputStream();
                int len = 0;


                int photoBytes = 0;
                int bytesLeft = 0;
                String photoName = "";
                int messageSize = 0;
                int photoSize = 0;

                while ((len = in.read(buffer)) != -1) {
                    System.out.println("buffer length: " + len);
                    //byteOut.write(buffer, 0, 1);
                    String connect = decodeString(buffer, 0, 10);

                    if (metadata) {
                        if (metaBytes == 0) {
                            //ByteArrayOutputStream temp = new ByteArrayOutputStream();
                            //temp.write(buffer, 9, 4);
                            messageSize = decodeInteger(Arrays.copyOfRange(buffer, 9, 13), 0);
                            int headerSize = 13 + messageSize;
                            System.out.println("Message size: " + messageSize);
                            buildMessage(headerSize, Arrays.copyOfRange(buffer, 0, headerSize));
                            metaBytes += headerSize;
                            //temp.close();
                        } else {
                            buildMessage(headerSize, Arrays.copyOfRange(buffer, metaBytes, len - (headerSize - metaBytes)));
                        }
                    } else {

                    }

                    if (connect.equals("CONNECTED,")) {
                        //System.out.println(decodeString(buffer, 0, 10));
                        mTCP.sendDataDB("CONNECTED,");
                        mMessageOut.reset();
                        buffer = clearBuffer();
                        metadata = true;
                        metaBytes = 0;
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    };
}


//	/**
//     * Runnable that will read from the server (Android phone) on a thread
//     */
//    private Runnable readFromServer = new Runnable() {
//        @Override
//        public void run() {
//            try {
//                System.out.println("Reading From Server");
//                in = mStreamConnection.openInputStream();
//                byte[] buffer = new byte[1024];
//                int offset = 0;
//                byteOut = new ByteArrayOutputStream();
//                ByteArrayOutputStream photoOut = new ByteArrayOutputStream();
//                int len = 0;
//                boolean metadata = false; //receiving photo metadata
//                boolean photodata = false; //receiving photo metadata
//                boolean mixeddata = false;
//                int totalBytes = 9999999;
//                int bytesReceived = -1;
//                //int messageBytes = 0;
//                int photoBytes = 0;
//                int messageBytes = 0;
//                int cursor = 0;
//                String photoName = "";
//                String message = "";
//                int messageSize = 0;
//                int photoSize = 0;
//                    while ((len = in.read(buffer)) != -1) {
//                        System.out.println("buffer length: " + len);
//                        try {
//                            byteOut.write(buffer, 0, 2);
//                            if (new String(byteOut.toByteArray(), "UTF-8").equals("P:")) {
//                                metadata = true;
//                                offset += 2;
//                            }
//                            if (metadata) {
//                            //message length
//                                byteOut.reset();
//                                byteOut.write(buffer, offset, 4);
//                                offset += 4;
//                                messageSize = new BigInteger(byteOut.toByteArray()).intValue();
//                                byteOut.reset();
//
//                                //message
//                                byteOut.write(buffer, 6, messageSize);
//                                message = new String(byteOut.toByteArray(), "UTF-8");
//                                mTCP.sendDataDB(message);
//                                //                        Exception in thread "Thread-2" java.lang.StringIndexOutOfBoundsException: String index out of range: 43
//                                //                        at java.lang.String.substring(Unknown Source)
//                                //                        at Bluetooth.SPPClient$1.run(SPPClient.java:147)
//                                //                        at java.lang.Thread.run(Unknown Source)
//                                photoName = message.substring(22, 43);
//                                //System.out.println(photoName);
//                                byteOut.reset();
//                                offset += messageSize;
//
//                                //photo length
//                                byteOut.write(buffer, offset, 4);
//                                photoSize = new BigInteger(byteOut.toByteArray()).intValue();
//                                //System.out.println("Photo size: " + photoSize);
//                                offset += 4;
//                                byteOut.reset();
//
//                                totalBytes = 2 + 4 + messageSize + 4 + photoSize;
//                                //System.out.println("Total bytes: " + totalBytes);
//                                photodata = true;
//                                metadata = false;
//                                //Start reading first part of photo
//                                photoOut.write(buffer, offset, len - offset);
//                                //System.out.println("Byte array size: " + photoOut.size())
//                                offset += (len - offset);
//                                bytesReceived = 0;
//                                bytesReceived += offset;
//
//                                photoBytes = len - offset;
//
//                            } else if (photodata) {
//                            //start reading photo
//                            //System.out.println("Data offset " + offset);
//                            //System.out.println("Data Read: " + len);
//                                if (photoBytes >= photoSize) {
//                                    byte photo[] = photoOut.toByteArray();
//
//                                    byteOut.reset();
//                                    photoOut.reset();
//                                    CameraApp.setIcon(photo, photoName);
//                                    writeLog(message, bytesReceived, totalBytes);
//                                    bytesReceived = 0;
//                                    metadata = false;
//                                    photodata = false;
//                                    offset = 0;
//                                    photoBytes = 0;
//                                } else {
//                                    bytesReceived += len;
//                                    photoOut.write(buffer, 0, len);
//                                }
//
//
//                            }
//                        } catch (UnsupportedEncodingException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//
//                } catch (IOException  e) {
//                    e.printStackTrace();
//                }
//        }
//    };
//} //end class
//                while ((len = in.read(buffer)) != -1) {
//                    System.out.println("buffer length: " + len);
//                    try {
//                        //System.out.println("bytes received: " + len);
//                        byteOut.write(buffer, 0, 2);
//                        if (new String(byteOut.toByteArray(), "UTF-8").equals("P:")) {
//                            metadata = true;
//
////                        } else if (new String(byteOut.toByteArray(), "UTF-8").equals("Z:")) {
////                            metadata = false;
////                        }
//                        } else {
//                            metadata = false;
//                        }
//                        offset += 2;
//
//                        if (metadata) {
//                            //message length
//
//                            byteOut.write(buffer, offset, 4);
//                            offset += 4;
//                            messageSize = new BigInteger(byteOut.toByteArray()).intValue();
//                            byteOut.reset();
//
//                            //message
//                            byteOut.write(buffer, 6, messageSize);
//                            message = new String(byteOut.toByteArray(), "UTF-8");
//                            mTCP.sendDataDB(message);
//                            //                        Exception in thread "Thread-2" java.lang.StringIndexOutOfBoundsException: String index out of range: 43
//                            //                        at java.lang.String.substring(Unknown Source)
//                            //                        at Bluetooth.SPPClient$1.run(SPPClient.java:147)
//                            //                        at java.lang.Thread.run(Unknown Source)
//                            photoName = message.substring(22, 43);
//                            //System.out.println(photoName);
//                            byteOut.reset();
//                            offset += messageSize;
//
//                            //photo length
//                            byteOut.write(buffer, offset, 4);
//                            int photoSize = new BigInteger(byteOut.toByteArray()).intValue();
//                            //System.out.println("Photo size: " + photoSize);
//                            offset += 4;
//                            byteOut.reset();
//
//                            totalBytes = 2 + 4 + messageSize + 4 + photoSize;
//                            //System.out.println("Total bytes: " + totalBytes);
//                            photodata = true;
//                            metadata = false;
//                            //Start reading first part of photo
//                            photoOut.write(buffer, offset, len - offset);
//                            //System.out.println("Byte array size: " + photoOut.size())
//                            offset += (len - offset);
//                            bytesReceived = 0;
//                            bytesReceived += offset;
//                        } else if (photodata) {
//                            //start reading photo
//                            //System.out.println("Data offset " + offset);
//                            //System.out.println("Data Read: " + len);
//                            bytesReceived += len;
//                            photoOut.write(buffer, 0, len);
//                        } else if (mixeddata) {
//                            //start reading photo
//                            //System.out.println("Data offset " + offset);
//                            //System.out.println("Data Read: " + len);
//                            bytesReceived += len;
//                            photoOut.write(buffer, 0, len);
//                            //System.out.println("Byte array size: " + photoOut.size());
//
//                        } else { //handle message only
//                            byteOut.reset();
//                            //byteOut.write(buffer, 2, len - 2);
//                            byteOut.write(buffer, 2, 2);
//                            offset += 2;
//                            String prefix = new String(byteOut.toByteArray(), "UTF-8");
//                            if (prefix.equals("M:") || prefix.equals("E:") || prefix.equals("B:")) {
//                                //byteOut.reset();
//                                for (int i = offset; i < len; i++) {
//                                    offset++;
//                                    System.out.println("Offset:" + offset);
//                                    byteOut.write(buffer, offset, 1);
//                                    //String ch = new String(byteOut.toByteArray(), "UTF-8");
//                                    String ch = new String(buffer,"UTF-8");
//                                    if (ch.equals(",")) {
//                                        message = new String(byteOut.toByteArray(), "UTF-8");
//                                        System.out.println("Message only:" + message);
//                                        totalBytes = 2 + byteOut.size();
//                                        bytesReceived = len;
//                                        mTCP.sendDataDB(message);
//                                        byteOut.reset();
//                                        //buffer = clearBuffer();
//                                        //offset = 0;
//                                        break;
//                                    }
//                                }
//                            }
//                            if (offset < len) {
//                                metadata = true;
//                            } else {
//                                metadata = false;
//                            }
//                        }
//                        if ((bytesReceived >= totalBytes) && photodata) {
//                            System.out.println("Expected bytes: " + totalBytes);
//                            System.out.println("Bytes read: " + bytesReceived);
//                            byte photo[] = photoOut.toByteArray();
//
//                            byteOut.reset();
//                            photoOut.reset();
//                            CameraApp.setIcon(photo, photoName);
//                            writeLog(message, bytesReceived, totalBytes);
//                            bytesReceived = 0;
//                            metadata = false;
//                            photodata = false;
//                            offset = 0;
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        System.out.println("Error: Resetting....");
//                        byteOut.reset();
//                        photoOut.reset();
//                        bytesReceived = 0;
//                        metadata = false;
//                        photodata = false;
//                        offset = 0;
//                        writeLog(e);
//                    }
//                }
//                in.close();
//            } catch (IOException  e) {
//                e.printStackTrace();
//            }

//            try {
//
//                String buffer;
//                System.out.println("Reading From Server");
//                in = mStreamConnection.openInputStream();
//                reader = new BufferedReader(new InputStreamReader(in));
//                while ((buffer=reader.readLine())!=null) {
//
//                    //System.out.println("And: " + buffer);
//                    if (buffer.contains("NOTRECORDING")) {
//                        CameraApp.setRecording(false);
//                        mTCP.sendDataDB(buffer);
//                    } else if (buffer.contains("RECORDING")) {
//                        CameraApp.setRecording(true);
//                        mTCP.sendDataDB(buffer);
//                    } else if (buffer.contains("CONNECTED")) {
//                        CameraApp.setStatus("CONNECTED");
//                        mTCP.sendDataDB(buffer);
//                    } else if (buffer.contains("HOME:")) {
//                        if (buffer.contains("DESTROYED") || buffer.contains("DETACHED")) {
//                            mTCP.sendDataDB("NOTCONNECTED,");
//                            CameraApp.setStatus("NOTCONNECTED");
//                            CameraApp.setRecording(false);
//                        }
////                    } else if (buffer.contains("R:")) {
////                        mTCP.sendDataDB(buffer);
////                        //CameraApp.setPhotoLabel(buffer.substring(12));
//                    } else if (buffer.contains("B:")) {
//                        mTCP.sendDataDB(buffer);
//                        //CameraApp.setBatteryLabel(buffer.substring(2));
//                    } else if (buffer.contains("M:")) {
//                        mTCP.sendDataDB(buffer);
//                    } else if (buffer.contains("T:")) {
//                        mTCP.sendDataDB(buffer);
//                    } else if (buffer.contains("A:")) {
//                        mTCP.sendDataDB(buffer);
//                    } else if (buffer.contains("S:")) {
//                        mTCP.sendDataDB(buffer);
//                        //CameraApp.setMemoryLabel(buffer.substring(2));
//                    } else if (buffer.contains("E:")){
//                        mTCP.sendDataDB(buffer);
//                        CameraApp.setRecording(false);
//                        CameraApp.setConnected(false);
//                    }   else {
//                        mTCP.sendDataDB(buffer);
//                    }
//                }
//            } catch (IOException e) {
//                try {
//
//                    mTCP.sendDataDB(e.toString());
//                    in.close();
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                }
//                e.printStackTrace();
//            }

