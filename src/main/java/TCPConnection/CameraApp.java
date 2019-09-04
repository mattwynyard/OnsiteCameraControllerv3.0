/**
 * Copyright 2018 - Onsite Developments
 * @author Matt Wynyard November 2018
 * @version 1.1
 */

package TCPConnection;


import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import Bluetooth.BluetoothManager;


/**
 * Main application class for CameraApp
 * Intialiases bluetooth adapter with camera id and handles the image build.
 *
 *
 */
public class CameraApp {

    private static boolean connected = false;
    private static boolean recording = false;
    private static String status;
    private static BluetoothManager mBluetooth;
    public static int cameras;
    private static int count = 0;


    private static Runnable ShutdownHook = new Runnable() {
        @Override
        public void run () {
            if (mBluetooth.tcpServer != null) {
                mBluetooth.tcpServer.sendDataDB("NOTRECORDING,");
                mBluetooth.tcpServer.sendDataDB("NOTCONNECTED,");
                mBluetooth.tcpServer.sendDataDB("ERROR,");
                mBluetooth.sppServer.
                mBluetooth.tcpServer.closeAll();
            }
        }

    };

    /**
     * Main program entry point - takes an arguement sent from access which is the camera id,
     * to setup bluetooth adapter name
     * @param args
     */
    public static void main(String[] args) {

        //System.out.println(args[0]);
        cameras = Integer.valueOf(args[0]);
        mBluetooth = new BluetoothManager();
        mBluetooth.start();
        Runtime.getRuntime().addShutdownHook(new Thread(ShutdownHook));

        while(true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Reads in byte array to input stream to build a buffered image, then writes jpeg image to disk
     * @param bytes - byte array containing pixel data for the image
     * @param name - the photo name
     */
    public static void setIcon(byte[] bytes, String name) {

        try {
            long start = System.currentTimeMillis();
            InputStream in = new ByteArrayInputStream(bytes);
            //ImageInputStream in = ImageIO.createImageInputStream(bytes);
            BufferedImage bufferedImage = ImageIO.read(in);
            //System.out.println("Length:" + bytes.length);
            //System.out.println("Width:" + bufferedImage.getWidth());
            //System.out.println("File Saved: " + name);
            
            File imageFile = new File("C:\\Road Inspection\\Thumbnails\\" + name + ".jpg");
            ImageIO.write(bufferedImage, "JPG", imageFile);
            in.close();
            long end = System.currentTimeMillis();
            System.out.println("jpeg save time: " + (end - start));
        } catch (IOException e) {
            e.printStackTrace();
        }
        count++;
    }
}