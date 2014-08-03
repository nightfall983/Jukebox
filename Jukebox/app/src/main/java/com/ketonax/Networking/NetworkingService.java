package com.ketonax.Networking;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
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
import com.ketonax.jukebox.Util.Mp3Info;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by nightfall on 7/13/14.
 */

public class NetworkingService extends Service {

    static Messenger mClient;
    private final IBinder mBinder = new MyBinder();
    boolean keepRunning = true;
    private UDP_Sender sender = null;
    private UDP_Receiver receiver = null;
    private GroupReceiver groupReceiver = null;
    private ServerSocket serverSocket = null;
    private TCP_Receiver tcpReceiver = null;
    private Messenger mMessenger = new Messenger(new IncomingHandler());
    /* Network Variables */
    private DatagramSocket udpSocket = null;
    private MulticastSocket multicastSocket = null;
    private InetAddress serverAddress = null;

    public NetworkingService() {

    }

    public static void sendSong(String userIP, Mp3Info song) {

        new TCP_Sender(userIP, song).start();
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
        receiver = new UDP_Receiver();
        receiver.start();
        tcpReceiver = new TCP_Receiver();
        tcpReceiver.start();
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

        if (!serverSocket.isClosed()) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    private static class TCP_Sender extends Thread {

        Socket socket;
        boolean isConnected = false;
        ObjectOutputStream outputStream;
        FileEvent fileEvent;
        String successStatus = "Success";
        String errorStatus = "Error";
        String userIP;
        Mp3Info song;

        public TCP_Sender(final String userIP, final Mp3Info song) {
            this.userIP = userIP;
            this.song = song;
        }

        @Override
        public void run() {
            connect();

            if (isConnected) {
                send();
            }
        }

        private void send() {

            fileEvent = new FileEvent();
            String fileName = Uri.parse(song.getUrl()).getLastPathSegment();
            fileEvent.setFilename(fileName);
            File file = new File(song.getUrl());

                    /* Check file validity */
            if (file.isFile()) {

                try {
                    DataInputStream inputStream = new DataInputStream(new FileInputStream(file));
                    long length = (int) file.length();
                    byte[] fileBytes = new byte[(int) length];
                    int read = 0;
                    int numRead = 0;
                    while (read < fileBytes.length && (numRead = inputStream.read(fileBytes,
                            read, fileBytes.length - read)) >= 0) {
                        read = read + numRead;
                    }
                    fileEvent.setSongName(song.getTitle());
                    fileEvent.setFileSize(length);
                    fileEvent.setFileData(fileBytes);
                    fileEvent.setStatus(successStatus);
                } catch (Exception e) {
                    e.printStackTrace();
                    fileEvent.setStatus(errorStatus);
                }
            } else {
                Log.e(AppConstants.APP_TAG, song.getUrl() + " is not a path to a file.");
                fileEvent.setStatus(errorStatus);
            }

                    /* Write fileEvent to serverSocket */
            try {
                if (isConnected) {
                    outputStream.writeObject(fileEvent);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void connect() {

            if (!isConnected) {
                try {
                    Log.d(AppConstants.APP_TAG, "Trying to send song to user at: " + userIP);
                    socket = new Socket(userIP, Networking.TCP_PORT_NUMBER);
                    outputStream = new ObjectOutputStream(socket.getOutputStream());
                    isConnected = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class MyBinder extends Binder {
        public NetworkingService getService() {
            return NetworkingService.this;
        }
    }

    private class UDP_Sender {
        /**
         * Used for sending data to Jukebox Server
         */
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
                        sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress,
                                Networking.SERVER_PORT);
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

            if (Build.VERSION.SDK_INT >= 11) {
                asyncSender.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                asyncSender.execute();
            }
        }
    }

    private class UDP_Receiver extends Thread {

        public UDP_Receiver() {
        }

        @Override
        public void run() {

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
                            Message msg = Message.obtain(null,
                                    AppConstants.STATION_KILLED_NOTIFIER);
                            msg.setData(bundle);
                            try {
                                if (mClient != null) {
                                    mClient.send(msg);
                                }
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
                                if (mClient != null) {
                                    mClient.send(msg);
                                }
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
                                if (mClient != null) {
                                    mClient.send(msg);
                                }
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
                                if (mClient != null) {
                                    mClient.send(msg);
                                }
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
                                if (mClient != null) {
                                    mClient.send(msg);
                                }
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
                            if (userIPString.startsWith("/")) {
                                userIPString = userIPString.replaceFirst("/", "");
                            }

                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            bundle.putString(AppConstants.USER_IP_KEY, userIPString);
                            bundle.putInt(AppConstants.USER_UDP_PORT_KEY, userPort);
                            Message msg = Message.obtain(null, AppConstants.USER_ADDED_NOTIFIER);
                            msg.setData(bundle);
                            try {
                                if (mClient != null) {
                                    mClient.send(msg);
                                }
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
                            if (userIPString.startsWith("/")) {
                                userIPString = userIPString.replaceFirst("/", "");
                            }

                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            bundle.putString(AppConstants.USER_IP_KEY, userIPString);
                            bundle.putInt(AppConstants.USER_UDP_PORT_KEY, userPort);
                            Message msg = Message.obtain(null, AppConstants.USER_REMOVED_NOTIFIER);
                            msg.setData(bundle);
                            try {
                                if (mClient != null) {
                                    mClient.send(msg);
                                }
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.CURRENTLY_PLAYING_NOTIFIER)) {
                            Log.i(AppConstants.APP_TAG, Networking.CURRENTLY_PLAYING_NOTIFIER);
                            String stationName = msgArray[1];
                            String currentSongPlaying = msgArray[2];
                            String songHolderAddress[] = msgArray[3].split(":");
                            String songHolderIPString = songHolderAddress[0];

                            // Check to see if userIP contains '/' and remove it
                            if (songHolderIPString.startsWith("/")) {
                                songHolderIPString = songHolderIPString.replaceFirst("/", "");
                            }
                            Message msg = Message.obtain(null,
                                    AppConstants.CURRENTLY_PLAYING_NOTIFIER);
                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            bundle.putString(AppConstants.SONG_NAME_KEY, currentSongPlaying);
                            bundle.putString(AppConstants.USER_IP_KEY, songHolderIPString);
                            msg.setData(bundle);
                            try {
                                if (mClient != null) {
                                    mClient.send(msg);
                                }
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.SEND_SONG_CMD)) {
                            Log.i(AppConstants.APP_TAG, "Received send song command.");
                            String stationName = msgArray[1];
                            String songName = msgArray[2];
                            Message msg = Message.obtain(null, AppConstants.SEND_SONG_CMD);
                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            bundle.putString(AppConstants.SONG_NAME_KEY, songName);
                            msg.setData(bundle);
                            try {
                                if (mClient != null) {
                                    mClient.send(msg);
                                }
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.STATION_LIST_REQUEST_RESPONSE)) {
                            Log.i(AppConstants.APP_TAG, Networking.STATION_LIST_REQUEST_RESPONSE);
                            String stationName = msgArray[1];
                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            Message msg = Message.obtain(null,
                                    AppConstants.STATION_LIST_REQUEST_RESPONSE);
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
                            if (userIPString.startsWith("/")) {
                                userIPString = userIPString.replaceFirst("/", "");
                            }

                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            bundle.putString(AppConstants.USER_IP_KEY, userIPString);
                            bundle.putInt(AppConstants.USER_UDP_PORT_KEY, userPort);
                            Message msg = Message.obtain(null, AppConstants.USER_ON_LIST_RESPONSE);
                            msg.setData(bundle);
                            try {
                                if (mClient != null) {
                                    mClient.send(msg);
                                }
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        Log.e(AppConstants.APP_TAG, "Unknown message received from server: " +
                                message);
                    }
                } else {
                    Log.e(AppConstants.APP_TAG, "Unknown message received from server: " + message);
                }
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
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            Message msg = Message.obtain(null,
                                    AppConstants.STATION_KILLED_NOTIFIER);
                            msg.setData(bundle);
                            try {
                                if (mClient != null) {
                                    mClient.send(msg);
                                }
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.STATION_ADDED_NOTIFIER)) {
                            Log.d(AppConstants.APP_TAG, Networking.STATION_ADDED_NOTIFIER);
                            String stationName = msgArray[1];

                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            Message msg = Message.obtain(null, AppConstants.STATION_ADDED_NOTIFIER);
                            msg.setData(bundle);
                            try {
                                if (mClient != null) {
                                    mClient.send(msg);
                                }
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
                            if (userIPString.startsWith("/")) {
                                userIPString = userIPString.replaceFirst("/", "");
                            }

                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            bundle.putString(AppConstants.USER_IP_KEY, userIPString);
                            bundle.putInt(AppConstants.USER_UDP_PORT_KEY, userPort);
                            Message msg = Message.obtain(null, AppConstants.USER_ADDED_NOTIFIER);
                            msg.setData(bundle);
                            try {
                                if (mClient != null) {
                                    mClient.send(msg);
                                }
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.USER_REMOVED_NOTIFIER)) {
                            String stationName = msgArray[1];
                            String userAddress[] = msgArray[2].split(":");
                            String userIPString = userAddress[0];
                            int userPort = Integer.parseInt(userAddress[1]);

                            // Check to see if userIP contains '/' and remove it
                            if (userIPString.startsWith("/")) {
                                userIPString = userIPString.replaceFirst("/", "");
                            }

                            Bundle bundle = new Bundle();
                            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                            bundle.putString(AppConstants.USER_IP_KEY, userIPString);
                            bundle.putInt(AppConstants.USER_UDP_PORT_KEY, userPort);
                            Message msg = Message.obtain(null, AppConstants.USER_REMOVED_NOTIFIER);
                            msg.setData(bundle);
                            try {
                                if (mClient != null) {
                                    mClient.send(msg);
                                }
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else if (msgArray[0].equals(Networking.CURRENTLY_PLAYING_NOTIFIER)) {
                            String currentSongPlaying = msgArray[1];
                            String songHolderAddress[] = msgArray[2].split(":");
                            String songHolderIPString = songHolderAddress[0];

                            // Check to see if userIP contains '/' and remove it
                            if (songHolderIPString.startsWith("/")) {
                                songHolderIPString = songHolderIPString.replaceFirst("/", "");
                            }
                        } else {
                            Log.e(AppConstants.APP_TAG, "Unknown message received from server: "
                                    + message);
                        }
                    } else {
                        Log.e(AppConstants.APP_TAG, "Unknown message received from server: " +
                                message);
                    }
                } else {
                    Log.e(AppConstants.APP_TAG, "Unknown message received from server: " + message);
                }
            }
        }
    }

    private class TCP_Receiver extends Thread {

        String statusSuccess = "Success";
        String statusError = "Error";
        private ObjectInputStream objectInputStream = null;
        private boolean isConnected = false;
        private FileEvent fileEvent;
        private File downloadedFile;
        private FileOutputStream fileOutputStream = null;

        public TCP_Receiver() {

        }

        @Override
        public void run() {

            Log.d(AppConstants.APP_TAG, "TCP receiver has started.");

            try {
                serverSocket = new ServerSocket(Networking.TCP_PORT_NUMBER);
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (keepRunning) {

                /* Connect serverSocket and initialize objectInputStream */
                try {

                    Socket connectionSocket = serverSocket.accept();
                    objectInputStream = new ObjectInputStream(connectionSocket.getInputStream());
                    isConnected = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (isConnected) {
                    downloadFile();
                }
            }
        }

        private void downloadFile() {

            try {
                fileEvent = (FileEvent) objectInputStream.readObject();
                if (fileEvent.getStatus().equals(statusError)) {
                    Log.e(AppConstants.APP_TAG, "Download error occurred");
                } else {
                    String fileName = fileEvent.getFilename();
                    downloadedFile = File.createTempFile(fileName, null,
                            getApplicationContext().getCacheDir());

                        /* Save downloaded file */
                    saveFile();

                        /* Give MainActivity the song name and path */
                    songDownloadedNotifier();
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void saveFile() {
            /** Writes file to temporary cache storage if it doesn't already exist.*/

            try {
                fileOutputStream = new FileOutputStream(downloadedFile);
                fileOutputStream.write(fileEvent.getFileData());
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void songDownloadedNotifier() {
            /**This method notifies the main activity that the current song to be played has been
             *  downloaded.
             * */

            Message msg = Message.obtain(null, AppConstants.SONG_DOWNLOADED);
            Bundle bundle = new Bundle();
            bundle.putString(AppConstants.SONG_NAME_KEY, fileEvent.getSongName());
            bundle.putString(AppConstants.SONG_PATH_KEY, downloadedFile.getPath());
            msg.setData(bundle);
            try {
                if (mClient != null) {
                    mClient.send(msg);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
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
                    String[] elements = {Networking.ADD_SONG_CMD, stationName, songName,
                            songLength};
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
                case AppConstants.SONG_DOWNLOADED_NOTIFIER: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String[] elements = {Networking.SONG_DOWNLOADED_NOTIFIER, stationName};
                    String notifier = MessageBuilder.buildMessage(elements, Networking.SEPARATOR);
                    sender.send(notifier);
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