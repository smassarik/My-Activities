package cs.umass.edu.myactivitiestoolkit.view.activities;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.HeartRateConsentListener;

import java.lang.ref.WeakReference;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.view.fragments.AboutFragment;
import cs.umass.edu.myactivitiestoolkit.view.fragments.AudioFragment;
import cs.umass.edu.myactivitiestoolkit.view.fragments.ExerciseFragment;
import cs.umass.edu.myactivitiestoolkit.view.fragments.HeartRateFragment;
import cs.umass.edu.myactivitiestoolkit.view.fragments.LocationsFragment;
import cs.umass.edu.myactivitiestoolkit.view.fragments.SettingsFragment;

import static com.microsoft.band.BandErrorType.UNSUPPORTED_SDK_VERSION_ERROR;

/**
 * The main activity is the entry point for the application. It is the primary UI and allows
 * the user to interact with the system.
 *
 * To help you organize your UI, we've grouped health aspects together into tabs. Swipe from
 * tab to tab to showcase the work that you've done.
 *
 * Most of the work you do will be in the individual fragments, services and helper classes.
 * You are not required to change anything in the main activity, although displaying status
 * messages appropriately may be useful and you're more than welcome to play around with the
 * tab layout.
 *
 * @author Sean Noran


 */
public class MainActivity extends AppCompatActivity {

    private Button btnStart, btnConsent;


    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = MainActivity.class.getName();
    private BandClient client = null;
    final WeakReference<Activity> reference = new WeakReference<Activity>(this);

    /**
     * Defines all available tabs in the main UI. For help
     enums,
     * see the <a href="https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html">Java documentation</a>.
     *
     * Each enum constant is parameterized with the class of the fragment associated
     * with it. The {@link #getPageNumber()} and {@link #getTitle()} methods define
     * which tab the fragment sits in and the tab text displayed to the user.
     *
     * If you wish to add another tab, e.g. for your final project, just follow the same setup.
     */
    public enum PAGES {
        MOTION_DATA(ExerciseFragment.class) {
            @Override
            public String getTitle() {
                return "My Exercise";
            }

            @Override
            public int getPageNumber() {
                return 0;
            }
        },
        AUDIO_DATA(AudioFragment.class) {
            @Override
            public String getTitle() {
                return "My Friends";
            }

            @Override
            public int getPageNumber() {
                return 1;
            }
        },
        PPG_DATA(HeartRateFragment.class) {
            @Override
            public String getTitle() {
                return "My Heart";
            }

            @Override
            public int getPageNumber() {
                return 2;
            }
        },
        LOCATION_DATA(LocationsFragment.class) {
            @Override
            public String getTitle() {
                return "My Locations";
            }

            @Override
            public int getPageNumber() {
                return 3;
            }
        },
        SETTINGS(SettingsFragment.class) {
            @Override
            public String getTitle() {
                return "Settings";
            }

            @Override
            public int getPageNumber() {
                return 4;
            }
        },
        ABOUT(AboutFragment.class) {
            @Override
            public String getTitle() {
                return "About";
            }

            @Override
            public int getPageNumber() {
                return 5;
            }
        };

        /**
         * Indicates the title of the page. This will be displayed in the tab.
         * Default is the enum name, e.g. "ABOUT". Override this to return a different title.
         * @return the page title displayed in the tab
         */
        public String getTitle(){
            return name();
        }

        /**
         * Returns the fragment associated with the page
         * @return Fragment object, null if an instantiate error occurs.
         */
        public Fragment getFragment(){
            try {
                return fragment.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch(IllegalAccessException e) {
                Log.d(TAG, "Cannot instantiate fragment. Constructor may be private.");
                e.printStackTrace();
            }
            return null;
        }

        /**
         * Indicates the page number of the page. If omitted, it will return
         * its position in the enum. Override this to specify a different page number.
         * @return the page number
         */
        public int getPageNumber(){
            return ordinal();
        }

        /**
         * Returns the number of pages available.
         * @return the length of {@link #values()}
         */
        static int getCount(){
            return values().length;
        }

        /**
         * Constructor for a page. It requires a fragment class which defines the fragment
         * that will be displayed in the tab.
         * @param fragment class type that extends Fragment
         */
        PAGES(Class<? extends Fragment> fragment){
            this.fragment = fragment;
        }

        /**
         * The fragment class associated with the enum type.
         */
        private final Class<? extends Fragment> fragment;
    }

    /** Displays status messages, e.g. connection station. **/
    private TextView txtStatus;

    private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
        @Override
        protected Void doInBackground(WeakReference<Activity>... params) {
            try {
                if (getConnectedBandClient()) {

                    if (params[0].get() != null) {
                        client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
                            @Override
                            public void userAccepted(boolean consentGiven) {
                            }
                        });
                    }
                } else {
                    showStatus("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                showStatus(exceptionMessage);

            } catch (Exception e) {
                showStatus(e.getMessage());
            }
            return null;
        }
    }

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
            Log.i("getConnectedBandClient","");
        if (client == null) {
            //Find paired bands
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                //No bands found...message to user
                Log.i("band is not connected","");
                return false;
            }
            //need to set client if there are devices
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if(ConnectionState.CONNECTED == client.getConnectionState()) {
            Log.i("band is connected","");
            return true;

        }

