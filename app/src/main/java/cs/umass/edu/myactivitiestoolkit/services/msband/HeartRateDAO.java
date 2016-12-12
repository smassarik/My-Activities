package cs.umass.edu.myactivitiestoolkit.services.msband;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import cs.umass.edu.myactivitiestoolkit.storage.GeneralDAO;

/**
 * Data access object for reports
 *
 * @author Abhinav Parate
 */
public class HeartRateDAO extends GeneralDAO {

    // --------------------------------------------
    // SCHEMA
    // --------------------------------------------

    public static String TABLE_NAME = "gsrreports";

    public static final String TAG = "HeartRateDAO";

    public static final String CNAME_ID = "_id";
    public static final String CNAME_TIMESTAMP = "timestamp";
    public static final String CNAME_RESISTANCE = "resistance";


    public static final String[] PROJECTION = {
            CNAME_ID,
            CNAME_TIMESTAMP,
            CNAME_RESISTANCE
    };

    public final static int CNUM_ID = 0;
    public final static int CNUM_TIMESTAMP = 1;
    public final static int CNUM_RESISTANCE = 2;


    public static final String TABLE_CREATE = "CREATE TABLE " + TABLE_NAME + " (" +
            CNAME_ID + " INTEGER PRIMARY KEY, " +
            CNAME_TIMESTAMP + " LONG, " +
            CNAME_RESISTANCE + " REAL, " +
            ");";

    // --------------------------------------------
    // QUERIES
    // --------------------------------------------

    private final static String WHERE_ID = CNAME_ID + "=?";
    private final static String WHERE_TIME_RANGE = CNAME_TIMESTAMP + ">=?"+" AND "+CNAME_TIMESTAMP + "<=?";

    // --------------------------------------------
    // LIVECYCLE
    // --------------------------------------------

    public HeartRateDAO(Context context) {
        super(context);
    }

    // --------------------------------------------
    // QUERY IMPLEMENTATIONS
    // --------------------------------------------

    public HeartRateReading getLocationById(int id) {
        Cursor c = db.query(
                TABLE_NAME,
                PROJECTION,
                WHERE_ID,
                new String[]{id+""},
                null,
                null,
                null);
        return cursor2location(c);
    }

    public HeartRateReading[] getLocationByTimeRange(long startTime, long endTime) {
        Cursor c = db.query(
                TABLE_NAME,
                PROJECTION,
                WHERE_TIME_RANGE,
                new String[]{startTime+"",endTime+""},
                null,
                null,
                null);
        return cursor2locations(c);
    }

    public HeartRateReading[] getAllLocations() {
        Cursor c = db.query(
                TABLE_NAME,
                PROJECTION,
                null,
                null,
                null,
                null,
                CNAME_TIMESTAMP+" DESC");
        return cursor2locations(c);
    }

    // --------------------------------------------
    // UPDATES
    // --------------------------------------------


    public void insert(HeartRateReading r) {
        ContentValues cv = location2ContentValues(r);
        db.insert(TABLE_NAME, null, cv);
    }

    public void update(HeartRateReading r) {
        ContentValues values = location2ContentValues(r);
        db.update(TABLE_NAME, values , WHERE_ID, new String[]{r.id+""});
    }

    public void delete(HeartRateReading r) {
        Log.d(TAG,"delete report " + r.id);
        db.delete(TABLE_NAME, WHERE_ID, new String[]{r.id+""});
    }

    public void deleteAll() {
        Log.d(TAG,"delete all from " + TABLE_NAME);
        db.delete(TABLE_NAME, null, null);
    }

    // --------------------------------------------
    // LOCATION-CURSOR TRANSFORMATION UTILITIES
    // --------------------------------------------

    private static HeartRateReading cursor2location(Cursor c) {
        c.moveToFirst();
        HeartRateReading r = new HeartRateReading();
        r.id = c.getInt(CNUM_ID);
        r.timestamp =c.getLong(CNUM_TIMESTAMP);
        r.latitude = c.getDouble(CNUM_LATITUDE);
        r.longitude = c.getDouble(CNUM_LONGITUDE);
        r.accuracy = c.getFloat(CNUM_ACCURACY);
        return r;
    }

    public static HeartRateReading[] cursor2locations(Cursor c) {
        c.moveToFirst();
        LinkedList<HeartRateReading> locations = new LinkedList<HeartRateReading>();
        while(!c.isAfterLast()){
            HeartRateReading r = new HeartRateReading();
            r.id = c.getInt(CNUM_ID);
            r.timestamp =c.getLong(CNUM_TIMESTAMP);
            r.latitude = c.getDouble(CNUM_LATITUDE);
            r.longitude = c.getDouble(CNUM_LONGITUDE);
            r.accuracy = c.getFloat(CNUM_ACCURACY);
            locations.add(r);
            c.moveToNext();
        }
        return locations.toArray(new HeartRateReading[locations.size()]);
    }

    private static ContentValues location2ContentValues(HeartRateReading r) {
        ContentValues cv = new ContentValues();
        cv.put(CNAME_TIMESTAMP, r.timestamp);
        cv.put(CNAME_LATITUDE, r.latitude);
        cv.put(CNAME_LONGITUDE, r.longitude);
        cv.put(CNAME_ACCURACY, r.accuracy);
        return cv;
    }

    public static String getISOTimeString(long time) {
        Calendar gc = GregorianCalendar.getInstance();
        gc.setTimeInMillis(time);
        String AM = "AM";
        int day = gc.get(Calendar.DAY_OF_MONTH);
        String ds = (day<10?"0":"")+day;
        int month = (gc.get(Calendar.MONTH)+1);
        String ms = (month<10?"0":"")+month;
        int hour = gc.get(Calendar.HOUR_OF_DAY);
        String hs = "";
        if(hour>=12){ AM = "PM"; if(hour>12) hour = hour-12;}
        hs = (hour<10?"0":"")+hour;
        int min = gc.get(Calendar.MINUTE);
        String mins = (min<10?"0":"")+min;
        String s = gc.get(Calendar.YEAR)+"-"+ms+"-"+ds+" "+hs+":"+mins+" "+AM;
        return s;
    }


}
