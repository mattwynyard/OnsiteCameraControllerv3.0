/**
 * Copyright 2018 - Onsite Developments
 * @author Matt Wynyard November 2018
 * @version 1.1
 */

package TCPConnection;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import Bluetooth.BluetoothManager;
import java.sql.*;

/**
 * Main application class for CameraApp
 * Builds user interface and intialises a bluetooth connection to Android App via the custom class Bluetooth Manager
 *
 *
 */
public class CameraApp {

//    //text labels
//    private static JLabel statusText;
//    private static JLabel cameraText;
//    private static JLabel photoText;
//    private static JLabel memoryText;
//    private static JLabel ipText;
//    private static JLabel batteryText;
//
//    //message labels
//    private static JLabel statusLabel;
//    private static JLabel cameraLabel;
//    private static JLabel photoLabel;
//    private static JLabel batteryLabel;
//    private static JLabel memoryLabel;
//    private static JLabel ipLabel;
//
//    //buttons
//	private static JButton startButton;
//    private static JButton stopButton;
//    private static JButton connectButton;
//
//    //constants
//    public static final Color DARK_GREEN = new Color(0,153,0);
//    private static final Insets insets = new Insets(0, 0, 0, 0);
//
//    private Scanner sc;
//
//    private static String ip;
//    private static int flag = 0;
    private static boolean connected = false;
    private static boolean recording = false;
    private static String status;
//    private static boolean DEBUG = true;
//    private static String mode;
    private static BluetoothManager mBluetooth;
    private static int count = 0;
    //private static TCPServer mTCP;

    private static Runnable ShutdownHook = new Runnable() {
        @Override
        public void run () {
            if (mBluetooth.mClient != null) {
                mBluetooth.mClient.mTCP.sendDataDB("NOTRECORDING");
                mBluetooth.mClient.mTCP.sendDataDB("NOTCONNECTED");
                mBluetooth.mClient.mTCP.sendDataDB("ERROR");
                mBluetooth.mClient.mTCP.closeAll();
                mBluetooth.mClient.closeAll();
            }
        }

    };

    public static void main(String[] args) {

        System.out.println(args[0]);

        mBluetooth = new BluetoothManager(args[0]);
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

    public static void setIcon(byte[] bytes, String name) {

        int length = bytes.length;
        //System.out.println("Bytes received " + length);
        try {
            InputStream in = new ByteArrayInputStream(bytes);

            final BufferedImage bufferedImage = ImageIO.read(in);
            String suffix;
            if (count % 2 == 0) {
                suffix = Integer.toString(0);
            } else {
                suffix = Integer.toString(1);
            }
            final File imageFile = new File("C:\\Road Inspection\\Thumbnails\\" + name + ".jpg");
            //icon = new ImageIcon(bufferedImage);
            //imageLabel.setIcon(icon);

            ImageIO.write(bufferedImage, "jpg", imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        count++;

    }



    public static void setConnected(boolean state) {
        connected = state;

    }

    public synchronized static void setStatus(String state) {
        status = state;

    }

    public synchronized static void setRecording(boolean state) {
        recording = state;

    }


}