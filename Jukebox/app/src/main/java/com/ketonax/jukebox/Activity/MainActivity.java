package com.ketonax.jukebox.Activity;

import android.app.ActionBar;
import android.app.Activity;
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
import com.ketonax.jukebox.R;

import java.util.ArrayList;


public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    static Messenger mService;
    static ArrayAdapter<String> stationAdapter;
    static ListView stationListView;
    //Show the music list
    static ListView musiclistView;
    static ArrayList<String> stationList = new ArrayList<String>();
    /* Other variables*/
    static String currentStation;
    /* Service Variables */
    boolean mIsBound;
    Messenger mMessenger = new Messenger(new IncomingHandler());
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mService = new Messenger(service);
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
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mIsBound = false;
            Log.i(AppConstants.APP_TAG, "Service is disconnected.");
        }
    };
    /* Views and related variables */
    EditText createStationEdit = null;
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

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        /* Set up the drawer. */
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
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
        stationAdapter.clear();
        stationAdapter.addAll(stationList);
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
                fragmentManager.beginTransaction()
                        .replace(R.id.container, JoinStationFragment.newInstance(position + 1))
                        .commit();
                break;
            case 1:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, MyStationFragment.newInstance(position + 1))
                        .commit();
                break;
            case 2:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, AboutInfoFragment.newInstance(position + 1))
                        .commit();
                break;
        }
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
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

    public void createStation(View view) {
        /**
         * Method called when Create button is pressed.
         * Sends the name of the station to be created to the NetworkService.
         */

        String stationToCreate;
        createStationEdit = (EditText) findViewById(R.id.station_name_entry);
        stationToCreate = createStationEdit.getText().toString();

        if (stationToCreate.isEmpty())
            Toast.makeText(getApplicationContext(), "Please enter a name for the station.", Toast.LENGTH_SHORT).show();
        else {
            /* Send stationToCreate to service */
            try {
                Message msg = Message.obtain(null, AppConstants.CREATE_STATION_CMD);
                Bundle bundle = new Bundle();
                bundle.putString(Networking.CREATE_STATION_CMD, stationToCreate);
                msg.setData(bundle);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                //e.printStackTrace();
            }
            currentStation = stationToCreate;
        }

        createStationEdit.setText(null);
    }

    public void leaveCurrentStation() {
        /**
         * This methods sends a command to leave the current station to the Networking service
         */

        try {
            Message msg = Message.obtain(null, AppConstants.LEAVE_STATION_CMD);
            Bundle bundle = new Bundle();
            bundle.putString(Networking.LEAVE_STATION_CMD, currentStation);
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
            String command = Networking.EXIT_JUKEBOX_NOTIFIER;
            Bundle bundle = new Bundle();
            bundle.putString(Networking.EXIT_JUKEBOX_NOTIFIER, command);
            msg.setData(bundle);
            mService.send(msg);
            stationList.clear();
            stationAdapter.clear();
            //TODO Stop background music playback service
        } catch (RemoteException e) {
            //e.printStackTrace();
        }
    }

    /* Private Methods */
    private void doBindService() {
        /** This method establishes a connection with a service */

        bindService(new Intent(this,
                NetworkingService.class), mConnection, Context.BIND_AUTO_CREATE);
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
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.join_station_fragment, container, false);
            showStationListView(rootView);
            return rootView;
        }

        public void showStationListView(final View rootView) {

            stationListView = (ListView) rootView.findViewById(R.id.station_list_view);
            stationAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1);
            stationAdapter.addAll(stationList);

            if (stationListView == null)
                return;
            stationListView.setAdapter(stationAdapter);
            stationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                /* */
                /* Send message to service to send JOIN_STATION_CMD */
                    String stationName = stationList.get(position);

                    if (currentStation != null) {

                        if (!currentStation.equals(stationName)) {
                            currentStation = stationName;
                            Message msg = Message.obtain(null, AppConstants.JOIN_STATION_CMD);
                            Bundle bundle = new Bundle();
                            bundle.putString(Networking.JOIN_STATION_CMD, stationName);
                            msg.setData(bundle);

                            try {
                                mService.send(msg);
                            } catch (RemoteException e) {
                                //e.printStackTrace();
                            }
                            Toast.makeText(rootView.getContext(), "Joining " + stationName, Toast.LENGTH_SHORT).show();
                        } else
                            Toast.makeText(rootView.getContext(), "You are already connected to this station ", Toast.LENGTH_SHORT).show();
                    } else {
                        currentStation = stationName;
                        Message msg = Message.obtain(null, AppConstants.JOIN_STATION_CMD);
                        Bundle bundle = new Bundle();
                        bundle.putString(Networking.JOIN_STATION_CMD, stationName);
                        msg.setData(bundle);

                        try {
                            mService.send(msg);
                        } catch (RemoteException e) {
                            //e.printStackTrace();
                        }
                        Toast.makeText(rootView.getContext(), "Joining " + stationName, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(AppConstants.ARG_SECTION_NUMBER));
        }
    }

    //Create a toast that lists all the music on local and play it.
    public void search_local_music(View view){
        Intent intent = new Intent(this, MusicList.class);
        startActivity(intent);
        return;
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
            showMusicListView(rootView);
            return rootView;
        }



        public void showMusicListView(final View rootView){
            musiclistView=(ListView)rootView.findViewById(R.id.song_queue_id);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(AppConstants.ARG_SECTION_NUMBER));
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
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(AppConstants.ARG_SECTION_NUMBER));
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
                    String stationName = bundle.getString(Networking.STATION_ADDED_NOTIFIER);
                    if (!stationList.contains(stationName)) {
                        stationList.add(stationName);
                        stationAdapter.add(stationName);
                    }
                }
                break;
                case AppConstants.STATION_KILLED_NOTIFIER: {
                    Bundle bundle = msg.getData();
                    String stationName = bundle.getString(Networking.STATION_KILLED_NOTIFIER);

                    /* Reset current station if it has been removed from the server*/
                    if (currentStation != null) {
                        if (currentStation.equals(stationName))
                            currentStation = null;
                    }
                    stationList.remove(stationName);
                    stationAdapter.remove(stationName);
                }
                break;
            }
        }
    }
}

