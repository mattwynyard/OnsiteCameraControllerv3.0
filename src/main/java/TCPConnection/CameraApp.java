/**
 * Copyright 2018 - Onsite Developments
 * @author Matt Wynyard November 2018
 * @version 1.1
 */

package TCPConnection;

import java.awt.*;
import java.util.*;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JLabel;
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
//        Connection con = null;
//        System.out.println("Hello");
//            try {
//                Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
//                con = DriverManager.getConnection("jdbc:odbc:driver={Microsoft Access Driver(*.mdb, *.mde, *.accdb)" +
//                        "};DBQ=C:\\androidapp\\Access\\Controller_TEST_v1.0.accdb");
//                //Statement st = con.createStatement();
//                if (con != null) {
//                    System.out.println(con.getClientInfo().toString());
//                    Statement stmt = con.createStatement();
//                    stmt.execute("select * from Camera");
//                    ResultSet rs = stmt.getResultSet();
//                    System.out.println("Camera: " + rs.getString("Camera"));
//
//                } else {
//                    System.out.println("Connection null");
//                }
//
//            } catch (Exception ex){
//                System.out.println(ex.getMessage());
//            }

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