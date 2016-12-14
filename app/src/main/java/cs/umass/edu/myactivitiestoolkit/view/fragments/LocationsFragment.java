package cs.umass.edu.myactivitiestoolkit.view.fragments;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashSet;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.clustering.Cluster;
import cs.umass.edu.myactivitiestoolkit.clustering.Clusterable;
import cs.umass.edu.myactivitiestoolkit.clustering.ClusteringRequest;
import cs.umass.edu.myactivitiestoolkit.clustering.DBScan;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.location.FastConvexHull;
import cs.umass.edu.myactivitiestoolkit.location.GPSLocation;
import cs.umass.edu.myactivitiestoolkit.location.LocationDAO;
import cs.umass.edu.myactivitiestoolkit.services.AccelerometerService;
import cs.umass.edu.myactivitiestoolkit.services.LocationService;
import cs.umass.edu.myactivitiestoolkit.services.ServiceManager;
import cs.umass.edu.myactivitiestoolkit.services.msband.GsrDAO;
import cs.umass.edu.myactivitiestoolkit.services.msband.GsrReading;
import cs.umass.edu.myactivitiestoolkit.services.msband.HeartRateDAO;
import cs.umass.edu.myactivitiestoolkit.services.msband.HeartRateReading;
import cs.umass.edu.myactivitiestoolkit.util.PermissionsUtil;
import edu.umass.cs.MHLClient.client.MessageReceiver;
import edu.umass.cs.MHLClient.client.MobileIOClient;

/**
 * Fragment which visualizes the stored locations along with their clusters and allows
 * the user to select a clustering algorithm and change its parameters. The locations
 * are visualized using Google Maps API.
 * <br><br>
 * Assignment 5 : Before you get started, you will need a Google API key. See the
 * assignment details for instructions on how to do this.
 *
 * You will be implementing several clustering algorithms:
 * <ol>
 *      <li> You will implement DBScan for generic-typed {@link Clusterable} points. See {@link DBScan}.</li>
 *      <li> You will cluster locations using k-means in scikit-learn </li>
 *      <li> You will cluster locations using mean-shift in scikit-learn </li>
 * </ol>
 *
 * <br><br>
 *
 * @author CS390MB
 *
 * @see GoogleMap
 * @see MapView
 * @see LocationService
 * @see Fragment
 * @see DBScan
 * @see Clusterable
 * @see Cluster
 * @see ClusteringRequest
 */
public class LocationsFragment extends Fragment {

    @SuppressWarnings("unused")
    /** Used during debugging to identify logs by class */
    private static final String TAG = LocationsFragment.class.getName();
    /** Request code required for obtaining location permission. **/
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 4;
    /** The view which contains the {@link #map} **/
    MapView mapView;
    /** The map object. **/
    private GoogleMap map;
    /** The list of visual map markers representing saved locations. */
    private final List<Marker> locationMarkers;
    /** The list of visual map markers representing cluster centers. */
    private final List<Marker> clusterMarkers;
    /** Indicates whether map markers, excluding cluster centers, should be displayed. **/
    private boolean hideMarkers = false;
    /** The location services icon which functions as a button to toggle the {@link LocationService}. **/
    private View btnToggleLocationService;
    /** Allows the user to define the epsilon parameter for DBScan. **/
    private EditText txtEps;
    /** Allows the user to define the minimum points parameter for DBScan. **/
    private EditText txtMinPts;
    /** Allows the user to define the number of clusters for k-means clustering. **/
    private EditText txtKClusters;
    /** Reference to the service manager which communicates to the {@link LocationService}. **/
    private ServiceManager serviceManager;
    /** Responsible for communicating with the data collection server. */
    protected MobileIOClient client;
    /** The user ID required to authenticate the server connection. */
    protected String userID;

