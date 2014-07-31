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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.ketonax.Constants.AppConstants;
import com.ketonax.Constants.Networking;
import com.ketonax.Networking.NetworkingService;
import com.ketonax.jukebox.Adapter.MusicListAdapter;
import com.ketonax.jukebox.R;
import com.ketonax.jukebox.Util.MediaUtil;
import com.ketonax.jukebox.Util.Mp3Info;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

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

    /* Service Variables */ boolean mIsBound;
    private Messenger mMessenger = new Messenger(new IncomingHandler());
    private NetworkingService networkService= null;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mService = new Messenger(binder);
            mIsBound = true;

            try {
                Message msg = Message.obtain(null, AppConstants.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

                Message rqstStationList = Message.obtain(null, AppConstants.STATION_LIST_REQUEST_CMD);
                mService.send(rqstStationList);
                Log.i(AppConstants.APP_TAG, "Service is connected.");
            } catch (RemoteException e) {
                /* Service has crashed before anything can be done */
                //e.printStackTrace();
            }

            NetworkingService.MyBinder b = (NetworkingService.MyBinder) binder;
            networkService= b.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mIsBound = false;
            networkService = null;
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

        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        /* Set up the drawer. */
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
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
        stationAdapter.clear();
        stationAdapter.addAll(stationList);

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
            unbindService();
        } catch (Throwable t) {
            Log.e("MainActivity", "Failed to unbind from the service", t);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        try {
            exitJukebox();
            unbindService();
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
                fragmentManager.beginTransaction().replace(R.id.container, JoinStationFragment.newInstance(position + 1)).commit();
                break;
            case 1:
                fragmentManager.beginTransaction().replace(R.id.container, MyStationFragment.newInstance(position + 1)).commit();
                break;
            case 2:
                fragmentManager.beginTransaction().replace(R.id.container, AboutInfoFragment.newInstance(position + 1)).commit();
                break;
        }
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                if (currentStation != null)
                    mTitle = currentStation;
                else
                    mTitle = getString(R.string.title_section2);
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
            Toast.makeText(getApplicationContext(), "Please enter a name for the station.", Toast.LENGTH_SHORT).show();
        } else {
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
    }

    public void searchLocalMusic(View view) {
        //Intent intent = new Intent(this, MusicList.class);
        //startActivityForResult(intent, AppConstants.ADD_SONG_REQUEST_CODE);

        if (currentStation != null) {
            FragmentManager fm = getFragmentManager();
            ChooseMusicDialog chooseMusic = new ChooseMusicDialog();
            chooseMusic.show(fm, "Choose Music Dialog");
        } else
            Toast.makeText(getApplicationContext(), "Please join a station.", Toast.LENGTH_SHORT).show();
    }
    /* End of button methods */

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

    public void exitJukebox() {
        /**
         * This method sends an exit command to the Networking service
         */

        leaveCurrentStation();

        try {
            Message msg = Message.obtain(null, AppConstants.EXIT_JUKEBOX_NOTIFIER);
            mService.send(msg);
            stationList.clear();
            stationAdapter.clear();
            songList.clear();
            stationQueueAdapter.clear();
            //TODO Stop background music playback service
        } catch (RemoteException e) {
            //e.printStackTrace();
        }
    }

    /* Private Methods */
    private void doBindService() {
        /** This method establishes a connection with a service */

        bindService(new Intent(this, NetworkingService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    private void unbindService() {
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
            }

            unbindService(mConnection);
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
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.join_station_fragment, container, false);
            showStationListView(rootView);
            return rootView;
        }

        public void showStationListView(final View rootView) {

            stationListView = (ListView) rootView.findViewById(R.id.station_list_view);
            stationAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1);
            stationAdapter.addAll(stationList);

            if (stationListView == null) {
                return;
            }
            stationListView.setAdapter(stationAdapter);
            stationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                /* */
                /* Send message to service to send JOIN_STATION_CMD */
                    String stationName = stationList.get(position);
                    joinStation(stationName);
                }
            });
        }

        public void joinStation(String stationName) {

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

                    Toast.makeText(getActivity(), "Joining " + stationName, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "You are already connected to this station ", Toast.LENGTH_SHORT).show();
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

            /* Clear songList for previous station */
            if (stationQueueAdapter != null) {
                songList.clear();
                stationQueueAdapter.clear();
            }

            /* Send request for new song queue */
            /*Message msg = Message.obtain(null, AppConstants.GET_PLAYLIST_CMD);
            Bundle bundle = new Bundle();
            bundle.putString(AppConstants.STATION_NAME_KEY, stationName);
            msg.setData(bundle); */

            /* Clear userIPList and udpPortMap for previous station */
            userIPList.clear();
            udpPortMap.clear();
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(getArguments().getInt(AppConstants.ARG_SECTION_NUMBER));
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
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.my_station_fragment, container, false);
            showStationQueue(rootView);
            return rootView;
        }

        public void showStationQueue(final View rootView) {
            stationQueueListView = (ListView) rootView.findViewById(R.id.song_queue_id);
            stationQueueAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1);
            stationQueueAdapter.addAll(songList);
            stationQueueListView.setAdapter(stationQueueAdapter);

            if (stationQueueListView == null) {
                return;
            }
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(getArguments().getInt(AppConstants.ARG_SECTION_NUMBER));
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
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.about_info_fragment, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(getArguments().getInt(AppConstants.ARG_SECTION_NUMBER));
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
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View view = inflater.inflate(R.layout.music_list, container);
            getDialog().setTitle(R.string.title_choose_music);

            mMusiclist = (ListView) view.findViewById(R.id.local_music_list_view);

            /* Set List Adapter */
            mp3Infos = MediaUtil.getMp3Infos(getActivity());
            listAdapter = new MusicListAdapter(getActivity(), mp3Infos);
            mMusiclist.setAdapter(listAdapter);
            mMusiclist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    String songName = mp3Infos.get(position).getTitle();
                    long songLength = mp3Infos.get(position).getDuration();
                    String toastMessage = "Added " + songName + " to " + currentStation + " queue.";
                    Toast.makeText(getActivity(), toastMessage, Toast.LENGTH_SHORT).show();

                    /* Instruct service to send command to the server to add the song to the station */
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
                    String stationName = bundle.getString(Networking.STATION_LIST_REQUEST_RESPONSE);
                    if (!stationList.contains(stationName)) {
                        stationList.add(stationName);
                        stationAdapter.add(stationName);
                    }
                }
                break;
                case AppConstants.STATION_ADDED_NOTIFIER: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    if (!stationList.contains(stationName)) {
                        stationList.add(stationName);
                        stationAdapter.add(stationName);
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
                }
                break;
                case AppConstants.SONG_ADDED_NOTIFIER: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String songName = bundle.getString(AppConstants.SONG_NAME_KEY);
                    if (currentStation.equals(stationName)) {
                        if (!songList.contains(songName)) {
                            songList.add(songName);
                            stationQueueAdapter.add(songName);
                        }
                    }
                }
                break;
                case AppConstants.SONG_REMOVED_NOTIFIER: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String songName = bundle.getString(AppConstants.SONG_NAME_KEY);
                    if (currentStation.equals(stationName)) {
                        songList.remove(songName);
                        stationQueueAdapter.remove(songName);
                    }
                }
                break;
                case AppConstants.SONG_ON_LIST_RESPONSE: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String songName = bundle.getString(AppConstants.SONG_NAME_KEY);
                    if (currentStation.equals(stationName)) {
                        songList.add(songName);
                        stationQueueAdapter.add(songName);
                    }
                }
                break;
                case AppConstants.USER_ADDED_NOTIFIER: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String userIP = bundle.getString(AppConstants.USER_IP_KEY);
                    int udpPort = bundle.getInt(AppConstants.USER_UDP_PORT_KEY);
                    if (!userIPList.contains(userIP))
                        userIPList.add(userIP);
                    udpPortMap.put(userIP, udpPort);
                }
                break;
                case AppConstants.USER_REMOVED_NOTIFIER: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String userIP = bundle.getString(AppConstants.USER_IP_KEY);
                    int udpPort = bundle.getInt(AppConstants.USER_UDP_PORT_KEY);
                    userIPList.remove(userIP);
                    udpPortMap.remove(userIP);
                }
                break;
                case AppConstants.USER_ON_LIST_RESPONSE: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(AppConstants.STATION_NAME_KEY);
                    String userIP = bundle.getString(AppConstants.USER_IP_KEY);
                    int udpPort = bundle.getInt(AppConstants.USER_UDP_PORT_KEY);
                    userIPList.add(userIP);
                    udpPortMap.put(userIP, udpPort);
                }
                break;
                case AppConstants.PLAY_SONG_CMD: {
                    Bundle bundle = msg.getData();
                    String songName = bundle.getString(AppConstants.SONG_NAME_KEY);
                    for(String userIP : userIPList){
                        networkService.sendSong(userIP, Networking.TCP_PORT_NUMBER, songName);
                    }
                }
                break;
            }
        }
    }
}

