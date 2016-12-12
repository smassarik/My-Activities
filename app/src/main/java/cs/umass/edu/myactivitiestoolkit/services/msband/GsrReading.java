package cs.umass.edu.myactivitiestoolkit.services.msband;

//import org.json.JSONException;
//import org.json.JSONObject;

public class GsrReading {

    public double resistance;
    public long timestamp;
    public int id = -1;

    public GsrReading(long t, double resistance){
        this.resistance = resistance;
        this.timestamp = t;
    }

    public GsrReading(int id, long t, double resistance){
        this.id = id;
        this.resistance = resistance;
        this.timestamp = t;
    }


// keeping stuff below just in case we go back to using python

    public GsrReading(String userID, String deviceType, String deviceID, long t, double resistance){
//        super(userID, deviceType, deviceID, "BAND_GSR", t);

        this.resistance = resistance;
        this.timestamp = t;
    }

    public GsrReading(int id, String userID, String deviceType, String deviceID, long t, double resistance){
//        super(userID, deviceType, deviceID, "BAND_GSR", t);

        this.resistance = resistance;
        this.timestamp = t;
        this.id = id;
    }

    public GsrReading(){}

//    @Override
//    protected JSONObject toJSONObject(){
//        JSONObject obj = getBaseJSONObject();
//        JSONObject data = new JSONObject();
//
//        try {
//            data.put("t", timestamp);
//            data.put("resistance", resistance);
//
//            obj.put("data", data);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        return obj;
//    }
}
