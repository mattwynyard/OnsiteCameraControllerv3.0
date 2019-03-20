/**
 * Copyright 2018 - Onsite Developments
 * @author Matt Wynyard November 2018
 * @version 0.2
 */

package Bluetooth;

import java.io.*;
import java.math.BigInteger;
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
    private ByteArrayOutputStream byteOut;

	
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
     * @param command - the command to send
     */
    public void sendCommand(String command) {
        writer.println(command);
        writer.flush();
        try {
            Thread.sleep(200);
        } catch(Exception e) {
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

	/**
     * Runnable that will read from the server (Android phone) on a thread
     */
    private Runnable readFromServer = new Runnable() {
        @Override
        public void run() {
            try {
                System.out.println("Reading From Server");
                in = mStreamConnection.openInputStream();
                byte[] buffer = new byte[1024];
                int offset = 0;
                int dataOffset = 0;
                byteOut = new ByteArrayOutputStream();
                ByteArrayOutputStream photoOut = new ByteArrayOutputStream();
                int len = 0;
                boolean metadata = false; //receiving photo metadata
                boolean photodata = false; //receiving photo metadata
                int totalBytes = 9999999;
                int bytesReceived = 0;
                int cursor = 0;
                String photoName = "";
                while ((len = in.read(buffer)) != -1) {

                    //System.out.println("bytes received: " + len);
                    byteOut.write(buffer, 0, 2);
                    if (new String(byteOut.toByteArray(), "UTF-8").equals("P:")) {
                        metadata = true;
                    }
                    if (metadata) {
                        //message length
                        offset += 2;
                        byteOut.write(buffer, offset, 4);
                        offset += 4;
                        int messageSize = new BigInteger(byteOut.toByteArray()).intValue();
                        byteOut.reset();

                        //message
                        byteOut.write(buffer, 6, messageSize);
                        String message = new String(byteOut.toByteArray(), "UTF-8");
                        mTCP.sendDataDB(message);
//                        Exception in thread "Thread-2" java.lang.StringIndexOutOfBoundsException: String index out of range: 43
//                        at java.lang.String.substring(Unknown Source)
//                        at Bluetooth.SPPClient$1.run(SPPClient.java:147)
//                        at java.lang.Thread.run(Unknown Source)
                        photoName = message.substring(22, 43);
                        //System.out.println(photoName);
                        byteOut.reset();
                        offset += messageSize;

                        //photo length
                        byteOut.write(buffer, offset, 4);
                        int photoSize = new BigInteger(byteOut.toByteArray()).intValue(); //line 147
                        //System.out.println("Photo size: " + photoSize);
                        offset += 4;
                        byteOut.reset();

                        totalBytes = 2 + 4 + messageSize + 4 + photoSize;
                        //System.out.println("Total bytes: " + totalBytes);
                        photodata = true;
                        metadata = false;
                        //Start reading first part of photo
                        photoOut.write(buffer, offset, len - offset);
                        //System.out.println("Byte array size: " + photoOut.size())
                        offset += (len - offset);
                        bytesReceived = 0;
                        bytesReceived += offset;
                    } else if (photodata) {
                        //start reading photo
                        //System.out.println("Data offset " + offset);
                        //System.out.println("Data Read: " + len);
                        bytesReceived += len;
                        photoOut.write(buffer, 0, len);
                        //System.out.println("Byte array size: " + photoOut.size());


                    } else { //handle message only
                        byteOut.reset();
                        byteOut.write(buffer, 2, len - 2);
                        String message = new String(byteOut.toByteArray(), "UTF-8");
                        //System.out.println("Message only:"  + message);
                        mTCP.sendDataDB(message);
                        byteOut.reset();
                        buffer = clearBuffer();
                        offset = 0;
                    }
                    if (bytesReceived >= totalBytes) {
                        byte photo [] = photoOut.toByteArray();

                        byteOut.reset();
                        photoOut.reset();
                        CameraApp.setIcon(photo, photoName);
                        bytesReceived = 0;
                        metadata = false;
                        photodata = false;
                        offset = 0;
                    }
                }
                in.close();
            } catch (IOException e) {
                try {
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }

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
        }
    };
} //end class