    /**
     * We listen for text input, so that we can decide how the UI is modified when the keyboard appears.
     */
    private final View.OnFocusChangeListener textLostFocusListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus){
            if(!hasFocus) {
                InputMethodManager imm =  (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }
    };

    public LocationsFragment(){
        locationMarkers = new ArrayList<>();
        clusterMarkers = new ArrayList<>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceManager = ServiceManager.getInstance(getActivity());
        userID = getString(R.string.mobile_health_client_user_id);
        client = MobileIOClient.getInstance(getActivity().getApplicationContext(), userID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_location, container, false);

        final RadioGroup clusterAlgorithmOpts = (RadioGroup) rootView.findViewById(R.id.radioGroupClusteringAlgorithm);
        final View parametersDBScan = rootView.findViewById(R.id.parameters_dbscan);
        final View parametersKMeans = rootView.findViewById(R.id.parameters_kmeans);
        clusterAlgorithmOpts.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.radioButtonDBScan:
                        parametersDBScan.setVisibility(View.VISIBLE);
                        parametersKMeans.setVisibility(View.INVISIBLE);
                        break;
                    case R.id.radioButtonKMeans:
                        parametersDBScan.setVisibility(View.INVISIBLE);
                        parametersKMeans.setVisibility(View.VISIBLE);
                        break;
                    case R.id.radioButtonMeanShift:
                        parametersDBScan.setVisibility(View.INVISIBLE);
                        parametersKMeans.setVisibility(View.INVISIBLE);
                        break;
                }
            }
        });

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        txtEps = (EditText) rootView.findViewById(R.id.txtEps);
        txtEps.setOnFocusChangeListener(textLostFocusListener);

        txtKClusters = (EditText) rootView.findViewById(R.id.txtKClusters);
        txtKClusters.setOnFocusChangeListener(textLostFocusListener);

        txtMinPts = (EditText) rootView.findViewById(R.id.txtMinPts);
        txtMinPts.setOnFocusChangeListener(textLostFocusListener);

        mapView = (MapView) rootView.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
            System.gc();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                LocationsFragment.this.map = mMap;
            }
        });
        View btnUpdate = rootView.findViewById(R.id.btnUpdateMap);
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GPSLocation[] locations = getSavedLocations();
                HeartRateReading[] heartrates = getSavedHeartRates();
                GsrReading[] resistances = getSavedResistances();

                if (locations.length == 0){
                    Toast.makeText(getActivity(), "No locations to cluster.", Toast.LENGTH_LONG).show();
                    return;
                }
                //Place a marker at each point and also adds it to the global list of markers
                map.clear();
                locationMarkers.clear();
                if (!hideMarkers) {
                    for (GPSLocation loc : locations) {
                        Marker marker = map.addMarker(new MarkerOptions()
                                .position(new LatLng(loc.latitude, loc.longitude)) //sets the latitude & longitude
                                .title("At " + LocationDAO.getISOTimeString(loc.timestamp))); //display the time it occurred when clicked
                        locationMarkers.add(marker);
                    }
                }
                switch (clusterAlgorithmOpts.getCheckedRadioButtonId()){
                    case R.id.radioButtonDBScan:
                        float eps = Float.parseFloat(txtEps.getText().toString());
                        int minPts = Integer.parseInt(txtMinPts.getText().toString());
                        runDBScan(locations, resistances, heartrates, eps, minPts);
                        break;
                    case R.id.radioButtonKMeans:
                        int k = Integer.parseInt(txtKClusters.getText().toString());
                        runKMeans(locations, resistances, heartrates, k);
                        break;
                    case R.id.radioButtonMeanShift:
                        runMeanShift(locations, resistances, heartrates);
                        break;
                }
                zoomInOnMarkers(100); // zoom to clusters automatically
            }
        });
        View btnSettings = rootView.findViewById(R.id.btnMapsSettings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(getActivity(), view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_maps, popup.getMenu());
                popup.show();
                popup.getMenu().getItem(0).setTitle(hideMarkers ? "Show Markers" : "Hide Markers");
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.action_hide_markers) {
                            hideMarkers = !hideMarkers;
                            for (Marker marker : locationMarkers){
                                marker.setVisible(!hideMarkers);
                            }
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
            }
        });
        btnToggleLocationService = rootView.findViewById(R.id.btnToggleLocation);
        if (serviceManager.isServiceRunning(LocationService.class)) {
            btnToggleLocationService.setBackgroundResource(R.drawable.ic_location_on_black_48dp);
        } else {
            btnToggleLocationService.setBackgroundResource(R.drawable.ic_location_off_black_48dp);
        }
        btnToggleLocationService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!serviceManager.isServiceRunning(LocationService.class)) {
                    requestPermissions();
                }else{
                    serviceManager.stopSensorService(LocationService.class);
                }
            }
        });
        return rootView;
    }

    /**
     * When the fragment starts, register a {@link #receiver} to receive messages from the
     * {@link LocationService}. The intent filter defines messages
     * we are interested in receiving. We would like to receive notifications for when the
     * service has started and stopped in order to update the toggle icon appropriately.
     */
    @Override
    public void onStart() {
        super.onStart();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION.BROADCAST_MESSAGE);
        broadcastManager.registerReceiver(receiver, filter);
    }

    /**
     * When the fragment stops, e.g. the user closes the application or opens a new activity,
     * then we should unregister the {@link #receiver}.
     */
    @Override
    public void onStop() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        try {
            broadcastManager.unregisterReceiver(receiver);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
        super.onStop();
    }

    /**
     * Retrieves all locations saved in the local database.
     * @return a list of {@link GPSLocation}s.
     */
    private GPSLocation[] getSavedLocations(){
        LocationDAO dao = new LocationDAO(getActivity());
        try {
            dao.openRead();
            return dao.getAllLocations();
        } finally {
            dao.close();
        }
    }

    private HeartRateReading[] getSavedHeartRates(){
        HeartRateDAO dao = new HeartRateDAO(getActivity());
        try {
            dao.openRead();
            return dao.getAllHeartRates();
        } finally {
            dao.close();
        }
    }

    private GsrReading[] getSavedResistances(){
        GsrDAO dao = new GsrDAO(getActivity());
        try {
            dao.openRead();
            return dao.getAllResistances();
        } finally {
            dao.close();
        }
    }

    /**
     * Here you should draw clusters on the map. We have given you {@link #drawHullFromPoints(GPSLocation[], int)},
     * which draws a convex hull around the specified points, in the given color. For each cluster,
     * you should draw the convex hull in a unique color (it's OK if it's not unique after several
     * clusters, as long as we can distinguish clusters that are close or overlap). We provided you
     * with an array of colors you can index into. Make sure if you have more clusters than the size
     * of the list to handle an {@link ArrayIndexOutOfBoundsException}, e.g. by using the modulus
     * operator (%).
     * <br><br>
     * EXTRA CREDIT: You may optionally display the cluster centers as a marker. You may approximate
     * the geographic cluster center by averaging the latitudes and longitudes separately, or
     * you may go above and beyond and account for the spherical nature of the earth.
     * See <a href="http://www.geomidpoint.com/calculation.html">geomidpoint.com</a> for details.
     */
    private void drawClusters(final Collection<Cluster<GPSLocation>> clusters, final GsrReading[] resistances, final HeartRateReading[] heartrates){

        final int[] colors = new int[]{Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.CYAN, Color.WHITE};
        Log.i("number of clusters", String.valueOf(clusters.size()));
        int i =0;
        for(Cluster a: clusters ){
        Log.i("clusters are", String.valueOf(a.size()));}
        for(Cluster<GPSLocation> a: clusters){
            GPSLocation[] pts = a.getPoints().toArray(new GPSLocation[a.size()]);
            GsrReading[] gsrs = getClusterResistances(pts, resistances);
            HeartRateReading[] hrs = getClusterHeartrates(pts, heartrates);
            final int color = Color.argb(computeStressRating(hrs, gsrs), 255, 0, 0);
            drawHullFromPoints(pts,color);
            i++;
            }
            if(i>colors.length) i =0;
    }


    // The locations in each call of getClusterResistances and getClusterHeartrates
    // correspond to all of the locations comprising a cluster. Grab timestamp from
    // each location and find the two corresponding resistance/heartrate
    // entries that occurred before and after this timestamp. If the event
    private GsrReading[] getClusterResistances(GPSLocation[] locations, GsrReading[] resistances){
        HashSet<GsrReading> gsrList = new HashSet<GsrReading>();
        for(int i = 0; i < locations.length; i++) {
            for(int j = 0; j < resistances.length; j++) {
                if(locations[i].timestamp > resistances[j].timestamp && j > 0) {
                    // case: most recent location is more recent than most recent gsr
                    GsrReading previous = resistances[j-1];
                    GsrReading next = resistances[j];
                    if(locations[i].timestamp - previous.timestamp > 900000) {
                        gsrList.add(previous);
                    }
                    if(next.timestamp - locations[i].timestamp > 900000) {
                        gsrList.add(next);
                    }
                }
            }
        }
        return gsrList.toArray(new GsrReading[gsrList.size()]);
    }

    private HeartRateReading[] getClusterHeartrates(GPSLocation[] locations, HeartRateReading[] heartrates) {
        HashSet<HeartRateReading> hrList = new HashSet<HeartRateReading>();
        for(int i = 0; i < locations.length; i++) {
            for(int j = 0; j < heartrates.length; j++) {
                if(locations[i].timestamp > heartrates[j].timestamp && j > 0) {
                    // case: most recent location is more recent than most recent gsr
                    HeartRateReading previous = heartrates[j-1];
                    HeartRateReading next = heartrates[j];
                    if(locations[i].timestamp - previous.timestamp > 900000) {
                        hrList.add(previous);
                    }
                    if(next.timestamp - locations[i].timestamp > 900000) {
                        hrList.add(next);
                    }
                }
            }
        }
        return hrList.toArray(new HeartRateReading[hrList.size()]);
    }

    private void drawHullFromPoints(GPSLocation[] locations, int color){
        if (locations.length <= 2) return;
        ArrayList<GPSLocation> hull = FastConvexHull.execute(locations);
        PolygonOptions options = new PolygonOptions();
        for(GPSLocation loc : hull){
            options.add(new LatLng(loc.latitude,loc.longitude));
        }
        options = options.strokeColor(Color.RED).fillColor(color);
        map.addPolygon(options); // draw a polygon
    }

    private int computeStressRating(HeartRateReading[] heartrates, GsrReading[] resistances){
        // TODO: compute some composite value [0,255] representing stress from
        // heartrates and resistances that will correspond to the fill color's alpha

        double unstressed_hr = 80.0, stressed_hr = 150.0,
               unstressed_gsr = 0.0, stressed_gsr = 100.0,
               hrsum = 0.0, gsrsum = 0.0, hravg = 0.0, gsravg = 0.0;
        for(int i = 0; i < heartrates.length; i++) {
            hrsum += heartrates[i].heartRate;
        }
        for(int i = 0; i < resistances.length; i++) {
            gsrsum += resistances[i].resistance;
        }
        hravg = hrsum / heartrates.length;
        gsravg = gsrsum / resistances.length;

        return 0;
    }

    private void runDBScan(GPSLocation[] locations, GsrReading[] resistances, HeartRateReading[] heartrates, float eps, int minPts){
        DBScan<GPSLocation> scan = new DBScan<GPSLocation>(eps,minPts);
        List z = Arrays.asList(locations);
        List x =  scan.cluster(z);
        drawClusters(x, resistances, heartrates);
    }

    private void runKMeans(final GPSLocation[] locations, final GsrReading[] resistances, final HeartRateReading[] heartrates, final int k){
        client.registerMessageReceiver(new MessageReceiver() {
            @Override
            protected void onMessageReceived(JSONObject json) {
                final Map<Integer, Cluster<GPSLocation>> clusters = new ArrayMap<>();
                try {
                    // These few lines of code parse the String object containing
                    // the indexes.
                    String str = json.getString("indexes");
                    String[] indexList = str.substring(1, str.length()-1).split(",");
                    int[] indexes = new int[indexList.length];
                    for (int i = 0; i < indexList.length; i++){
                        indexes[i] = Integer.parseInt(indexList[i].replace("\"", "").trim());
                    }


                    for (int i = 0; i < indexes.length; i++) {
                        int index = indexes[i];
                        // Using the index of each location, generate a list of k clusters, then call drawClusters().
                        // You may choose to use the Map defined above or find a different way of doing it.
                        // For example, if we send over points A, B, C, D, E and F and the clustering algorithm
                        // groups A and D into cluster 0, B, E and F into cluster 1, and C into cluster 2, then
                        // the resulting array of indexes will be [0,1,2,0,1,1].
                        if(clusters.get(index) == null){
                            Cluster c = new Cluster();
                            c.addPoint(locations[i]);
                            clusters.put(index, c);
                        }else{
                            Cluster c = clusters.get(index);
                            c.addPoint(locations[i]);
                            clusters.put(index, c);
                        }
                    }
                    // We are only allowed to manipulate the map on the main (UI) thread:
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            drawClusters(clusters.values(), resistances, heartrates);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    client.unregisterMessageReceiver(this);
                }
            }
        });
        client.connect();
        client.sendSensorReading(new ClusteringRequest(userID, "", "", System.currentTimeMillis(), locations, "k_means", k));
    }

    private void runMeanShift(final GPSLocation[] locations, final GsrReading[] resistances, final HeartRateReading[] heartrates){
        client.registerMessageReceiver(new MessageReceiver() {
            @Override
            protected void onMessageReceived(JSONObject json) {
                final Map<Integer, Cluster<GPSLocation>> clusters = new ArrayMap<>();
                try {
                    // These few lines of code parse the String object containing
                    // the indexes.
                    String str = json.getString("indexes");
                    String[] indexList = str.substring(1, str.length()-1).split(",");
                    int[] indexes = new int[indexList.length];
                    for (int i = 0; i < indexList.length; i++){
                        indexes[i] = Integer.parseInt(indexList[i].replace("\"", "").trim());
                    }
                    for (int i = 0; i < indexes.length; i++) {
                        int index = indexes[i];
                        if(clusters.get(index) == null){
                            Cluster c = new Cluster();
                            c.addPoint(locations[i]);
                            clusters.put(index, c);
                        }else{
                            Cluster c = clusters.get(index);
                            c.addPoint(locations[i]);
                            clusters.put(index, c);
                        }
                    }
                    // We are only allowed to manipulate the map on the main (UI) thread:
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            drawClusters(clusters.values(), resistances, heartrates);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    client.unregisterMessageReceiver(this);
                }
            }
        });
        client.connect();
        client.sendSensorReading(new ClusteringRequest(userID, "", "", System.currentTimeMillis(), locations, "mean_shift", -1));
    }

    /**
     * Zooms in as much as possible such that all markers are visible on the map
     * Thanks to andr at http://stackoverflow.com/questions/14828217/android-map-v2-zoom-to-show-all-the-markers
     * for this clean solution.
     * @param padding the number of pixels padding the ege of the map layout between markers
     */
    public void zoomInOnMarkers(int padding){
        if (locationMarkers.size() + clusterMarkers.size() == 0) return;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : locationMarkers) {
            builder.include(marker.getPosition());
        }
        for (Marker marker : clusterMarkers) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        map.animateCamera(cu);
    }

    /**
     * The receiver listens for messages from the {@link AccelerometerService}, e.g. was the
     * service started/stopped, and updates the status views accordingly. It also
     * listens for sensor data and displays the sensor readings to the user.
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Constants.ACTION.BROADCAST_MESSAGE)){
                    int message = intent.getIntExtra(Constants.KEY.MESSAGE, -1);
                    if (message == Constants.MESSAGE.LOCATION_SERVICE_STARTED){
                        btnToggleLocationService.setBackgroundResource(R.drawable.ic_location_on_black_48dp);
                    } else if (message == Constants.MESSAGE.LOCATION_SERVICE_STOPPED){
                        btnToggleLocationService.setBackgroundResource(R.drawable.ic_location_off_black_48dp);
                    }
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        System.gc();
        mapView.onLowMemory();
    }

    /**
     * Request permissions required for video recording. These include
     * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE},
     * and {@link android.Manifest.permission#CAMERA CAMERA}. If audio is enabled, then
     * the {@link android.Manifest.permission#RECORD_AUDIO RECORD_AUDIO} permission is
     * additionally required.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void requestPermissions(){
        String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

        if (!PermissionsUtil.hasPermissionsGranted(getActivity(), permissions)) {
            requestPermissions(permissions, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        onLocationPermissionGranted();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                //If the request is cancelled, the result array is empty.
                if (grantResults.length == 0) return;

                for (int i = 0; i < permissions.length; i++){
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                        switch (permissions[i]) {
                            case Manifest.permission.ACCESS_COARSE_LOCATION:
                                return;
                            case Manifest.permission.ACCESS_FINE_LOCATION:
                                return;
                            default:
                                return;
                        }
                    }
                }
                onLocationPermissionGranted();
            }
        }
    }

    /**
     * Called when location permissions have been granted by the user.
     */
    public void onLocationPermissionGranted(){
        serviceManager.startSensorService(LocationService.class);
    }
}