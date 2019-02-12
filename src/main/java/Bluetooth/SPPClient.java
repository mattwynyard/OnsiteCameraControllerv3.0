/**
 * Copyright 2018 - Onsite Developments
 * @author Matt Wynyard November 2018
 * @version 0.2
 */

package Bluetooth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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

	/**
     * Runnable that will read from the server (Android phone) on a thread
     */
    private Runnable readFromServer = new Runnable() {
        @Override
        public void run() {
            try {
                String buffer;
                System.out.println("Reading From Server"); 
                in = mStreamConnection.openInputStream();
                reader = new BufferedReader(new InputStreamReader(in));

                while ((buffer=reader.readLine())!=null) {
                    System.out.println("And: " + buffer);
                    if (buffer.contains("NOTRECORDING")) {
                        CameraApp.setRecording(false);
                        mTCP.sendDataDB(buffer);
                    } else if (buffer.contains("RECORDING")) {
                        CameraApp.setRecording(true);
                        mTCP.sendDataDB(buffer);
                    } else if (buffer.contains("CONNECTED")) {
                        CameraApp.setStatus("CONNECTED");
                        mTCP.sendDataDB(buffer);
                    } else if (buffer.contains("HOME:")) {
                        if (buffer.contains("DESTROYED") || buffer.contains("DETACHED")) {
                            mTCP.sendDataDB(buffer);
                            CameraApp.setStatus("NOTCONNECTED");
                            CameraApp.setRecording(false);
                        }
                    } else if (buffer.contains(".jpg")) {
                        mTCP.sendDataDB(buffer);
                        //CameraApp.setPhotoLabel(buffer.substring(12));
                    } else if (buffer.contains("B:")) {
                        mTCP.sendDataDB(buffer);
                        //CameraApp.setBatteryLabel(buffer.substring(2));
                    } else if (buffer.contains("M:")) {
                        mTCP.sendDataDB(buffer);
                        //CameraApp.setMemoryLabel(buffer.substring(2));
                    } else if (buffer.contains("APP: Crash")){
                        mTCP.sendDataDB(buffer);
                        CameraApp.setRecording(false);
                        CameraApp.setConnected(false);
                    }   else {
                        mTCP.sendDataDB(buffer);
                    }
                }
            } catch (IOException e) {
                try {

                    mTCP.sendDataDB(e.toString());
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
        }
    };
} //end class
