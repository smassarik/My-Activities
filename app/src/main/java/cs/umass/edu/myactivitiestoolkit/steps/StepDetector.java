package cs.umass.edu.myactivitiestoolkit.steps;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import cs.umass.edu.myactivitiestoolkit.processing.Filter;

/**
 * This class is responsible for detecting steps from the accelerometer sensor.
 * All {@link OnStepListener step listeners} that have been registered will
 * be notified when a step is detected.
 */
public class StepDetector implements SensorEventListener {
    /** Used for debugging purposes. */
    @SuppressWarnings("unused")
    private static final String TAG = StepDetector.class.getName();

    /** Maintains the set of listeners registered to handle step events. **/
    private ArrayList<OnStepListener> mStepListeners;

    /**
     * The number of steps taken.
     */
    private int stepCount;

    private Filter mFilter = new Filter(3.0);
    private float[] mbuffer = new float[50];
    private int valueCount = 0;
    private long[] timestamps = new long[50];


    public StepDetector(){
        mStepListeners = new ArrayList<>();
        stepCount = 0;
    }

    /**
     * Registers a step listener for handling step events.
     * @param stepListener defines how step events are handled.
     */
    public void registerOnStepListener(final OnStepListener stepListener){
        mStepListeners.add(stepListener);
    }

    /**
     * Unregisters the specified step listener.
     * @param stepListener the listener to be unregistered. It must already be registered.
     */
    public void unregisterOnStepListener(final OnStepListener stepListener){
        mStepListeners.remove(stepListener);
    }

    /**
     * Unregisters all step listeners.
     */
    public void unregisterOnStepListeners(){
        mStepListeners.clear();
    }

    /**
     * Here is where you will receive accelerometer readings, mbuffer them if necessary
     * and run your step detection algorithm. When a step is detected, call
     * {@link #onStepDetected(long, float[])} to notify all listeners.
     *
     * Recall that human steps tend to take anywhere between 0.5 and 2 seconds.
     *
     * @param event sensor reading
     */
    @Override
    //TODO: Detect steps! Call onStepDetected(...) when a step is detected.
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float minAccel, maxAccel, oldSlope, newSlope = 0;
            long minTime = 0, maxTime = 0, curTime = event.timestamp;
            float[] filteredValues = mFilter.getFilteredValues(event.values);
            float vLength = (float)Math.sqrt((Math.pow(filteredValues[0],2) + Math.pow(filteredValues[1], 2) + Math.pow(filteredValues[2], 2)/3));

            if(valueCount < 50){
                timestamps[valueCount] = curTime;
                mbuffer[valueCount++] = vLength;
            }else{
                minAccel = mbuffer[0];
                maxAccel = mbuffer[0];
                for(int i = 0; i < mbuffer.length; i++){
                    if(mbuffer[i] > maxAccel){
                        maxAccel = mbuffer[i];
                        maxTime = timestamps[i];
                    } else if(mbuffer[i] < minAccel){
                        minAccel = mbuffer[i];
                        minTime = timestamps[i];
                    }
                }

                oldSlope = newSlope;
                //determine which of the extrema occur later and calculate new slope
                if(minTime > maxTime) newSlope = (minAccel - maxAccel)/(minTime - maxTime);
                else newSlope = (maxAccel - minAccel)/(maxTime);

                //check for change in sign
                if(Math.abs(oldSlope) + Math.abs(newSlope) > Math.abs(oldSlope + newSlope)) onStepDetected(curTime, mbuffer);

                mbuffer = new float[50];
                valueCount = 0;
                mbuffer[valueCount++] = vLength;
            }


        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // do nothing
    }

    /**
     * This method is called when a step is detected. It updates the current step count,
     * notifies all listeners that a step has occurred and also notifies all listeners
     * of the current step count.
     */
    private void onStepDetected(long timestamp, float[] values){
        stepCount++;
        for (OnStepListener stepListener : mStepListeners){
            stepListener.onStepDetected(timestamp, values);
            stepListener.onStepCountUpdated(stepCount);
        }
    }
}
