package cs.umass.edu.myactivitiestoolkit.services.msband;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.SampleRate;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.services.SensorService;
import edu.umass.cs.MHLClient.sensors.AccelerometerReading;
import edu.umass.cs.MHLClient.sensors.GyroscopeReading;
import cs.umass.edu.myactivitiestoolkit.services.msband.GsrReading;
import cs.umass.edu.myactivitiestoolkit.services.msband.GsrDAO;
import cs.umass.edu.myactivitiestoolkit.services.msband.HeartRateReading;
import cs.umass.edu.myactivitiestoolkit.services.msband.HeartRateDAO;

/* @see Service#startForeground(int, Notification)
        * @see BandClient
        * @see BandGyroscopeEventListener
        */

public class BandService extends SensorService implements BandGyroscopeEventListener, BandHeartRateEventListener, BandGsrEventListener {

    /** used for debugging purposes */
    private static final String TAG = BandService.class.getName();

    /** The object which receives sensor data from the Microsoft Band */
    private BandClient bandClient = null;

    @Override
    protected void onServiceStarted() {
        HeartRateDAO hrdao = new HeartRateDAO(getApplicationContext());
        GsrDAO gsrdao = new GsrDAO(getApplicationContext());
        hrdao.create();
        gsrdao.create();
        broadcastMessage(Constants.MESSAGE.BAND_SERVICE_STARTED);
    }

    @Override
    protected void onServiceStopped() {
        broadcastMessage(Constants.MESSAGE.BAND_SERVICE_STOPPED);
    }

    /**
     * Asynchronous task for connecting to the Microsoft Band accelerometer and gyroscope sensors.
     * Errors may arise if the Band does not support the Band SDK version or the Microsoft Health
     * application is not installed on the mobile device.
     **
     * @see com.microsoft.band.BandErrorType#UNSUPPORTED_SDK_VERSION_ERROR
     * @see com.microsoft.band.BandErrorType#SERVICE_ERROR
     * @see BandClient#getSensorManager()
     * @see com.microsoft.band.sensors.BandSensorManager
     */
    private class SensorSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    broadcastStatus(getString(R.string.status_connected));
                    bandClient.getSensorManager().registerGyroscopeEventListener(BandService.this, SampleRate.MS16);
                    bandClient.getSensorManager().registerGsrEventListener(BandService.this);
                    bandClient.getSensorManager().registerHeartRateEventListener(BandService.this);
                } else {
                    broadcastStatus(getString(R.string.status_not_connected));
                }
            } catch (BandException e) {
                String exceptionMessage;
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = getString(R.string.err_unsupported_sdk_version);
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = getString(R.string.err_service);
                        break;
                    default:
                        exceptionMessage = getString(R.string.err_default) + e.getMessage();
                        break;
                }
                Log.e(TAG, exceptionMessage);
                broadcastStatus(exceptionMessage);

            } catch (Exception e) {
                broadcastStatus(getString(R.string.err_default) + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Connects the mobile device to the Microsoft Band
     * @return True if successful, False otherwise
     * @throws InterruptedException if the connection is interrupted
     * @throws BandException if the band SDK version is not compatible or the Microsoft Health band is not installed
     */
    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (bandClient == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                broadcastStatus(getString(R.string.status_not_paired));
                return false;
            }
            bandClient = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == bandClient.getConnectionState()) {
            return true;
        }

        broadcastStatus(getString(R.string.status_connecting));
        return ConnectionState.CONNECTED == bandClient.connect().await();
    }

    @Override
    protected void registerSensors() {
        
        new SensorSubscriptionTask().execute();

    }

    /**
     * unregisters the sensors from the sensor service
     */
    @Override
    public void unregisterSensors() {
        if (bandClient != null) {
            try {
                bandClient.getSensorManager().unregisterAllListeners();
                disconnectBand();
            } catch (BandIOException e) {
                broadcastStatus(getString(R.string.err_default) + e.getMessage());
            }
        }
    }

    @Override
    protected int getNotificationID() {
        return Constants.NOTIFICATION_ID.ACCELEROMETER_SERVICE;
    }

    @Override
    protected String getNotificationContentText() {
        return getString(R.string.activity_service_notification);
    }

    @Override
    protected int getNotificationIconResourceID() {
        return R.drawable.ic_running_white_24dp;
    }

    /**
     * disconnects the sensor service from the Microsoft Band
     */
    public void disconnectBand() {
        if (bandClient != null) {
            try {
                bandClient.disconnect().await();
            } catch (InterruptedException | BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
    }

    @Override
    public void onBandGyroscopeChanged(BandGyroscopeEvent event) {
        //TODO: Remove code from starter code
        Object[] data = new Object[]{event.getTimestamp(),
                event.getAccelerationX(), event.getAccelerationY(), event.getAccelerationZ(),
                event.getAngularVelocityX(), event.getAngularVelocityY(), event.getAngularVelocityZ()};
        mClient.sendSensorReading(new AccelerometerReading(mUserID, "", "", event.getTimestamp(),
                event.getAccelerationX(), event.getAccelerationY(), event.getAccelerationZ()));
        broadcastAccelerometerReading(event.getTimestamp(),
                event.getAccelerationX(), event.getAccelerationY(), event.getAccelerationZ());
        mClient.sendSensorReading(new GyroscopeReading(mUserID, "", "", event.getTimestamp(),
                event.getAngularVelocityX(), event.getAngularVelocityY(), event.getAngularVelocityZ()));
        String sample = TextUtils.join(",", data);
        Log.d(TAG, sample);
    }

    public void onBandHeartRateChanged(BandHeartRateEvent bandHeartRateEvent) {
        HeartRateReading heartRateReading = new HeartRateReading(bandHeartRateEvent.getTimestamp(), bandHeartRateEvent.getHeartRate());
        HeartRateDAO dao = new HeartRateDAO(getApplicationContext());
        dao.openWrite();
        dao.insert(heartRateReading);
        dao.close();
    }

    public void onBandGsrChanged(BandGsrEvent event) {
        GsrReading gsrReading = new GsrReading(event.getTimestamp(), event.getResistance());
        GsrDAO dao = new GsrDAO(getApplicationContext());
        dao.openWrite();
        dao.insert(gsrReading);
        dao.close();
    }


    /**
     * Broadcasts the accelerometer reading to other application components, e.g. the main UI.
     * @param accelerometerReadings the x, y, and z accelerometer readings
     */
    public void broadcastAccelerometerReading(final long timestamp, final float... accelerometerReadings) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.TIMESTAMP, timestamp);
        intent.putExtra(Constants.KEY.ACCELEROMETER_DATA, accelerometerReadings);
        intent.setAction(Constants.ACTION.BROADCAST_ACCELEROMETER_DATA);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }
}