package cs.umass.edu.myactivitiestoolkit.services.msband;

//import org.json.JSONException;
//import org.json.JSONObject;

//import edu.umass.cs.MHLClient.sensors.SensorReading;

public class HeartRateReading {

    public double heartRate;
    public long timestamp;
    public int id = -1;

    public HeartRateReading(long t, double heartRate){
        this.heartRate = heartRate;
        this.timestamp = t;
    }

    public HeartRateReading(int id, long t, double heartRate){
        this.id = id;
        this.heartRate = heartRate;
        this.timestamp = t;
    }

    // keeping stuff below just in case we go back to using python

    public HeartRateReading(String userID, String deviceType, String deviceID, long t, double heartRate){
//        super(userID, deviceType, deviceID, "BAND_HR", t);

        this.heartRate = heartRate;
        this.timestamp = t;
    }

    public HeartRateReading(int id, String userID, String deviceType, String deviceID, long t, double heartRate){
//        super(userID, deviceType, deviceID, "BAND_HR", t);

        this.heartRate = heartRate;
        this.timestamp = t;
        this.id = id;
    }

    public HeartRateReading(){}

//    @Override
//    protected JSONObject toJSONObject(){
//        JSONObject obj = getBaseJSONObject();
//        JSONObject data = new JSONObject();
//
//        try {
//            data.put("t", timestamp);
//            data.put("heartRate", heartRate);
//
//            obj.put("data", data);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        return obj;
//    }
}
