/**
 * Copyright 2018 - Onsite Developments
 * @author Matt Wynyard November 2018
 * @version 0.2
 */

package Bluetooth;

import TCPConnection.CameraApp;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import java.io.IOException;
import java.util.Vector;
import javax.bluetooth.UUID;


public class BluetoothManager implements DiscoveryListener {
	
	private LocalDevice mLocalDevice;
	private RemoteDevice mRemoteDevice;
	private DiscoveryAgent mAgent;
	final Object lock = new Object();
	final Object enquiryLock = new Object();
	final Object searchLock = new Object();
    //vector containing the devices discovered, kept as Vector in case we need to a more remote devices
	private Vector<RemoteDevice> mDevices = new Vector();
	private String connectionURL = null;
	public SPPClient mClient;

	private long start;
	private long stop;

	/**
	 * Class constructor for Bluetooth manager
	 */
	public BluetoothManager() {
		
		try {	
			this.mLocalDevice = LocalDevice.getLocalDevice();
			mDevices.clear();
			//ObexClient obex = new ObexClient();
		} catch (IOException e) {
			e.printStackTrace();
			//mClient.mTCP.sendDataDB(e.toString());
		}
	}

	/**
	 * Sends message 'Start' to Android phone
	 */
	public void sendStartCommand() {
		mClient.sendCommand("Start");
	}
	/**
	 * Sends message 'Stop' to Android phone
	 */
	public void sendStopCommand() {
		mClient.sendCommand("Stop");
	}

	/**
	 * Main entry point for Bluetooth manager. Intialises the discovery agent and then searches for bluetooth devices
	 * Device discovery and connection is handled by the bluecove callbacks.
	 */
	public void start() {

		System.out.println("Local Bluetooth Address: " + mLocalDevice.getBluetoothAddress());
		System.out.println("Name: " + mLocalDevice.getFriendlyName());
        //CameraApp.setStatus("DISCOVERING");
		//mClient.mTCP.sendData("Discovering");
        start = System.currentTimeMillis();

		try {
			synchronized (enquiryLock) {
			//Limited Dedicated Inquiry Access Code (LIAC)
			mAgent = mLocalDevice.getDiscoveryAgent();
			try {
				mAgent.startInquiry(DiscoveryAgent.LIAC, this);
			} catch (BluetoothStateException e){
				e.printStackTrace();
				//mClient.mTCP.sendDataDB(e.toString());
			} catch (IOException e) {
				e.printStackTrace();
				//mClient.mTCP.sendDataDB(e.toString());
			}
				enquiryLock.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			//mClient.mTCP.sendDataDB(e.toString());
		}
		System.out.println("Device Inquiry Completed. ");

		int deviceCount = mDevices.size();

		if (deviceCount <= 0) {
			System.out.println("No Devices Found.");
			//CameraApp.setStatus("NODEVICES");
			//mClient.mTCP.sendData("No Devices Found");
			mAgent.cancelInquiry(this);
		} else {
			//print bluetooth device addresses and names in the format [ No. address (name) ]
			System.out.println("Bluetooth Devices: ");
			for (int i = 0; i < deviceCount; i++) {
				mRemoteDevice = mDevices.elementAt(i);
				try {
					System.out.println((i + 1) + ". " + mRemoteDevice.getBluetoothAddress() +
							" (" + mRemoteDevice.getFriendlyName(true) + ")");
				} catch (IOException e) {
					e.printStackTrace();
					//mClient.mTCP.sendDataDB(e.toString());
				}
			}
		}
	}
		/**
	 * Called when a remote device OnSite_BLT_Adapter is found. Searches for service on that device to connect to.
	 * @param remoteDevice - the Onsite Bluetooth Adapter
	 * @param agent - local devices discovery agent
	 * @param client - this
	 */
	public void connect(RemoteDevice remoteDevice, DiscoveryAgent agent, BluetoothManager client) {
		
		UUID[] uuidSet = new UUID[1];
        uuidSet[0]=new UUID("0003000000001000800000805F9B34FB", false);
        int[] attrIds = { 0x0003 }; //RFCOMM
        System.out.println("\nSearching for service...");
        //CameraApp.setStatus("SEARCHING");
		//mClient.mTCP.sendData("Searching");
        try {
        	synchronized(searchLock) {

        		agent.searchServices(attrIds, uuidSet, remoteDevice, client);
				//System.out.println(agent.selectService(uuidSet[0],  ServiceRecord.NOAUTHENTICATE_NOENCRYPT, true));
				searchLock.wait();
        	}
		} catch (BluetoothStateException e) {
			e.printStackTrace();
			//mClient.mTCP.sendDataDB(e.toString());
		} catch (InterruptedException e) {
			e.printStackTrace();
			//mClient.mTCP.sendDataDB(e.toString());
		}
		if(connectionURL == null){
			System.out.println("Device does not support Simple SPP Service.");
			//mClient.mTCP.sendDataDB("Device does not support Simple SPP Service");
			//CameraApp.setStatus("NOTCONNECTED");
			//mClient.mTCP.sendDataDB("Bluetooth not connected");
			//System.exit(0);
		}
	}

	//***BLUECOVE CALLBACKS
	/**
	 * This call back method will be called for each discovered bluetooth devices.
	 * Each device added to device vector.
	 * @param btDevice - The Remote Device discovered.
	 * @param cod - The class of device record. Contains information on the bluetooth device.
	 * 
	 */
	public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
//		synchronized (discoverLock) {
//			discoverLock.notifyAll();
//		}
		System.out.println("Device discovered: " + btDevice.getBluetoothAddress());
		//mClient.mTCP.sendData("Device discovered: " + btDevice.getBluetoothAddress());
		try {
			if (btDevice.getFriendlyName(false).equals("OnSite_BLT_Adapter")) {
				System.out.println("Trusted: " + btDevice.isTrustedDevice());
				connect(btDevice, mAgent, this);
				mAgent.cancelInquiry(this);
				mDevices.addElement(btDevice);
			}
		} catch (IOException e) {
			e.printStackTrace();
			mClient.mTCP.sendDataDB(e.toString());
		}
		return;
	}
	
