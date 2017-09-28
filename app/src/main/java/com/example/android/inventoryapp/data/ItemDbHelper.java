package com.example.android.inventoryapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.inventoryapp.data.ItemContract.ItemEntry;

/**
 * Created by tom.mills-mock on 04/09/2017.
 */

public class ItemDbHelper extends SQLiteOpenHelper {

    public static final String LOG_TAG = ItemDbHelper.class.getSimpleName();

    /**
     * Name of the database file
     */
    private static final String DATABASE_NAME = "item.db";

    /**
     * Database version. If you change the database schema, you must increment the database version.
     */
    private static final int DATABASE_VERSION = 1;

    private static String SQL_CREATE_INVENTORY_TABLE;

    public ItemDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This is called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create a String that contains the SQL statement to create the inventory table
        SQL_CREATE_INVENTORY_TABLE = "CREATE TABLE " + ItemEntry.TABLE_NAME + " ("
                + ItemEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ItemEntry.COLUMN_ITEM_NAME + " TEXT NOT NULL, "
                + ItemEntry.COLUMN_ITEM_QUANTITY + " INTEGER NOT NULL DEFAULT 0, "
                + ItemEntry.COLUMN_ITEM_PRICE + " REAL NOT NULL, "
                + ItemEntry.COLUMN_ITEM_SUPPLIER + " TEXT NOT NULL, "
                + ItemEntry.COLUMN_ITEM_IMAGE + " TEXT);";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_INVENTORY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /*
         * Check the versions and update where applicable. For now, this just returns the current
         * database table, which is better than having just an empty function. In future, this code
         * can be changed to handle any changes to the database that we need to make.
         */

        if (oldVersion < 2 || oldVersion < 3) {
            db.execSQL(SQL_CREATE_INVENTORY_TABLE);
        }
    }
}
