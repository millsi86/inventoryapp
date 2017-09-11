package com.example.android.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.inventoryapp.data.ItemContract.ItemEntry;

import static com.example.android.inventoryapp.R.id.price;

/**
 * Created by tom.mills-mock on 04/09/2017.
 */

public class ItemCursorAdapter extends CursorAdapter {

    public ItemCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, viewGroup, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        final int itemId = cursor.getInt(cursor.getColumnIndexOrThrow(ItemEntry._ID));
        // Find individual views to modify on the list item
        TextView nameTextView = view.findViewById(R.id.name);
        TextView quantityTextView = view.findViewById(R.id.quantity);
        TextView priceTextView = view.findViewById(price);

        // Find the columns of the item attributes that we want
        int nameColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME);
        int quantityColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_QUANTITY);
        int priceColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_PRICE);

        // Read the item attributs from the Cursor for the current pet
        String itemName = cursor.getString(nameColumnIndex);
        final String itemQuantity = cursor.getString(quantityColumnIndex);
        Double itemPrice = Double.valueOf(cursor.getString(priceColumnIndex));

        // formats the price to 2dp
        String itemprice_2dp = String.format("%.2f", itemPrice);

        // update the TextViews with the attributes of the current item
        nameTextView.setText(itemName);
        quantityTextView.setText(itemQuantity);
        priceTextView.setText(itemprice_2dp);

        // On saleButton press decrement the quantity and update teh db
        Button saleButton = view.findViewById(R.id.sale_button);
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view1) {
                ContentValues values = new ContentValues();
                Integer quantity = Integer.parseInt(itemQuantity);
                if (quantity > 0) {
                    quantity--;
                    values.put(ItemEntry.COLUMN_ITEM_QUANTITY, quantity);
                    Uri uri = ContentUris.withAppendedId(ItemEntry.CONTENT_URI, itemId);
                    context.getContentResolver().update(uri, values, null, null);
                }
            }
        });
    }
}
