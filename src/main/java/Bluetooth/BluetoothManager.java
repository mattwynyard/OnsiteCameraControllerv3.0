/**
 * Copyright 2018 - Onsite Developments
 * @author Matt Wynyard November 2018
 * @version 0.2
 */

package Bluetooth;

import TCPConnection.TCPServer;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import java.io.IOException;
import java.util.Vector;

public class BluetoothManager extends Thread  {
	
	private LocalDevice mLocalDevice;
	private RemoteDevice mRemoteDevice;
	private DiscoveryAgent mAgent;
	private String camera;
	final Object lock = new Object();
	final Object enquiryLock = new Object();
	final Object searchLock = new Object();
    //vector containing the devices discovered, kept as Vector in case we need to a more remote devices
	private Vector<RemoteDevice> mDevices = new Vector();
	private String connectionURL = null;
    private TCPServer tcpServer;
	private long start;
	private long stop;

	/**
	 * Class constructor for Bluetooth manager
	 */
	public BluetoothManager() {
		
		try {	
			this.mLocalDevice = LocalDevice.getLocalDevice();
			mDevices.clear();
			//this.camera = camera;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Main entry point for Bluetooth manager. Intialises the discovery agent and then searches for bluetooth devices
	 * Device discovery and connection is handled by the bluecove callbacks.
	 */
	public void run() {

		System.out.println("Local Bluetooth Address: " + mLocalDevice.getBluetoothAddress());
		System.out.println("Device Name: " + mLocalDevice.getFriendlyName());

        tcpServer = new TCPServer(38200);
        if (tcpServer != null) {
            SPPServer sppServer = new SPPServer();
            sppServer.start();
            tcpServer.setPhoneServer(sppServer);
            sppServer.setTCPServer(tcpServer);
            //System.out.println("TCP Server waiting for connection...");
        }

        start = System.currentTimeMillis();
	}

} //end class