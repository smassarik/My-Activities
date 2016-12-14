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
public class GsrDAO extends GeneralDAO {

    // --------------------------------------------
    // SCHEMA
    // --------------------------------------------

    public static String TABLE_NAME = "gsrreports";

    public static final String TAG = "GsrDAO";

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
            CNAME_RESISTANCE + " REAL " +
            ");";

    // --------------------------------------------
    // QUERIES
    // --------------------------------------------

    private final static String WHERE_ID = CNAME_ID + "=?";
    private final static String WHERE_TIME_RANGE = CNAME_TIMESTAMP + ">=?"+" AND "+CNAME_TIMESTAMP + "<=?";

    // --------------------------------------------
    // LIVECYCLE
    // --------------------------------------------

    public GsrDAO(Context context) {
        super(context);
    }

    // --------------------------------------------
    // QUERY IMPLEMENTATIONS
    // --------------------------------------------

    public GsrReading getResistanceById(int id) {
        Cursor c = db.query(
                TABLE_NAME,
                PROJECTION,
                WHERE_ID,
                new String[]{id+""},
                null,
                null,
                null);
        return cursor2resistance(c);
    }

    public GsrReading[] getResistanceByTimeRange(long startTime, long endTime) {
        Cursor c = db.query(
                TABLE_NAME,
                PROJECTION,
                WHERE_TIME_RANGE,
                new String[]{startTime+"",endTime+""},
                null,
                null,
                null);
        return cursor2resistances(c);
    }

    public GsrReading[] getAllResistances() {
        Cursor c = db.query(
                TABLE_NAME,
                PROJECTION,
                null,
                null,
                null,
                null,
                CNAME_TIMESTAMP+" DESC");
        return cursor2resistances(c);
    }

    // --------------------------------------------
    // UPDATES
    // --------------------------------------------


    public void insert(GsrReading r) {
        ContentValues cv = resistance2ContentValues(r);
        db.insert(TABLE_NAME, null, cv);
    }

    public void update(GsrReading r) {
        ContentValues values = resistance2ContentValues(r);
        db.update(TABLE_NAME, values , WHERE_ID, new String[]{r.id+""});
    }

    public void delete(GsrReading r) {
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

    private static GsrReading cursor2resistance(Cursor c) {
        c.moveToFirst();
        GsrReading r = new GsrReading();
        r.id = c.getInt(CNUM_ID);
        r.timestamp =c.getLong(CNUM_TIMESTAMP);
        r.resistance = c.getDouble(CNUM_RESISTANCE);
        return r;
    }

    public static GsrReading[] cursor2resistances(Cursor c) {
        c.moveToFirst();
        LinkedList<GsrReading> resistances = new LinkedList<GsrReading>();
        while(!c.isAfterLast()){
            GsrReading r = new GsrReading();
            r.id = c.getInt(CNUM_ID);
            r.timestamp =c.getLong(CNUM_TIMESTAMP);
            r.resistance = c.getDouble(CNUM_RESISTANCE);
            resistances.add(r);
            c.moveToNext();
        }
        return resistances.toArray(new GsrReading[resistances.size()]);
    }

    private static ContentValues resistance2ContentValues(GsrReading r) {
        ContentValues cv = new ContentValues();
        cv.put(CNAME_TIMESTAMP, r.timestamp);
        cv.put(CNAME_RESISTANCE, r.resistance);
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
