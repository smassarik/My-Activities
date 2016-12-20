package cs.umass.edu.myactivitiestoolkit.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.nfc.Tag;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.ppg.HRSensorReading;
import cs.umass.edu.myactivitiestoolkit.ppg.PPGSensorReading;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.ppg.HeartRateCameraView;
import cs.umass.edu.myactivitiestoolkit.ppg.PPGEvent;
import cs.umass.edu.myactivitiestoolkit.ppg.PPGListener;
import cs.umass.edu.myactivitiestoolkit.processing.FFT;
import cs.umass.edu.myactivitiestoolkit.processing.Filter;
import cs.umass.edu.myactivitiestoolkit.util.Interpolator;
import edu.umass.cs.MHLClient.client.MobileIOClient;

/**
 * Photoplethysmography service. This service uses a {@link HeartRateCameraView}
 * to collect PPG data using a standard camera with continuous flash. This is where
 * you will do most of your work for this assignment.
 * <br><br>
 * <b>ASSIGNMENT (PHOTOPLETHYSMOGRAPHY)</b> :
 * In {@link #onSensorChanged(PPGEvent)}, you should smooth the PPG reading using
 * a {@link Filter}. You should send the filtered PPG reading both to the server
 * and to the {@link cs.umass.edu.myactivitiestoolkit.view.fragments.HeartRateFragment}
 * for visualization. Then call your heart rate detection algorithm, buffering the
 * readings if necessary, and send the bpm measurement back to the UI.
 * <br><br>
 * EXTRA CREDIT:
 *      Follow the steps outlined <a href="http://www.marcoaltini.com/blog/heart-rate-variability-using-the-phones-camera">here</a>
 *      to acquire a cleaner PPG signal. For additional extra credit, you may also try computing
 *      the heart rate variability from the heart rate, as they do.
 *
 * @author CS390MB
 *
 * @see HeartRateCameraView
 * @see PPGEvent
 * @see PPGListener
 * @see Filter
 * @see MobileIOClient
 * @see PPGSensorReading
 * @see Service
 */
public class PPGService extends SensorService implements PPGListener
{
    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = PPGService.class.getName();

    /* Surface view responsible for collecting PPG data and displaying the camera preview. */
    private HeartRateCameraView mPPGSensor;

    @Override
    protected void start() {
        Log.d(TAG, "START");
        mPPGSensor = new HeartRateCameraView(getApplicationContext(), null);

        WindowManager winMan = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);

        //surface view dimensions and position specified where service intent is called
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;

        //display the surface view as a stand-alone window
        winMan.addView(mPPGSensor, params);
        mPPGSensor.setZOrderOnTop(true);

        // only once the surface has been created can we start the PPG sensor
        mPPGSensor.setSurfaceCreatedCallback(new HeartRateCameraView.SurfaceCreatedCallback() {
            @Override
            public void onSurfaceCreated() {
                mPPGSensor.start(); //start recording PPG
            }
        });

