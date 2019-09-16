package Bluetooth;

import TCPConnection.CameraApp;
import TCPConnection.TCPServer;

import javax.bluetooth.RemoteDevice;
import javax.microedition.io.StreamConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;

public class Client implements Runnable {

    String camera;
    RemoteDevice device;
    String deviceName;
    StreamConnection connection;
    TCPServer mTCP;
    String macAddress;
    InputStream in; //input stream from android
    ByteArrayOutputStream mMessageOut;
    ByteArrayOutputStream mPhotoOut;
    ByteArrayOutputStream byteBuffer;
    String recording;
    int battery;
    int error;
    String message;
    String messageSend;

    public Client(String device, StreamConnection connection, TCPServer tcp) {
        this.deviceName = device;
        this.connection = connection;
        this.mTCP = tcp;
        //macAddress = this.device.getBluetoothAddress();

    }
    //private Runnable readFromClient = new Runnable() {

    @Override
    public void run() {
        System.out.println("Reading From Client: ");
        mMessageOut = new ByteArrayOutputStream();
        mPhotoOut = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        byteBuffer = new ByteArrayOutputStream();
        int len;

        String photoName;
        boolean metadata = true;
        int payloadSize = 0;
        int messageSize = 0;
        int photoSize = 0;
        try {
            in = connection.openInputStream();
            while ((len = in.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len); //read in total buffer from socket

                if (metadata == true) {
                    payloadSize = decodeInteger(Arrays.copyOfRange(byteBuffer.toByteArray(), 0, 4), 0);
                    System.out.println("payload length: " + payloadSize);
                }
                metadata = false;

                if (byteBuffer.size() >= payloadSize) {
                    messageSize = decodeInteger(Arrays.copyOfRange(byteBuffer.toByteArray(), 4, 8), 0);
                    photoSize = decodeInteger(Arrays.copyOfRange(byteBuffer.toByteArray(), 8, 12), 0);
                    //System.out.println("payload length: " + payloadSize);
                    //System.out.println("message length: " + messageSize);
                    //System.out.println("photo length: " + photoSize);
                    mMessageOut.write(byteBuffer.toByteArray(), 12, 1);
                    recording = new String(mMessageOut.toByteArray(), "UTF-8");
                    mMessageOut.reset();

                    mMessageOut.write(byteBuffer.toByteArray(), 13, 4);
                    battery = decodeInteger(mMessageOut.toByteArray(), 0);
                    mMessageOut.reset();
                    //mTCP.sendDataDB("B:" + Integer.toString(battery) + ",");
                    mMessageOut.write(byteBuffer.toByteArray(), 17, 4);
                    error = decodeInteger(mMessageOut.toByteArray(), 0);
                    mMessageOut.reset();
                    //mTCP.sendDataDB("E:" + Integer.toString(error) + ",");

                    mMessageOut.write(byteBuffer.toByteArray(), 21, messageSize);
                    message = new String(mMessageOut.toByteArray(), "UTF-8");
                    //String photoName = message.substring(22, 43);
                    mMessageOut.reset();
                    if (message.contains("CAMERA")) {
                        camera = message.substring(6, 9);

                        System.out.println("Inital Message: " + message);
                        message = message.substring(9, messageSize);
                        System.out.println("Reading From Client: " + camera);
                        //
                    }
                    if (message.contains("NOTCONNECTED")) {
                        System.out.println("Client: " + camera + " closed connection");
                        closeAll();
                        //
                    }
                    if (recording.equals("N")) {
                        recording = "NOTRECORDING";
                        //mTCP.sendDataDB("NOTRECORDING,");
                        //System.out.println("NOTRECORDING,");
                    } else {
                        recording = "RECORDING";
                        //mTCP.sendDataDB("RECORDING,");
                        //System.out.println("RECORDING,");
                    }
                    messageSend = camera + "," + "B:" + battery + "," + "E:" + error + "," + recording
                            + "," + message + ",";
                    //System.out.println("java: " + messageSend);
                    mTCP.sendDataDB(messageSend);

                    try {
                        if (byteBuffer.size() > payloadSize) {
                            if (photoSize != 0) {
                                mPhotoOut.write(byteBuffer.toByteArray(), 21 + messageSize, photoSize);
                                //System.out.println("photoOut: " + mPhotoOut.size());
                                //photoName = message.substring(22, 43);
                                photoName = message.substring(29, 53);
                                CameraApp.setIcon(mPhotoOut.toByteArray(), photoName);
                                mTCP.sendPhotoDB(mPhotoOut.toByteArray());
                            }
                            ByteArrayOutputStream tempBuffer = new ByteArrayOutputStream();
                            tempBuffer.write(byteBuffer.toByteArray(), payloadSize, byteBuffer.size() - payloadSize);
                            byteBuffer.reset();
                            byteBuffer.write(tempBuffer.toByteArray());
                            metadata = true;
                            tempBuffer.close();
                        } else {
                            if (photoSize != 0) {
                                mPhotoOut.write(byteBuffer.toByteArray(), 21 + messageSize, photoSize);
                                //System.out.println("photoOut: " + mPhotoOut.size());
                                //photoName = message.substring(22, 43);
                                photoName = message.substring(26, 53);
                                CameraApp.setIcon(mPhotoOut.toByteArray(), photoName);
                                mTCP.sendPhotoDB(mPhotoOut.toByteArray());
                                byteBuffer.reset();
                                mPhotoOut.reset();
                                metadata = true;
                            } else {
                                byteBuffer.reset();
                                metadata = true;
                            }
                        }
                    } catch (OutOfMemoryError e) {
                        e.printStackTrace();
                        System.out.println("byte buffer length: " + byteBuffer.size());
                        System.out.println("buffer length: " + len);
                        System.out.println("payload length: " + payloadSize);
                        System.out.println("message length: " + messageSize);
                        System.out.println("photo length: " + photoSize);
                        System.out.println("meta data: " + metadata);
                        metadata = true;
                        byteBuffer.reset();
                        mPhotoOut.reset();
                        mMessageOut.reset();
                        //mTCP.sendDataAndroid("Stop");
                        mTCP.sendDataDB("NOTRECORDING" + ",");

                    } catch (IndexOutOfBoundsException e) {
                        e.printStackTrace();
                        System.out.println("byte buffer length: " + byteBuffer.size());
                        System.out.println("buffer length: " + len);
                        System.out.println("payload length: " + payloadSize);
                        System.out.println("message length: " + messageSize);
                        System.out.println("photo length: " + photoSize);
                        System.out.println("meta data: " + metadata);
                        metadata = true;
                        byteBuffer.reset();
                        mPhotoOut.reset();
                        mMessageOut.reset();
                        buffer = clearBuffer();
                        //mTCP.sendDataAndroid("Stop");
                        mTCP.sendDataDB("NOTRECORDING" + ",");
                    }
                }
            }
            messageSend = camera + "," + "NOTCONNECTED" + ",";
            mTCP.sendDataDB(messageSend);

            closeAll();
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
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

    public void closeAll() {
        try {
            in.close();
            mMessageOut.close();
            mPhotoOut.close();
            byteBuffer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
