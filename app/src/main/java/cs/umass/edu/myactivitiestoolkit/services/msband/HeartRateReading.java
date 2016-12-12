package cs.umass.edu.myactivitiestoolkit.services.msband;

import org.json.JSONException;
import org.json.JSONObject;

import edu.umass.cs.MHLClient.sensors.SensorReading;

public class HeartRateReading extends SensorReading {

    private final double heartRate;

    public HeartRateReading(String userID, String deviceType, String deviceID, long t, double heartRate){
        super(userID, deviceType, deviceID, "BAND_HR", t);

        this.heartRate = heartRate;
    }

    @Override
    protected JSONObject toJSONObject(){
        JSONObject obj = getBaseJSONObject();
        JSONObject data = new JSONObject();

        try {
            data.put("t", timestamp);
            data.put("heartRate", heartRate);

            obj.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }
}