        super.start();
    }

    @Override
    protected void onServiceStarted() {
        broadcastMessage(Constants.MESSAGE.PPG_SERVICE_STARTED);
    }

    @Override
    protected void onServiceStopped() {
        if (mPPGSensor != null)
            mPPGSensor.stop();
        if (mPPGSensor != null) {
            ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).removeView(mPPGSensor);
        }
        broadcastMessage(Constants.MESSAGE.PPG_SERVICE_STOPPED);
    }

    @Override
    protected void registerSensors() {
        // TODO: Register a PPG listener with the PPG sensor (mPPGSensor)
         mPPGSensor.registerListener(this);

    }

    @Override
    protected void unregisterSensors() {
        // TODO: Unregister the PPG listener
        mPPGSensor.unregisterListener(this);
    }

    @Override
    protected int getNotificationID() {
        return Constants.NOTIFICATION_ID.PPG_SERVICE;
    }

    @Override
    protected String getNotificationContentText() {
        return getString(R.string.ppg_service_notification);
    }

    @Override
    protected int getNotificationIconResourceID() {
        return R.drawable.ic_whatshot_white_48dp;
    }

    /**
     * This method is called each time a PPG sensor reading is received.
     * <br><br>
     * You should smooth the data using {@link Filter} and then send the filtered data both
     * to the server and the main UI for real-time visualization. Run your algorithm to
     * detect heart beats, calculate your current bpm and send the bmp measurement to the
     * main UI. Additionally, it may be useful for you to send the peaks you detect to
     * the main UI, using {@link #broadcastPeak(long, double)}. The plot is already set up
     * to draw these peak points upon receiving them.
     * <br><br>
     * Also make sure to send your bmp measurement to the server for visualization. You
     * can do this using {@link HRSensorReading}.
     *
     * @param event The PPG sensor reading, wrapping a timestamp and mean red value.
     *
     * @see PPGEvent
     * @see PPGSensorReading
     * @see HeartRateCameraView#onPreviewFrame(byte[], Camera)
     * @see MobileIOClient
     * @see HRSensorReading
     */


    int valueCount = 0;
    double[] timestamps = new double[50];
    double[] mbuffer = new double[50];
    double redMax = 0.0,
           redMin = 10000.0,
           maxTime = 0.0,
           minTime = 0.0,
           newSlope,
           curTime = 0.0;
    Filter mFilter = new Filter(3);
    float[] filteredValues;

    @SuppressWarnings("deprecation")
    @Override
    public void onSensorChanged(PPGEvent event) {
        // TODO: Smooth the signal using a Butterworth / exponential smoothing filter
        // TODO: send the data to the UI fragment for visualization, using broadcastPPGReading(...)
        // TODO: Send the filtered mean red value to the server
        // TODO: Buffer data if necessary for your algorithm
        // TODO: Call your heart beat and bpm detection algorithm
        // TODO: Send your heart rate estimate to the server
        curTime = event.timestamp;
        filteredValues = mFilter.getFilteredValues((float)event.value);
        if(valueCount < 15){
            timestamps[valueCount] = curTime;
            mbuffer[valueCount++] = filteredValues[0];
        }else{
            redMax = mbuffer[0];
            redMin = mbuffer[0];
            for(int i=0;i<mbuffer.length;i++){
                if(mbuffer[i]>redMax){
                    redMax=mbuffer[i];
                    maxTime=timestamps[i];
                } else if(mbuffer[i]<redMin){
                    redMin=mbuffer[i];
                    minTime=timestamps[i];
                }
            }
            newSlope = (redMax-redMin)/(maxTime-minTime);

        }

//
//        final long time = event.timestamp;
//        final double filtered = (double) filteredValues[0];
//        minTime = minTime==0?time:minTime;
//        maxTime = maxTime==0?time:maxTime;
//        broadcastPPGReading(time, filtered);
//        Log.d("PPGSERVICE", "\nfiltered: "+filtered);
////        Log.d("PPGSERVICE", "minTime: "+minTime);
////        Log.d("PPGSERVICE", "maxTime: "+maxTime);
//        Log.d("PPGSERVICE", "redMin: "+redMin);
//        Log.d("PPGSERVICE", "redMax: "+redMax);
//        Log.d("PPGSERVICE", "timeDiff: "+timeDiff);
//
//        if (filtered < redMin) {
//            redMin = filtered;
//            minTime = time;
//            Log.d("PPGSERVICE","    min found: "+redMin);
//        } else if (filtered > redMax) {
//            redMax = filtered;
//            maxTime = time;
//            Log.d("PPGSERVICE","    max found: "+redMax);
//        }
//
//        timeDiff = maxTime - minTime;
//        redDiff = redMax - redMin;
//        // example: timeDiff = .3;                                      example: bpm = 40
//        // bpm = beats/60 sec                                           40 beats/60 sec; [(1/40)*40 beats / (1/40)*60 sec]
//        // 1 beat/.3 sec; [(60/.3=200)*1 beat/(60/.3=200)*.3sec]        1 beat / (60/40=1.5) sec
//        // bpm = 200beats/min                                           timeDiff = 1.5
////        Log.d("red diff",""+(redMax-redMin));
////        Log.d("time diff",""+timeDiff);
//        if ( timeDiff>1 && timeDiff<1500) {
//                Log.d("PPGSERVICE","        HEARTBEAT");
//                bpm = (int) (60/timeDiff);
//                redMax = 0.0;
//                redMin = 10000.0;
//                timeDiff = 0.0;
//
//            broadcastPeak(time, redMax);
//        }else if(timeDiff>1500){
//            timeDiff = 0.0;
//            redMax = 0.0;
//            redMin = 10000.0;
//        }
//        broadcastBPM(bpm);
    }


    /**
     * Broadcasts the PPG reading to other application components, e.g. the main UI.
     * @param ppgReading the mean red value.
     */
    public void broadcastPPGReading(final long timestamp, final double ppgReading) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.PPG_DATA, ppgReading);
        intent.putExtra(Constants.KEY.TIMESTAMP, timestamp);
        intent.setAction(Constants.ACTION.BROADCAST_PPG);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    /**
     * Broadcasts the current heart rate in BPM to other application components, e.g. the main UI.
     * @param bpm the current beats per minute measurement.
     */
    public void broadcastBPM(final int bpm) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.HEART_RATE, bpm);
        intent.setAction(Constants.ACTION.BROADCAST_HEART_RATE);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    /**
     * Broadcasts the current heart rate in BPM to other application components, e.g. the main UI.
     * @param timestamp the current beats per minute measurement.
     */
    public void broadcastPeak(final long timestamp, final double value) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.PPG_PEAK_TIMESTAMP, timestamp);
        intent.putExtra(Constants.KEY.PPG_PEAK_VALUE, value);
        intent.setAction(Constants.ACTION.BROADCAST_PPG_PEAK);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }
}