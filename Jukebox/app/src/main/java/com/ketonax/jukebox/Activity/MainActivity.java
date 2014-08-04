package com.ketonax.jukebox.Activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.ketonax.Constants.AppConstants;
import com.ketonax.Constants.Networking;
import com.ketonax.Networking.NetworkingService;
import com.ketonax.jukebox.Adapter.MusicListAdapter;
import com.ketonax.jukebox.Playback.PlayMusicService;
import com.ketonax.jukebox.R;
import com.ketonax.jukebox.Util.MediaUtil;
import com.ketonax.jukebox.Util.Mp3Info;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity implements NavigationDrawerFragment
        .NavigationDrawerCallbacks {

    static Messenger mService;

    /* Display list variables*/
    static ListView stationListView;
    static ListView stationQueueListView;
    static ArrayAdapter<String> stationAdapter;
    static ArrayAdapter<String> stationQueueAdapter;

    static ArrayList<String> stationList = new ArrayList<String>();
    static ArrayList<String> songList = new ArrayList<String>();
    static ArrayList<String> userIPList = new ArrayList<String>();
    static HashMap<String, Integer> udpPortMap = new HashMap<String, Integer>();

    /* Other variables*/
    static String currentStation;
    static HashMap<String, Mp3Info> musicMap = new HashMap<String, Mp3Info>();
    static HashMap<String, String> stationQueueMap = new HashMap<String, String>();

    /* Service Variables */ boolean mIsBound;
    private Messenger mMessenger = new Messenger(new IncomingHandler());

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mService = new Messenger(binder);
            mIsBound = true;

            try {
                Message msg = Message.obtain(null, AppConstants.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
                Log.i(AppConstants.APP_TAG, "Service is connected.");

                Message rqstStationList = Message.obtain(null,
                        AppConstants.STATION_LIST_REQUEST_CMD);
                mService.send(rqstStationList);
            } catch (RemoteException e) {
                /* Service has crashed before anything can be done */
                //e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mIsBound = false;
            //networkService = null;
            Log.i(AppConstants.APP_TAG, "Service is disconnected.");
        }
    };
    /* Views and related variables */ EditText createStationEdit = null;
    /* Fragment managing the behaviors, interactions and presentation of the navigation drawer. */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    /* Used to store the last screen title. For use in {@link #restoreActionBar()}. */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Check for content in savedInstanceState */
        if (savedInstanceState != null) {
            stationList = savedInstanceState.getStringArrayList(AppConstants.STATION_LIST_KEY);
            currentStation = savedInstanceState.getString(AppConstants.CURRENT_STATION_KEY);
            mIsBound = savedInstanceState.getBoolean(AppConstants.SERVICE_CONNECTED_STATUS);
        }

        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager()
                .findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        /* Set up the drawer. */
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id
                .drawer_layout));
    }

    //for rotation
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList(AppConstants.STATION_LIST_KEY, stationList);
        outState.putString(AppConstants.CURRENT_STATION_KEY, currentStation);
        outState.putBoolean(AppConstants.SERVICE_CONNECTED_STATUS, mIsBound);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        doBindService();
    }

    @Override
    protected void onResume() {
        super.onResume();

        /* Reset station list view */
        if (stationAdapter != null) {
            stationAdapter.clear();
            stationAdapter.addAll(stationList);
        }

        /* Reset station queue view */
        if (stationQueueAdapter != null) {
            stationQueueAdapter.clear();
            stationQueueAdapter.addAll(songList);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {

        try {
            exitJukebox();
            doUnbindService();
        } catch (Throwable t) {
            Log.e("MainActivity", "Failed to unbind from the service", t);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        try {
            exitJukebox();
            doUnbindService();
        } catch (Throwable t) {
            Log.e("MainActivity", "Failed to unbind from the service", t);
        }
        super.onBackPressed();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();

        switch (position) {
            case 0:
                fragmentManager.beginTransaction().replace(R.id.container,
                        JoinStationFragment.newInstance(position + 1)).commit();
                break;
            case 1:
                fragmentManager.beginTransaction().replace(R.id.container,
                        MyStationFragment.newInstance(position + 1)).commit();
                break;
            case 2:
                fragmentManager.beginTransaction().replace(R.id.container,
                        AboutInfoFragment.newInstance(position + 1)).commit();
                break;
        }
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                if (currentStation != null) {
                    mTitle = currentStation;
                } else {
                    mTitle = getString(R.string.title_section2);
                }
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            /* Only show items in the action bar relevant to this screen
            if the drawer is not showing. Otherwise, let the drawer
            decide what to show in the action bar. */

            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /** Handle action bar item clicks here. The action bar will
         * automatically handle clicks on the Home/Up button, so long
         * as you specify a parent activity in AndroidManifest.xml. */

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* Button Methods */
    public void createStation(View view) {
        /**
         * Method called when Create button is pressed.
         * Sends the name of the station to be created to the NetworkService.
         */

        String stationToCreate;
        createStationEdit = (EditText) findViewById(R.id.station_name_entry);
        stationToCreate = createStationEdit.getText().toString();

        if (stationToCreate.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please enter a name for the station.",
                    Toast.LENGTH_SHORT).show();
        } else {

            /* Stop media playback */
            stopPlayback();

            /* Send stationToCreate to service */
            try {
                Message msg = Message.obtain(null, AppConstants.CREATE_STATION_CMD);
                Bundle bundle = new Bundle();
                bundle.putString(AppConstants.STATION_NAME_KEY, stationToCreate);
                msg.setData(bundle);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                //e.printStackTrace();
            }
            currentStation = stationToCreate;

            /* Clear songList for previous station*/
            if (stationQueueAdapter != null) {
                songList.clear();
                stationQueueAdapter.clear();
            }

            /* Clear userIPList and udpPortMap for previous station */
            userIPList.clear();
            udpPortMap.clear();
        }

        createStationEdit.setText(null);

        /* Hide the keyboard */
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public void searchLocalMusic(View view) {
        //Intent intent = new Intent(this, MusicList.class);
        //startActivityForResult(intent, AppConstants.ADD_SONG_REQUEST_CODE);

        if (currentStation != null) {
            FragmentManager fm = getFragmentManager();
            ChooseMusicDialog chooseMusic = new ChooseMusicDialog();
            chooseMusic.show(fm, "Choose Music Dialog");
        } else {
            Toast.makeText(getApplicationContext(), "Please join a station.",
                    Toast.LENGTH_SHORT).show();
        }
    }
    /* End of button methods */

    public void startPlayback(String songName, String songPath) {

        /* Stop any currently playing song */
        stopPlayback();
        Intent intent = new Intent(getApplicationContext(), PlayMusicService.class);
        intent.putExtra(PlayMusicService.SONG_NAME, songName);
        intent.putExtra(PlayMusicService.PATH_TO_SONG, songPath);
        startService(intent);
    }

    public void stopPlayback() {
        Intent intent = new Intent(getApplicationContext(), PlayMusicService.class);
        stopService(intent);
    }

    public void leaveCurrentStation() {
        /**
         * This methods sends a command to leave the current station to the Networking service
         */

        try {
            Message msg = Message.obtain(null, AppConstants.LEAVE_STATION_CMD);
            Bundle bundle = new Bundle();
            bundle.putString(AppConstants.STATION_NAME_KEY, currentStation);
            msg.setData(bundle);
            mService.send(msg);
            currentStation = null;
        } catch (RemoteException e) {
            //e.printStackTrace();
        }
    }

    public static void deleteCache(Context context) {
        /** This method deletes the application cache.
         * @param context: The context in which this function is called */

        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean deleteDir(File dir) {
        /** Delete a directory.
         * @param dir: The directory to be deleted */

         if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public void exitJukebox() {
        /**
         * This method sends an exit command to the Networking service
         */

        /* Stop any music currently playing */
        stopPlayback();

        /* Delete application cache */
        deleteCache(this);

        /* Instruct server to remove user from current station and exit the service */
        leaveCurrentStation();
        try {
            Message msg = Message.obtain(null, AppConstants.EXIT_JUKEBOX_NOTIFIER);
            mService.send(msg);
            stationList.clear();
            songList.clear();
            if (stationAdapter != null) {
                stationAdapter.clear();
            }

            if (stationQueueAdapter != null) {
                stationQueueAdapter.clear();
            }

            //TODO Stop background music playback service
        } catch (RemoteException e) {
            //e.printStackTrace();
        }
    }

    /* Private Methods */
    private void doBindService() {
        /** This method establishes a connection with a service */

        bindService(new Intent(this, NetworkingService.class), mConnection,
                Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    private void doUnbindService() {
        /** This method disconnects from a service */

        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, AppConstants.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                    mIsBound = false;
                } catch (RemoteException e) {
                    //e.printStackTrace();
                }
                unbindService(mConnection);
            }
        }
    }

    /* Navigation drawer fragments */
    public static class JoinStationFragment extends Fragment {

        public JoinStationFragment() {
        }

        public static JoinStationFragment newInstance(int sectionNumber) {
            /**
             * Returns a new instance of this fragment for the given section
             * number.
             */

            JoinStationFragment fragment = new JoinStationFragment();
            Bundle args = new Bundle();
            args.putInt(AppConstants.ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.join_station_fragment, container, false);
            showStationListView(rootView);
            return rootView;
        }

        public void showStationListView(final View rootView) {

            stationListView = (ListView) rootView.findViewById(R.id.station_list_view);
            stationListView.addHeaderView(new View(getActivity()));
            stationListView.addFooterView(new View(getActivity()));

            stationAdapter = new ArrayAdapter<String>(getActivity(),
                    R.layout.list_item_card, R.id.text1);
            stationAdapter.addAll(stationList);

            if (stationListView == null) {
                return;
            }
            stationListView.setAdapter(stationAdapter);
            stationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position,
                                        long l) {

                /* Send message to service to send JOIN_STATION_CMD */
                    String stationName = stationList.get(position - 1);
                    joinStation(stationName);
                }
            });
        }

        public void stopPlayback() {
            Intent intent = new Intent(getActivity().getApplicationContext(),
                    PlayMusicService.class);
            getActivity().getApplication().getApplicationContext().stopService(intent);
        }

        public void joinStation(String stationName) {
            /** Send command to server to join a station */

            /* Clear any playlist info for previous station */
            if (stationQueueAdapter != null) {
                stationQueueMap.clear();
                songList.clear();
                stationQueueAdapter.clear();
            }

            /* Clear userIPList and udpPortMap for previous station */
            userIPList.clear();
            udpPortMap.clear();

            /* Stop music playback */
            stopPlayback();

            if (currentStation != null) {

                if (!currentStation.equals(stationName)) {
                    currentStation = stationName;
                    Message msg = Message.obtain(null, AppConstants.JOIN_STATION_CMD);
                    Bundle bundle = new Bundle();
                    bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                    msg.setData(bundle);

                    try {
                        mService.send(msg);
                    } catch (RemoteException e) {
                        //e.printStackTrace();
                    }

                    Toast.makeText(getActivity(), "Joining " + stationName,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "You are already connected to this station ",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                currentStation = stationName;
                Message msg = Message.obtain(null, AppConstants.JOIN_STATION_CMD);
                Bundle bundle = new Bundle();
                bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
                msg.setData(bundle);

                try {
                    mService.send(msg);
                } catch (RemoteException e) {
                    //e.printStackTrace();
                }

                Toast.makeText(getActivity(), "Joining " + stationName, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(getArguments().getInt(AppConstants
                    .ARG_SECTION_NUMBER));
        }
    }

    public static class MyStationFragment extends Fragment {
        /**
         * A placeholder fragment containing a simple view.
         */

        public MyStationFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static MyStationFragment newInstance(int sectionNumber) {
            MyStationFragment fragment = new MyStationFragment();
            Bundle args = new Bundle();
            args.putInt(AppConstants.ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.my_station_fragment, container, false);
            showStationQueue(rootView);
            return rootView;
        }

        public void showStationQueue(final View rootView) {
            stationQueueListView = (ListView) rootView.findViewById(R.id.song_queue_id);
            stationQueueAdapter = new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_list_item_1, android.R.id.text1);
            stationQueueAdapter.addAll(songList);
            stationQueueListView.setAdapter(stationQueueAdapter);

            if (stationQueueListView == null) {
                return;
            }
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(getArguments().getInt(AppConstants
                    .ARG_SECTION_NUMBER));
        }
    }

    public static class AboutInfoFragment extends Fragment {
        /**
         * A placeholder fragment containing a simple view.
         */

        public AboutInfoFragment() {
        }

        public static AboutInfoFragment newInstance(int sectionNumber) {
            /**
             * Returns a new instance of this fragment for the given section
             * number.
             */

            AboutInfoFragment fragment = new AboutInfoFragment();
            Bundle args = new Bundle();
            args.putInt(AppConstants.ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.about_info_fragment, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(getArguments().getInt(AppConstants
                    .ARG_SECTION_NUMBER));
        }
    }

    public static class ChooseMusicDialog extends DialogFragment {

        MediaUtil music = new MediaUtil();
        MusicListAdapter listAdapter;
        private ListView mMusiclist;
        private List<Mp3Info> mp3Infos = null;

        public ChooseMusicDialog() {

        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View view = inflater.inflate(R.layout.music_list, container);
            getDialog().setTitle(R.string.title_choose_music);

            mMusiclist = (ListView) view.findViewById(R.id.local_music_list_view);

            /* Set List Adapter */
            mp3Infos = MediaUtil.getMp3Infos(getActivity());
            listAdapter = new MusicListAdapter(getActivity(), mp3Infos);
            mMusiclist.setAdapter(listAdapter);
            mMusiclist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position,
                                        long l) {
                    Mp3Info selectedSong = mp3Infos.get(position);
                    String songName = selectedSong.getTitle();
                    long songLength = selectedSong.getDuration();

                    /* Add selected song to musicMap */
                    musicMap.put(songName, selectedSong);

                    /* Instruct service to send command to the server to add the song to the
                    station */
                    Message msg = Message.obtain(null, AppConstants.ADD_SONG_CMD);
                    Bundle bundle = new Bundle();
                    bundle.putString(AppConstants.STATION_NAME_KEY, currentStation);
                    bundle.putString(AppConstants.SONG_NAME_KEY, songName);
                    bundle.putString(AppConstants.SONG_LENGTH_KEY, Long.toString(songLength));
                    msg.setData(bundle);
                    try {
                        mService.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    String toastMessage = "Added " + songName + " to " + currentStation + " queue.";
                    Toast.makeText(getActivity(), toastMessage, Toast.LENGTH_SHORT).show();

                    dismiss();
                }
            });

            return view;
        }
    }

    /* Receive messages from client */
    public class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AppConstants.STATION_LIST_REQUEST_RESPONSE: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    if (!stationList.contains(stationName)) {
                        stationList.add(stationName);
                        stationAdapter.add(stationName);
                        Log.i(AppConstants.APP_TAG, "Added " + stationName + " to station list.");
                    }
                }
                break;
                case AppConstants.STATION_ADDED_NOTIFIER: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    if (!stationList.contains(stationName)) {
                        stationList.add(stationName);
                        stationAdapter.add(stationName);
                        Log.i(AppConstants.APP_TAG, "Added " + stationName + " to station list.");
                    }
                }
                break;
                case AppConstants.STATION_KILLED_NOTIFIER: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);

                    /* Reset current station if it has been removed from the server*/
                    if (currentStation != null) {
                        if (currentStation.equals(stationName)) {
                            currentStation = null;
                        }
                    }
                    stationList.remove(stationName);
                    stationAdapter.remove(stationName);
                    Log.i(AppConstants.APP_TAG, "Removed " + stationName + " from station list.");
                }
                break;
                case AppConstants.SONG_ADDED_NOTIFIER: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String songName = bundle.getString(AppConstants.SONG_NAME_KEY);
                    if (currentStation.equals(stationName)) {
                        if (!songList.contains(songName)) {
                            songList.add(songName);

                            if (stationQueueAdapter != null) {
                                stationQueueAdapter.add(songName);
                            }
                        }
                        Log.i(AppConstants.APP_TAG, "Added  " + songName + " to " +
                                currentStation + " play queue.");
                    }
                }
                break;
                case AppConstants.SONG_REMOVED_NOTIFIER: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String songName = bundle.getString(AppConstants.SONG_NAME_KEY);
                    if (currentStation.equals(stationName)) {
                        songList.remove(songName);
                        if (stationQueueAdapter != null) {
                            stationQueueAdapter.remove(songName);
                        }
                        Log.i(AppConstants.APP_TAG, "Removed  " + songName + " from " +
                                currentStation + " play queue.");
                    }
                }
                break;
                case AppConstants.SONG_ON_LIST_RESPONSE: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String songName = bundle.getString(AppConstants.SONG_NAME_KEY);
                    if (currentStation.equals(stationName)) {
                        songList.add(songName);
                        if (stationQueueAdapter != null) {
                            stationQueueAdapter.add(songName);
                        }
                        Log.i(AppConstants.APP_TAG, "Added " + songName + " to station: " +
                                currentStation + " play queue.");
                    }
                }
                break;
                case AppConstants.USER_ADDED_NOTIFIER: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String userIP = bundle.getString(AppConstants.USER_IP_KEY);
                    int udpPort = bundle.getInt(AppConstants.USER_UDP_PORT_KEY);

                    String deviceIP = Networking.getLocalIP();
                    Log.d(AppConstants.APP_TAG, "This device IP is: " + deviceIP);

                    if (currentStation.equals(stationName) && !userIPList.contains(userIP) &&
                            !userIP.equals(deviceIP)) {
                        userIPList.add(userIP);
                        udpPortMap.put(userIP, udpPort);
                        Log.i(AppConstants.APP_TAG, "Added user at " + userIP + " to station user" +
                                " list.");
                    }
                }
                break;
                case AppConstants.USER_REMOVED_NOTIFIER: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String userIP = bundle.getString(AppConstants.USER_IP_KEY);
                    int udpPort = bundle.getInt(AppConstants.USER_UDP_PORT_KEY);
                    if (currentStation.equals(stationName)) {
                        userIPList.remove(userIP);
                        udpPortMap.remove(userIP);
                        Log.i(AppConstants.APP_TAG, "Removed user at " + userIP + " from station " +
                                "user list.");
                    }
                }
                break;
                case AppConstants.USER_ON_LIST_RESPONSE: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String userIP = bundle.getString(AppConstants.USER_IP_KEY);
                    int udpPort = bundle.getInt(AppConstants.USER_UDP_PORT_KEY);

                    String deviceIP = Networking.getLocalIP();

                    if (currentStation.equals(stationName) && !userIPList.contains(userIP) &&
                            !userIP.equals(deviceIP)) {
                        userIPList.add(userIP);
                        udpPortMap.put(userIP, udpPort);
                        Log.i(AppConstants.APP_TAG, "Added user at " + userIP + " to station user" +
                                " list.");
                    }
                }
                break;
                case AppConstants.CURRENTLY_PLAYING_NOTIFIER: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String ownerIP = bundle.getString(AppConstants.USER_IP_KEY);
                    String songName = bundle.getString(AppConstants.SONG_NAME_KEY);

                    if (stationName.equals(currentStation)) {

                        if (ownerIP.equals(Networking.getLocalIP())) {

                        /* Local music playback */
                            Mp3Info song = musicMap.get(songName);
                            String songPath = song.getUrl();

                        /* Start playing song */
                            startPlayback(songName, songPath);
                        } else {
                            String songPath = stationQueueMap.get(songName);

                        /* Start playing song */
                            startPlayback(songName, songPath);
                        }
                        Log.i(AppConstants.APP_TAG, songName + " is currently playing on station:" +
                                " " + currentStation);
                    } else {
                        Log.e(AppConstants.APP_TAG, "Can't play song from station: " +
                                stationName + " because user is on station: " + currentStation);
                    }
                }
                break;
                case AppConstants.SEND_SONG_CMD: {
                    Log.i(AppConstants.APP_TAG, "Received " + Networking.SEND_SONG_CMD + " from " +
                            "station: " + currentStation);
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String songName = bundle.getString(AppConstants.SONG_NAME_KEY);

                    if (stationName.equals(currentStation)) {

                        /* Send song to other devices */
                        Mp3Info song = musicMap.get(songName);

                        for (String userIP : userIPList) {
                            NetworkingService.sendSong(userIP, song);
                        }

                        Log.i(AppConstants.APP_TAG, "Sending songs to users on " + currentStation);
                    } else {
                        Log.e(AppConstants.APP_TAG, "Can't send song from station: " +
                                stationName + " because user is at station: " + currentStation);
                    }
                }
                break;
                case AppConstants.SONG_DOWNLOADED: {
                    Bundle bundle = msg.getData();
                    String songName = bundle.getString(AppConstants.SONG_NAME_KEY);
                    String songPath = bundle.getString(AppConstants.SONG_PATH_KEY);

                    /* Put song name and path into statioQueueMap */
                    stationQueueMap.put(songName, songPath);

                    /* Play the downloaded song */
                    // Toast.makeText(getApplicationContext(), "Current song on queue downloaded.",
                    //Toast.LENGTH_SHORT).show();
                    //startPlayback(songName, songPath);

                    Bundle responseBundle = new Bundle();
                    responseBundle.putString(AppConstants.STATION_NAME_KEY, currentStation);
                    Message msgResponse = Message.obtain(null,
                            AppConstants.SONG_DOWNLOADED_NOTIFIER);
                    msgResponse.setData(responseBundle);
                    try {
                        if (mService != null) {
                            mService.send(msgResponse);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    Log.i(AppConstants.APP_TAG, "Sent " + Networking.SONG_DOWNLOADED_NOTIFIER +
                            " to station:" + currentStation);
                }
                break;
            }
        }
    }
}

