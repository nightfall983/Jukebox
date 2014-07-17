package com.ketonax.Networking;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.ketonax.Constants.AppConstants;
import com.ketonax.Constants.ServiceConstants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by nightfall on 7/13/14.
 */
public class NetworkingService extends Service {
    /* Network Variables */
    private DatagramSocket udpSocket = null;
    private InetAddress serverAddress = null;
    private int serverPort = ServiceConstants.SERVER_PORT;
    boolean keepRunning = true;

    /* Jukebox Radio Variables */
    ArrayList<String> stationList = new ArrayList<String>();
    ArrayList<String> songList = new ArrayList<String>();
    HashMap<String, Integer> userMap = new HashMap<String, Integer>();
    String currentStation = null;
    String currentSongPlaying = null;
    String currentSongHolder = null;
    String songToPlay = null;
    UDP_Sender sender = null;
    Receiver receiver = null;

    Messenger mMessenger = new Messenger(new IncomingHandler());
    Messenger mClient;

    public NetworkingService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }



    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        keepRunning = false;
        super.onDestroy();

    }

    /*Used for sending data to Jukebox Server*/
    public class UDP_Sender {
        private byte[] sendData = null;
        private AsyncTask<Void, Void, Void> asyncSender;
        private InetAddress serverAddress = null;
        private DatagramPacket sendPacket = null;

        public void send(final String message) {
            asyncSender = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {

                    try {
                        serverAddress = InetAddress.getByName(ServiceConstants.SERVER_IP_STRING);
                        //udpSocket = ServiceConstants.getSocket();
                        udpSocket.connect(serverAddress, ServiceConstants.SERVER_PORT);
                        sendData = message.getBytes();
                        sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, ServiceConstants.SERVER_PORT);
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
            else asyncSender.execute();
        }
    }

    private class Receiver extends Thread {

        public Receiver() {
        }

        @Override
        public void run() {
            /* Request the current station list */
            String command = ServiceConstants.STATION_LIST_REQUEST_CMD;
            sender.send(command);

            while (keepRunning) {
                byte[] receiveData = new byte[ServiceConstants.DATA_LIMIT_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                try {
                    udpSocket.receive(receivePacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String message = new String(receivePacket.getData()).trim();

                /* TODO parse messages and respond accordingly */
                if (message.startsWith(ServiceConstants.JUKEBOX_MESSAGE_IDENTIFIER)) {

                    if (message.contains(ServiceConstants.SEPARATOR)) {
                        String[] msgArray = message.split(ServiceConstants.SEPARATOR);

                        if (msgArray[0].equals(ServiceConstants.PLAY_SONG_CMD)) {
                            String songName = msgArray[1];
                            Message msg = Message.obtain(null, AppConstants.PLAY_SONG_CMD, songName);
                            try {
                                mClient.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(ServiceConstants.STATION_LIST_REQUEST_RESPONSE)) {
                            String stationName = msgArray[1];
                            Bundle bundle = new Bundle();
                            bundle.putString(ServiceConstants.STATION_LIST_REQUEST_RESPONSE, stationName);
                            Message msg = Message.obtain(null, AppConstants.STATION_LIST_REQUEST_RESPONSE);
                            msg.setData(bundle);
                            try {
                                mClient.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(ServiceConstants.STATION_KILLED_NOTIFIER)) {
                            String stationName = msgArray[1];
                            Bundle bundle = new Bundle();
                            bundle.putString(ServiceConstants.STATION_KILLED_NOTIFIER, stationName);
                            Message msg = Message.obtain(null, AppConstants.STATION_KILLED_NOTIFIER);
                            msg.setData(bundle);
                            try {
                                mClient.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(ServiceConstants.STATION_ADDED_NOTIFIER)) {
                            String stationName = msgArray[1];
                            if (stationList.contains(stationName))
                                stationList.add(stationName);
                            Bundle bundle = new Bundle();
                            bundle.putString(ServiceConstants.STATION_ADDED_NOTIFIER, stationName);
                            Message msg = Message.obtain(null, AppConstants.STATION_ADDED_NOTIFIER);
                            msg.setData(bundle);
                            try {
                                mClient.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }  else if (msgArray[0].equals(ServiceConstants.SONG_ADDED_NOTIFIER)) {
                            String songName = msgArray[1];
                            songList.add(songName);
                        } else if (msgArray[0].equals(ServiceConstants.SONG_REMOVED_NOTIFIER)) {
                            String stationName = msgArray[1];
                            String songName = msgArray[2];

                            /* Remove songName if it belongs to current station */
                            if (currentStation.equals(stationName) && songList.contains(songName))
                                songList.remove(songName);

                        } else if (msgArray[0].equals(ServiceConstants.USER_ON_LIST_RESPONSE)) {
                            //Empty current map
                            userMap.clear();

                            String stationName = msgArray[1];
                            String userAddress[] = msgArray[2].split(":");
                            String userIPString = userAddress[0];
                            int userPort = Integer.parseInt(userAddress[1]);

                            // Check to see if userIP contains '/' and remove it
                            if (userIPString.startsWith("/"))
                                userIPString = userIPString.replaceFirst("/", "");

                            /* Add userIPString if it belongs to current station*/
                            if (currentStation.equals(stationName) && !userMap.containsKey(userIPString))
                                userMap.put(userIPString, userPort);

                        } else if (msgArray[0].equals(ServiceConstants.USER_ADDED_NOTIFIER)) {
                            String stationName = msgArray[1];
                            String userAddress[] = msgArray[2].split(":");
                            String userIPString = userAddress[0];
                            int userPort = Integer.parseInt(userAddress[1]);

                            // Check to see if userIP contains '/' and remove it
                            if (userIPString.startsWith("/"))
                                userIPString = userIPString.replaceFirst("/", "");

                            /* Add userIPString if it belongs to current station*/
                            if (currentStation.equals(stationName) && !userMap.containsKey(userIPString))
                                userMap.put(userIPString, userPort);

                        } else if (msgArray[0].equals(ServiceConstants.USER_REMOVED_NOTIFIER)) {
                            String stationName = msgArray[1];
                            String userAddress[] = msgArray[2].split(":");
                            String userIPString = userAddress[0];
                            int userPort = Integer.parseInt(userAddress[1]);

                            // Check to see if userIP contains '/' and remove it
                            if (userIPString.startsWith("/"))
                                userIPString = userIPString.replaceFirst("/", "");

                            /* Add userIPString if it belongs to current station*/
                            if (currentStation.equals(stationName) && userMap.containsKey(userIPString))
                                userMap.remove(userIPString);

                        } else if (msgArray[0].equals(ServiceConstants.CURRENTLY_PLAYING_NOTIFIER)) {
                            currentSongPlaying = msgArray[1];
                            currentSongHolder = msgArray[2];
                        }
                    } else {
                        //TODO handle unknown message
                    }
                } else {
                    //TODO handle unknown message
                }
            }
        }
    }

    /* Receive messages from client */
    public class IncomingHandler extends Handler {
        String messageToSend = null;

        public IncomingHandler() {
            sender = new UDP_Sender();
            try {
                udpSocket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }

            try {
                serverAddress = InetAddress.getByName(ServiceConstants.SERVER_IP_STRING);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            receiver = new Receiver();
            receiver.start();
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
                    String stationName = bundle.getString(ServiceConstants.CREATE_STATION_CMD);
                    String[] elements = {ServiceConstants.CREATE_STATION_CMD, stationName};
                    messageToSend = MessageBuilder.buildMessage(elements, ServiceConstants.SEPARATOR);
                    sender.send(messageToSend);
                    currentStation = stationName;
                }
                break;
                case AppConstants.STATION_LIST_REQUEST_CMD: {
                    //TODO
                }
                break;
                case AppConstants.JOIN_STATION_CMD:{
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(ServiceConstants.JOIN_STATION_CMD);
                    String[] elements = {ServiceConstants.JOIN_STATION_CMD, stationName};
                    messageToSend = MessageBuilder.buildMessage(elements, ServiceConstants.SEPARATOR);
                    sender.send(messageToSend);
                    currentStation = stationName;
                }
                    break;
                case AppConstants.LEAVE_STATION_CMD:{
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(ServiceConstants.LEAVE_STATION_CMD);
                    String[] elements = {ServiceConstants.LEAVE_STATION_CMD, stationName};
                    messageToSend = MessageBuilder.buildMessage(elements, ServiceConstants.SEPARATOR);
                    sender.send(messageToSend);
                }
                    break;
                case AppConstants.ADD_SONG_CMD:
                    break;
                case AppConstants.GET_PLAYLIST_CMD:
                    break;
                case AppConstants.EXIT_JUKEBOX_NOTIFIER:{
                    Bundle bundle = msg.getData();
                    String exitCommand = bundle.getString(ServiceConstants.EXIT_JUKEBOX_NOTIFIER);
                    sender.send(exitCommand);
                }
                    break;
            }
        }
    }
}
