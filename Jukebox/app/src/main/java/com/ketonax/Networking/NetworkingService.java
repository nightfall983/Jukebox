package com.ketonax.Networking;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.ketonax.Constants.AppConstants;
import com.ketonax.Constants.Networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by nightfall on 7/13/14.
 */

public class NetworkingService extends Service {

    static Messenger mClient;
    boolean keepRunning = true;
    UDP_Sender sender = null;
    Receiver receiver = null;
    GroupReceiver groupReceiver = null;
    Messenger mMessenger = new Messenger(new IncomingHandler());
    private final IBinder mBinder = new MyBinder();
    /* Network Variables */
    private DatagramSocket udpSocket = null;
    private MulticastSocket multicastSocket = null;
    private InetAddress serverAddress = null;

    public NetworkingService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        udpSocket = Networking.getSocket();
        multicastSocket = Networking.getMulticastSocket();
        try {
            multicastSocket.joinGroup(Networking.getGroupAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
        receiver = new Receiver();
        receiver.start();
        //groupReceiver = new GroupReceiver();
        //groupReceiver.start();
    }

    @Override
    public void onDestroy() {
        keepRunning = false;
        try {
            multicastSocket.leaveGroup(Networking.getGroupAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    public void sendSong(String userIP, int tcpPortNumber, String songName) {
        TCP_Sender tcpSender = new TCP_Sender();
        tcpSender.send(userIP, tcpPortNumber, songName);
    }

    public class MyBinder extends Binder {
        public NetworkingService getService() {
            return NetworkingService.this;
        }
    }

    private class UDP_Sender {
        /** Used for sending data to Jukebox Server */
        private byte[] sendData = null;
        private AsyncTask<Void, Void, Void> asyncSender;
        private DatagramPacket sendPacket = null;

        public void send(final String message) {
            asyncSender = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {

                    try {
                        serverAddress = InetAddress.getByName(Networking.SERVER_IP_STRING);
                        sendData = message.getBytes();
                        sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, Networking.SERVER_PORT);
                        udpSocket.send(sendPacket);
                    } catch (SocketException e) {
                        e.printStackTrace();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };

            if (Build.VERSION.SDK_INT >= 11)
                asyncSender.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                asyncSender.execute();
        }
    }

    private class TCP_Sender {

        private AsyncTask<Void, Void, Void> asyncSender;

        public void send(final String userIP, final int tcpPortNumber, final String songName) {
            asyncSender = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    //TODO Implement TCP send part

                    return null;
                }
            };

            if (Build.VERSION.SDK_INT >= 11)
                asyncSender.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                asyncSender.execute();
        }
    }

    private class Receiver extends Thread {

        public Receiver() {
        }

        @Override
        public void run() {
            //sender.send(Networking.STATION_LIST_REQUEST_CMD);

            while (keepRunning) {
                byte[] receiveData = new byte[Networking.DATA_LIMIT_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                try {
                    udpSocket.receive(receivePacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String message = new String(receivePacket.getData()).trim();

                /* Parse messages and respond accordingly */
                if (message.startsWith(Networking.JUKEBOX_MESSAGE_IDENTIFIER)) {

                    if (message.contains(Networking.SEPARATOR)) {
                        String[] msgArray = message.split(Networking.SEPARATOR);

                        if (msgArray[0].equals(Networking.STATION_KILLED_NOTIFIER)) {
                            Log.i(AppConstants.APP_TAG, Networking.STATION_KILLED_NOTIFIER);
                            String stationName = msgArray[1];
                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            Message msg = Message.obtain(null, AppConstants.STATION_KILLED_NOTIFIER);
                            msg.setData(bundle);
                            try {
                                if (mClient != null)
                                    mClient.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.STATION_ADDED_NOTIFIER)) {
                            Log.i(AppConstants.APP_TAG, Networking.STATION_ADDED_NOTIFIER);
                            String stationName = msgArray[1];
                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            Message msg = Message.obtain(null, AppConstants.STATION_ADDED_NOTIFIER);
                            msg.setData(bundle);
                            try {
                                if (mClient != null)
                                    mClient.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.SONG_ADDED_NOTIFIER)) {
                            Log.i(AppConstants.APP_TAG, Networking.SONG_ADDED_NOTIFIER);
                            String stationName = msgArray[1];
                            String songName = msgArray[2];
                            Message msg = Message.obtain(null, AppConstants.SONG_ADDED_NOTIFIER);
                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            bundle.putString(AppConstants.SONG_NAME_KEY, songName);
                            msg.setData(bundle);
                            try {
                                if (mClient != null)
                                    mClient.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.SONG_REMOVED_NOTIFIER)) {
                            Log.i(AppConstants.APP_TAG, Networking.SONG_REMOVED_NOTIFIER);
                            String stationName = msgArray[1];
                            String songName = msgArray[2];
                            Message msg = Message.obtain(null, AppConstants.SONG_REMOVED_NOTIFIER);
                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            bundle.putString(AppConstants.SONG_NAME_KEY, songName);
                            msg.setData(bundle);
                            try {
                                if (mClient != null)
                                    mClient.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.SONG_ON_LIST_RESPONSE)) {
                            String stationName = msgArray[1];
                            String songName = msgArray[2];
                            Message msg = Message.obtain(null, AppConstants.SONG_ON_LIST_RESPONSE);
                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            bundle.putString(AppConstants.SONG_NAME_KEY, songName);
                            msg.setData(bundle);
                            try {
                                if (mClient != null)
                                    mClient.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.USER_ADDED_NOTIFIER)) {
                            Log.i(AppConstants.APP_TAG, Networking.USER_ADDED_NOTIFIER);
                            String stationName = msgArray[1];
                            String userAddress[] = msgArray[2].split(":");
                            String userIPString = userAddress[0];
                            int userPort = Integer.parseInt(userAddress[1]);

                            // Check to see if userIP contains '/' and remove it
                            if (userIPString.startsWith("/"))
                                userIPString = userIPString.replaceFirst("/", "");

                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            bundle.putString(AppConstants.USER_IP_KEY, userIPString);
                            bundle.putInt(AppConstants.USER_UDP_PORT_KEY, userPort);
                            Message msg = Message.obtain(null, AppConstants.USER_ADDED_NOTIFIER);
                            msg.setData(bundle);
                            try {
                                if (mClient != null)
                                    mClient.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.USER_REMOVED_NOTIFIER)) {
                            Log.i(AppConstants.APP_TAG, Networking.USER_REMOVED_NOTIFIER);
                            String stationName = msgArray[1];
                            String userAddress[] = msgArray[2].split(":");
                            String userIPString = userAddress[0];
                            int userPort = Integer.parseInt(userAddress[1]);

                            // Check to see if userIP contains '/' and remove it
                            if (userIPString.startsWith("/"))
                                userIPString = userIPString.replaceFirst("/", "");

                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            bundle.putString(AppConstants.USER_IP_KEY, userIPString);
                            bundle.putInt(AppConstants.USER_UDP_PORT_KEY, userPort);
                            Message msg = Message.obtain(null, AppConstants.USER_REMOVED_NOTIFIER);
                            msg.setData(bundle);
                            try {
                                if (mClient != null)
                                    mClient.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.CURRENTLY_PLAYING_NOTIFIER)) {
                            Log.i(AppConstants.APP_TAG, Networking.CURRENTLY_PLAYING_NOTIFIER);
                            String currentSongPlaying = msgArray[1];
                            String songHolderAddress[] = msgArray[2].split(":");
                            String songHolderIPString = songHolderAddress[0];

                            // Check to see if userIP contains '/' and remove it
                            if (songHolderIPString.startsWith("/"))
                                songHolderIPString = songHolderIPString.replaceFirst("/", "");
                        } else if (msgArray[0].equals(Networking.PLAY_SONG_CMD)) {
                            String songName = msgArray[1];
                            Message msg = Message.obtain(null, AppConstants.PLAY_SONG_CMD);
                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.SONG_NAME_KEY, songName);
                            msg.setData(bundle);
                            try {
                                if (mClient != null)
                                    mClient.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.STATION_LIST_REQUEST_RESPONSE)) {
                            Log.i(AppConstants.APP_TAG, Networking.STATION_LIST_REQUEST_RESPONSE);
                            String stationName = msgArray[1];
                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            Message msg = Message.obtain(null, AppConstants.STATION_LIST_REQUEST_RESPONSE);
                            msg.setData(bundle);
                            try {
                                if (mClient != null) {
                                    mClient.send(msg);
                                }
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.USER_ON_LIST_RESPONSE)) {
                            String stationName = msgArray[1];
                            String userAddress[] = msgArray[2].split(":");
                            String userIPString = userAddress[0];
                            int userPort = Integer.parseInt(userAddress[1]);

                            // Check to see if userIP contains '/' and remove it
                            if (userIPString.startsWith("/"))
                                userIPString = userIPString.replaceFirst("/", "");

                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            bundle.putString(AppConstants.USER_IP_KEY, userIPString);
                            bundle.putInt(AppConstants.USER_UDP_PORT_KEY, userPort);
                            Message msg = Message.obtain(null, AppConstants.USER_ON_LIST_RESPONSE);
                            msg.setData(bundle);
                            try {
                                if (mClient != null)
                                    mClient.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    } else
                        Log.e(AppConstants.APP_TAG, "Unknown message received from server: " + message);
                } else
                    Log.e(AppConstants.APP_TAG, "Unknown message received from server: " + message);
            }
        }
    }

    private class GroupReceiver extends Thread {
        @Override
        public void run() {

            while (keepRunning) {
                byte[] receiveData = new byte[Networking.DATA_LIMIT_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                try {
                    multicastSocket.receive(receivePacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String message = new String(receivePacket.getData()).trim();

                /* Parse messages and respond accordingly */
                if (message.startsWith(Networking.JUKEBOX_MESSAGE_IDENTIFIER)) {

                    if (message.contains(Networking.SEPARATOR)) {
                        String[] msgArray = message.split(Networking.SEPARATOR);

                        if (msgArray[0].equals(Networking.STATION_KILLED_NOTIFIER)) {
                            Log.d(AppConstants.APP_TAG, Networking.STATION_KILLED_NOTIFIER);
                            String stationName = msgArray[1];
                            Bundle bundle = new Bundle();
                            bundle.putString(Networking.STATION_KILLED_NOTIFIER, stationName);
                            Message msg = Message.obtain(null, AppConstants.STATION_KILLED_NOTIFIER);
                            msg.setData(bundle);
                            try {
                                if (mClient != null)
                                    mClient.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.STATION_ADDED_NOTIFIER)) {
                            Log.d(AppConstants.APP_TAG, Networking.STATION_ADDED_NOTIFIER);
                            String stationName = msgArray[1];

                            Bundle bundle = new Bundle();
                            bundle.putString(Networking.STATION_ADDED_NOTIFIER, stationName);
                            Message msg = Message.obtain(null, AppConstants.STATION_ADDED_NOTIFIER);
                            msg.setData(bundle);
                            try {
                                if (mClient != null)
                                    mClient.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.SONG_ADDED_NOTIFIER)) {
                            String songName = msgArray[1];
                        } else if (msgArray[0].equals(Networking.SONG_REMOVED_NOTIFIER)) {
                            String stationName = msgArray[1];
                            String songName = msgArray[2];
                        } else if (msgArray[0].equals(Networking.USER_ADDED_NOTIFIER)) {
                            String stationName = msgArray[1];
                            String userAddress[] = msgArray[2].split(":");
                            String userIPString = userAddress[0];
                            int userPort = Integer.parseInt(userAddress[1]);

                            // Check to see if userIP contains '/' and remove it
                            if (userIPString.startsWith("/"))
                                userIPString = userIPString.replaceFirst("/", "");

                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            bundle.putString(AppConstants.USER_IP_KEY, userIPString);
                            bundle.putInt(AppConstants.USER_UDP_PORT_KEY, userPort);
                            Message msg = Message.obtain(null, AppConstants.USER_ADDED_NOTIFIER);
                            msg.setData(bundle);
                            try {
                                if (mClient != null)
                                    mClient.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.USER_REMOVED_NOTIFIER)) {
                            String stationName = msgArray[1];
                            String userAddress[] = msgArray[2].split(":");
                            String userIPString = userAddress[0];
                            int userPort = Integer.parseInt(userAddress[1]);

                            // Check to see if userIP contains '/' and remove it
                            if (userIPString.startsWith("/"))
                                userIPString = userIPString.replaceFirst("/", "");

                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            bundle.putString(AppConstants.USER_IP_KEY, userIPString);
                            bundle.putInt(AppConstants.USER_UDP_PORT_KEY, userPort);
                            Message msg = Message.obtain(null, AppConstants.USER_REMOVED_NOTIFIER);
                            msg.setData(bundle);
                            try {
                                if (mClient != null)
                                    mClient.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.CURRENTLY_PLAYING_NOTIFIER)) {
                            String currentSongPlaying = msgArray[1];
                            String songHolderAddress[] = msgArray[2].split(":");
                            String songHolderIPString = songHolderAddress[0];

                            // Check to see if userIP contains '/' and remove it
                            if (songHolderIPString.startsWith("/"))
                                songHolderIPString = songHolderIPString.replaceFirst("/", "");
                        } else
                            Log.e(AppConstants.APP_TAG, "Unknown message received from server: " + message);
                    } else
                        Log.e(AppConstants.APP_TAG, "Unknown message received from server: " + message);
                } else
                    Log.e(AppConstants.APP_TAG, "Unknown message received from server: " + message);
            }
        }

        private class TCP_Receiver extends Thread{
            //TODO Implement tcp receive
        }
    }

    /* Receive messages from client */
    public class IncomingHandler extends Handler {
        String messageToSend = null;

        public IncomingHandler() {
            sender = new UDP_Sender();

            try {
                serverAddress = InetAddress.getByName(Networking.SERVER_IP_STRING);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AppConstants.MSG_REGISTER_CLIENT:
                    mClient = msg.replyTo;
                    break;
                case AppConstants.MSG_UNREGISTER_CLIENT:
                    mClient = null;
                    break;
                case AppConstants.CREATE_STATION_CMD: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String[] elements = {Networking.CREATE_STATION_CMD, stationName};
                    messageToSend = MessageBuilder.buildMessage(elements, Networking.SEPARATOR);
                    sender.send(messageToSend);
                }
                break;
                case AppConstants.STATION_LIST_REQUEST_CMD: {
                    messageToSend = Networking.STATION_LIST_REQUEST_CMD;
                    sender.send(messageToSend);
                }
                break;
                case AppConstants.JOIN_STATION_CMD: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String[] elements = {Networking.JOIN_STATION_CMD, stationName};
                    messageToSend = MessageBuilder.buildMessage(elements, Networking.SEPARATOR);
                    sender.send(messageToSend);
                }
                break;
                case AppConstants.LEAVE_STATION_CMD: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String[] elements = {Networking.LEAVE_STATION_CMD, stationName};
                    messageToSend = MessageBuilder.buildMessage(elements, Networking.SEPARATOR);
                    sender.send(messageToSend);
                }
                break;
                case AppConstants.ADD_SONG_CMD: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String songName = bundle.getString(AppConstants.SONG_NAME_KEY);
                    String songLength = bundle.getString(AppConstants.SONG_LENGTH_KEY);
                    String[] elements = {Networking.ADD_SONG_CMD, stationName, songName, songLength};
                    messageToSend = MessageBuilder.buildMessage(elements, Networking.SEPARATOR);
                    sender.send(messageToSend);
                }
                break;
                case AppConstants.GET_PLAYLIST_CMD: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String[] elements = {Networking.GET_PLAYLIST_CMD, stationName};
                    messageToSend = MessageBuilder.buildMessage(elements, Networking.SEPARATOR);
                    sender.send(messageToSend);
                }
                break;
                case AppConstants.EXIT_JUKEBOX_NOTIFIER: {
                    Bundle bundle = msg.getData();
                    String exitCommand = Networking.EXIT_JUKEBOX_NOTIFIER;
                    sender.send(exitCommand);
                }
                break;
            }
        }
    }
}