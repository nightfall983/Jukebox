package com.ketonax.jukebox;

import android.app.Activity;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.ketonax.Networking.MessageBuilder;
import com.ketonax.Networking.Networking;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.google.android.gms.ads.identifier.AdvertisingIdClient.Info;


public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private DatagramSocket udpSocket = null;
    EditText createStationEdit = null;
    String currentStation = null;
    String userID = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Check for content in savedInstanceState */
        if (savedInstanceState != null) {
            currentStation = savedInstanceState.getString(MainConstants.CURRENT_STATION);
        }

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(MainConstants.CURRENT_STATION, currentStation);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();

        switch (position) {
            case 0:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, CreateStationFragment.newInstance(position + 1))
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
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void createStation(View view) {
        /** Method called when Create button is pressed */
        String stationName = null;
        String message = null;
        UDP_Sender sender = new UDP_Sender();

        createStationEdit = (EditText) findViewById(R.id.station_name_entry);
        stationName = createStationEdit.getText().toString();

        if (stationName.isEmpty())
            Toast.makeText(getApplicationContext(), "Please enter a name for the station.", Toast.LENGTH_SHORT).show();
        else {
            String[] elements = {Networking.CREATE_STATION_CMD, stationName};
            message = MessageBuilder.buildMessage(elements);
            sender.send(message);

            /* Set the current station */
            currentStation = stationName;
        }

        createStationEdit.setText(null);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class CreateStationFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public CreateStationFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static CreateStationFragment newInstance(int sectionNumber) {
            CreateStationFragment fragment = new CreateStationFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.create_station_fragment, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    public static class MyStationFragment extends Fragment {
        /** A placeholder fragment containing a simple view. */

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public MyStationFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static MyStationFragment newInstance(int sectionNumber) {
            MyStationFragment fragment = new MyStationFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.my_station_fragment, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    public static class AboutInfoFragment extends Fragment {
        /** A placeholder fragment containing a simple view. */

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public AboutInfoFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static AboutInfoFragment newInstance(int sectionNumber) {
            AboutInfoFragment fragment = new AboutInfoFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
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
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    /*Used for sending data to Jukebox Server*/
    public class UDP_Sender {
        private byte[] sendData = null;
        private AsyncTask<Void, Void, Void> asyncSender;
        private Boolean keepRunning = true;
        private InetAddress serverAddress = null;
        private DatagramPacket sendPacket = null;
        private Info adInfo = null;

        public void send(final String message) {
            asyncSender = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {

                    try {
                        serverAddress = InetAddress.getByName(Networking.SERVER_IP_STRING);
                        udpSocket = Networking.getSocket();
                        udpSocket.connect(serverAddress, Networking.SERVER_PORT);
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
            else asyncSender.execute();
        }
    }
}

