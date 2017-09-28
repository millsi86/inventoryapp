package com.example.android.inventoryapp.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by tom.mills-mock on 04/09/2017.
 */

public class ItemContract {

    // Unique Content Authority
    public static final String CONTENT_AUTHORITY = "com.example.android.inventoryapp";
    /**
     * Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
     * the content provider.
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    /**
     * Path for individual shop Item
     */
    public static final String PATH_ITEMS = "items";

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private ItemContract() {
    }

    /**
     * Inner class that defines constant values for the items database table.
     * Each entry in the table represents a single pet.
     */
    public static final class ItemEntry implements BaseColumns {
        /**
         * The content URI to access the item data in the provider
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_ITEMS);

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of items.
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ITEMS;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single item.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ITEMS;

        /**
         * Name of database table for items
         */
        public final static String TABLE_NAME = "inventory";

        /**
         * Unique ID number for the item (only for use in the database table), Type: INTEGER
         */
        public final static String _ID = BaseColumns._ID;

        /**
         * Name of the item., Type: TEXT
         */
        public final static String COLUMN_ITEM_NAME = "name";

        /**
         * Quantity of the item, Type: INTEGER
         */
        public final static String COLUMN_ITEM_QUANTITY = "quantity";

        /**
         * Price of the product, Type: REAL
         */
        public static final String COLUMN_ITEM_PRICE = "price";

        /**
         * Product supplier contect e-mail address, Type: STRING
         */
        public static final String COLUMN_ITEM_SUPPLIER = "supplier";

        /**
         * Product image, Type: BLOB
         */
        public static final String COLUMN_ITEM_IMAGE = "image";

        /**
         * Returns whether or not the given quantity value is allowed
         */
        public static boolean isValidQuantity(int quantity) {
            return quantity >= 0;
        }
    }
}