        //need to return connected status
        return ConnectionState.CONNECTED == client.connect().await();
    }

    @Override
    protected void onStart() {
        super.onStart();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        //the intent filter specifies the messages we are interested in receiving
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION.BROADCAST_MESSAGE);
        filter.addAction(Constants.ACTION.BROADCAST_STATUS);
        broadcastManager.registerReceiver(receiver, filter);
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        try {
            broadcastManager.unregisterReceiver(receiver);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("onCreate start","");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        /* Maintains the tabs and the tab layout interactions. */
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new FragmentPagerAdapter(getFragmentManager()) {
            private final String[] tabTitles = new String[PAGES.getCount()];
            private final Fragment[] fragments = new Fragment[PAGES.getCount()];
            //instance initializer:
            {
                for (PAGES page : PAGES.values()) {
                    tabTitles[page.getPageNumber()] = page.getTitle();
                    fragments[page.getPageNumber()] = page.getFragment();
                }
            }

            @Override
            public android.app.Fragment getItem(int position) {
                return fragments[position];
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return tabTitles[position];
            }

            @Override
            public int getCount() {
                return PAGES.getCount();
            }
        });
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        assert tabLayout != null;
        tabLayout.setupWithViewPager(viewPager);

        txtStatus = (TextView) findViewById(R.id.status); //set consent
        btnConsent = (Button) findViewById(R.id.btnConsent);
        btnStart = (Button) findViewById(R.id.btnConsent);

        // if the activity was started by clicking a notification, then the intent contains the
        // notification ID and can be used to set the proper tab.
        if (getIntent() != null) {
            int notificationID = getIntent().getIntExtra(Constants.KEY.NOTIFICATION_ID, Constants.NOTIFICATION_ID.ACCELEROMETER_SERVICE);
            switch (notificationID){
                case Constants.NOTIFICATION_ID.ACCELEROMETER_SERVICE:
                    viewPager.setCurrentItem(PAGES.MOTION_DATA.getPageNumber());
                    break;
                case Constants.NOTIFICATION_ID.AUDIO_SERVICE:
                    viewPager.setCurrentItem(PAGES.AUDIO_DATA.getPageNumber());
                    break;
                case Constants.NOTIFICATION_ID.LOCATION_SERVICE:
                    viewPager.setCurrentItem(PAGES.LOCATION_DATA.getPageNumber());
                    break;
                case Constants.NOTIFICATION_ID.PPG_SERVICE:
                    viewPager.setCurrentItem(PAGES.PPG_DATA.getPageNumber());
                    break;
            }
        }
        btnConsent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("onClickListener","");
                new HeartRateConsentTask().execute(reference);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Shows a removable status message at the bottom of the application.
     * @param message the status message shown
     */
    public void showStatus(String message){
        txtStatus.setText(message);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Constants.ACTION.BROADCAST_MESSAGE)){
                    int message = intent.getIntExtra(Constants.KEY.MESSAGE, -1);
                    switch (message){
                        case Constants.MESSAGE.ACCELEROMETER_SERVICE_STARTED:
                            showStatus(getString(R.string.accelerometer_started));
                            break;
                        case Constants.MESSAGE.ACCELEROMETER_SERVICE_STOPPED:
                            showStatus(getString(R.string.accelerometer_stopped));
                            break;
                        case Constants.MESSAGE.AUDIO_SERVICE_STARTED:
                            showStatus(getString(R.string.audio_started));
                            break;
                        case Constants.MESSAGE.AUDIO_SERVICE_STOPPED:
                            showStatus(getString(R.string.audio_stopped));
                            break;
                        case Constants.MESSAGE.LOCATION_SERVICE_STARTED:
                            showStatus(getString(R.string.location_started));
                            break;
                        case Constants.MESSAGE.LOCATION_SERVICE_STOPPED:
                            showStatus(getString(R.string.location_stopped));
                            break;
                        case Constants.MESSAGE.PPG_SERVICE_STARTED:
                            showStatus(getString(R.string.ppg_started));
                            break;
                        case Constants.MESSAGE.PPG_SERVICE_STOPPED:
                            showStatus(getString(R.string.ppg_stopped));
                            break;
                        case Constants.MESSAGE.BAND_SERVICE_STARTED:
                            showStatus(getString(R.string.band_started));
                            break;
                        case Constants.MESSAGE.BAND_SERVICE_STOPPED:
                            showStatus(getString(R.string.band_stopped));
                            break;
                    }
                } else if (intent.getAction().equals(Constants.ACTION.BROADCAST_STATUS)){
                    String message = intent.getStringExtra(Constants.KEY.STATUS);
                    if (message != null) {
                        showStatus(message);
                    }
                }
            }
        }
    };
}