	/**
	 * This callback will be called when services found by DiscoveryListener during service search
	 * @param transID - the transaction ID of the service search that is posting the result.
	 * @param servRecord - a list of services found during the search request.
	 */
		public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
			synchronized (lock) {
				lock.notifyAll();
			}
			System.out.println("Service discovered");
			System.out.println(servRecord[0].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, true));
			//URL needed for connection to android bluetooth server
			connectionURL = servRecord[0].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, true);
			mAgent.cancelServiceSearch(transID);
			//Creates client running on new thread on specified url
			mClient = new SPPClient(connectionURL);
			if (mClient != null) {
				//CameraApp.setStatus("CONNECTED");
				stop = System.currentTimeMillis();
				System.out.println("Device discovery: " + ((stop - start)/ 1000) + " s");
				mClient.start();
				//mClient.mTCP.sendDataDB("CONNECTED");
				System.out.println("Client started");
			}
		}

		/**
		 * Called when service search completed
		 * @param transID - the transaction ID identifying the request which initiated the service search
		 * @param respCode - the response code that indicates the status of the transaction
		 */
		public void serviceSearchCompleted(int transID, int respCode) {

			synchronized(searchLock) {
				searchLock.notifyAll();
			}
			switch (respCode) {
			case DiscoveryListener.SERVICE_SEARCH_COMPLETED:
				System.out.println("SERVICE_SEARCH_COMPLETED");
				break;
		
			case DiscoveryListener.SERVICE_SEARCH_TERMINATED:
				System.out.println("SERVICE_SEARCH_TERMINATED");
				break;
		
			case DiscoveryListener.SERVICE_SEARCH_ERROR:
				System.out.println("SERVICE_SEARCH_ERROR");
				break;
				
			case DiscoveryListener.SERVICE_SEARCH_NO_RECORDS:
				System.out.println("SERVICE_SEARCH_NO_RECORDS");
				break;
				
			case DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE:
				System.out.println("SERVICE_SEARCH_DEVICE_NOT_REACHABLE");
				break;
		
			default :
				System.out.println("Unknown Response Code");
				break;
			}
		}
	/**
	 * This callback method will be called when the device discovery is
	 * completed.
	 * @param discType integer value for discovery result.
	 */
	public void inquiryCompleted(int discType) {
		synchronized(enquiryLock){
			enquiryLock.notifyAll();
		}
		switch (discType) {
		case DiscoveryListener.INQUIRY_COMPLETED :
			System.out.println("INQUIRY_COMPLETED");
			break;
	
		case DiscoveryListener.INQUIRY_TERMINATED :
			System.out.println("INQUIRY_TERMINATED");
			break;
	
		case DiscoveryListener.INQUIRY_ERROR :
			System.out.println("INQUIRY_ERROR");
			break;
	
		default :
			System.out.println("Unknown Response Code");
			break;
		}
	}//end method

} //end class