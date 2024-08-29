package id.co.bankntbsyariah.lakupandai.ui;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "SignatureDB";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_SIGNATURES = "signatures";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_SIGNATURE = "signature";

    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SIGNATURES_TABLE = "CREATE TABLE " + TABLE_SIGNATURES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_SIGNATURE + " TEXT" + ")";
        db.execSQL(CREATE_SIGNATURES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SIGNATURES);
        onCreate(db);
    }

    public void insertSignature(String signature) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SIGNATURE, signature);

        db.insert(TABLE_SIGNATURES, null, values);
        db.close();
    }
}
