package rockscience.climbingscore;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bentopia on 6/8/2015.
 */
public class DatabaseHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "climbHistory",
            TABLE_HISTORY = "history",
            KEY_ID = "id",
            KEY_NAME = "name",
            KEY_SCORE = "score",
            KEY_CLIMBID = "climbid",
            KEY_START = "start",
            KEY_FINISH = "finish";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_HISTORY + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_NAME + " TEXT," + KEY_SCORE + " TEXT," + KEY_CLIMBID + " TEXT," + KEY_START + " TEXT," + KEY_FINISH + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);

        onCreate(db);
    }

    public void createClimb(Climb climb) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(KEY_NAME, climb.getName());
        values.put(KEY_SCORE, climb.getScore());
        values.put(KEY_CLIMBID, climb.getClimbid());
        values.put(KEY_START, climb.getStart());
        values.put(KEY_FINISH, climb.getFinish());

        db.insert(TABLE_HISTORY, null, values);
        db.close();
    }

    public Climb getClimb(int id) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(TABLE_HISTORY, new String[] { KEY_ID, KEY_NAME, KEY_SCORE, KEY_CLIMBID, KEY_START, KEY_FINISH }, KEY_ID + "=?", new String[] { String.valueOf(id) }, null, null, null, null );

        if (cursor != null)
            cursor.moveToFirst();

        Climb climb = new Climb(Integer.parseInt(cursor.getString(0)), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5));
        db.close();
        cursor.close();
        return climb;
    }

    public void deleteClimb(Climb contact) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_HISTORY, KEY_ID + "=?", new String[]{String.valueOf(contact.getId())});
        db.close();
    }

    public int getClimbCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_HISTORY, null);
        int count = cursor.getCount();
        db.close();
        cursor.close();

        return count;
    }

    public List<Climb> getAllClimbs() {
        List<Climb> climb = new ArrayList<Climb>();

        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_HISTORY, null);

        if (cursor.moveToFirst()) {
            do {
                climb.add(new Climb(Integer.parseInt(cursor.getString(0)), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5)));
            }
            while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return climb;
    }
}
