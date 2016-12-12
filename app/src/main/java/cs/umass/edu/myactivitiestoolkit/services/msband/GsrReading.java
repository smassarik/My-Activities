package cs.umass.edu.myactivitiestoolkit.services.msband;

import org.json.JSONException;
import org.json.JSONObject;

import edu.umass.cs.MHLClient.sensors.SensorReading;

public class GsrReading extends SensorReading {

    protected final double resistance;
    protected final long timestamp;

    public GsrReading(String userID, String deviceType, String deviceID, long t, double resistance){
        super(userID, deviceType, deviceID, "BAND_GSR", t);

        this.resistance = resistance;
        this.timestamp = t;
    }

    @Override
    protected JSONObject toJSONObject(){
        JSONObject obj = getBaseJSONObject();
        JSONObject data = new JSONObject();

        try {
            data.put("t", timestamp);
            data.put("resistance", resistance);

            obj.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }
}
