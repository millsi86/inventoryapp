package com.example.android.inventoryapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.ItemContract.ItemEntry;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tom.mills-mock on 04/09/2017.
 */

public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    //Identifier for the item data loader
    private static final int EXISTING_ITEM_LOADER = 0;

    // Activity Request code for getting an item image
    private static final int IMAGE_REQUEST = 0;

    // New Image file provider authority
    private static final String FILE_PROVIDER_AUTHORITY =
            "com.example.android.inventoryapp.data.ItemContract.ItemEntry";

    // Uri for the current item
    private Uri mCurrentItemUri;

    // EditText field to enter the item's name
    private EditText mNameEditText;

    // EditText field to enter the item's quantity
    private EditText mQuantityEditText;

    // EditText field for the item's price
    private EditText mPriceEditText;

    // ImageView of the item's picture
    private ImageView itemImageView;

    // Uri of the item's image from the gallery
    private Uri currImageURI;

    // Bitmap of the item image
    private Bitmap bitmap;

    // Local Uri of the image
    private String imageUriString;

    // Flag for if an image has been selected from the gallery
    private boolean galleryImage = false;

    // Boolean flag that keeps track of whether the item has been edited (true) or not (false)
    private boolean mItemHasChanged = false;

    // EditText field to enter the item's supplier contact details
    private EditText mSupplierEditText;

    // EditText field to enter the quantity of the item to change
    private EditText mQuantityChangeEditText;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mItemHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mItemHasChanged = true;
            return false;
        }
    };

    // Content URI for the existing item (null if it's a new item)
    // OnClickListener for if the user pressed the add image button
    View.OnClickListener getImage = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), IMAGE_REQUEST);
        }
    };

    // OnClickListener for the order button to launch an e-mail application
    View.OnClickListener orderEmail = new View.OnClickListener() {
        public void onClick(View v) {
            String[] email = {mSupplierEditText.getText().toString().trim()};
            Intent i = new Intent(Intent.ACTION_SENDTO);
            i.setType("text/plain");
            i.setData(Uri.parse("mailto:"));
            i.putExtra(Intent.EXTRA_EMAIL, email);
            i.putExtra(Intent.EXTRA_SUBJECT, "Order for " + mNameEditText.getText().toString().trim());
            if (i.resolveActivity(getPackageManager()) != null) {
                try {
                    startActivity(i);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(EditorActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    // OnClickListener for the Quantity Change Buttons
    View.OnClickListener quantityAdjustment = new View.OnClickListener() {
        public void onClick(View view) {
            if(TextUtils.isEmpty(mQuantityChangeEditText.getText())){
                Toast.makeText(EditorActivity.this, "Please Enter a Quantity amount to change.", Toast.LENGTH_SHORT).show();
            }
            else {
                Integer QuantityChangeAmount = Integer.valueOf(mQuantityChangeEditText.getText().toString().trim());
                Integer CurrentQuantity = Integer.valueOf(mQuantityEditText.getText().toString().trim());
                Integer newQuantity = 0;
                switch (view.getId()) {
                    case R.id.quantity_decrement_button:
                        if (CurrentQuantity - QuantityChangeAmount >= 0) {
                            newQuantity = CurrentQuantity - QuantityChangeAmount;
                        } else {
                            Toast.makeText(EditorActivity.this, "New Quantity is invalid.", Toast.LENGTH_SHORT).show();
                            newQuantity = CurrentQuantity;
                        }
                        break;
                    case R.id.quantity_increment_button:
                        newQuantity = CurrentQuantity + QuantityChangeAmount;
                        break;
                }
                mQuantityEditText.setText(newQuantity.toString());
            }
        }
    };

    /**
     * method is used for checking valid email id format.
     *
     * @param email
     * @return boolean true for valid false for invalid
     */
    public static boolean isEmailValid(String email) {
        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new item or editing an existing one.
        Intent intent = getIntent();
        mCurrentItemUri = intent.getData();

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_product_name);
        mQuantityEditText = (EditText) findViewById(R.id.edit_product_quantity);
        mPriceEditText = (EditText) findViewById(R.id.edit_product_price);
        mSupplierEditText = (EditText) findViewById(R.id.edit_product_supplier);
        mQuantityChangeEditText = (EditText) findViewById(R.id.quantity_to_change);
        itemImageView = (ImageView) findViewById(R.id.item_image);


        // If the intent DOES NOT contain an item content URI, then we know that we are
        // creating a new item.
        if (mCurrentItemUri == null) {
            // This is a new pet, so change the app bar to say "Add a item"
            setTitle(getString(R.string.new_item));

            // Hide the options to adjust the quantity, order, and delete the product
            findViewById(R.id.quantity_to_change).setVisibility(View.GONE);
            findViewById(R.id.quantity_decrement_button).setVisibility(View.GONE);
            findViewById(R.id.quantity_increment_button).setVisibility(View.GONE);
            findViewById(R.id.quantitylayout).setVisibility(View.GONE);
            findViewById(R.id.dummy1).setVisibility(View.GONE);
            findViewById(R.id.dummy2).setVisibility(View.GONE);
            findViewById(R.id.dummy3).setVisibility(View.GONE);
            findViewById(R.id.dummy4).setVisibility(View.GONE);
            findViewById(R.id.order_product_button).setVisibility(View.GONE);
            findViewById(R.id.line_one).setVisibility(View.INVISIBLE);
            findViewById(R.id.buttonsMiddleSpace).setVisibility(View.GONE);

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a pet that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing pet, so change app bar to say "Edit Pet"
            setTitle(getString(R.string.edit_item));

            // Change the quantity field to be unfocusable to ensure quantity adjustment section
            // is used
            mQuantityEditText.setFocusable(false);

            // Initialize a loader to read the pet data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_ITEM_LOADER, null, this);
        }

        // tup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mNameEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mQuantityChangeEditText.setOnTouchListener(mTouchListener);

        // identify the buttons used by the user
        Button decrement = (Button) findViewById(R.id.quantity_decrement_button);
        Button increment = (Button) findViewById(R.id.quantity_increment_button);
        Button order = (Button) findViewById(R.id.order_product_button);
        Button newImage = (Button) findViewById(R.id.new_image_button);

        // setup click listeners for the buttons
        decrement.setOnClickListener(quantityAdjustment);
        increment.setOnClickListener(quantityAdjustment);
        order.setOnClickListener(orderEmail);
        newImage.setOnClickListener(getImage);

        // hide the keypad on launch of the activity
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    // Once an image has been selected from the gallery this code will run to process the image
    // data to get the Image Uri
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode == Activity.RESULT_OK && resultData != null) {
            galleryImage = false;
            currImageURI = resultData.getData();
            bitmap = getBitmapFromCurrentItemURI(currImageURI);
            itemImageView.setImageBitmap(bitmap);
            imageUriString = getShareableImageUri().toString();
            galleryImage = true;
        }
    }

    private Bitmap getBitmapFromCurrentItemURI(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = null;
            if (parcelFileDescriptor != null)
                fileDescriptor = parcelFileDescriptor.getFileDescriptor();

            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            if (parcelFileDescriptor != null) parcelFileDescriptor.close();

            return image;
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (parcelFileDescriptor != null) parcelFileDescriptor.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Uri getShareableImageUri() {
        Uri imagesUri;
        if (galleryImage) {
            String filename = PathFinder();
            savingInFile(getCacheDir(), filename, bitmap, Bitmap.CompressFormat.JPEG, 100);
            File imagesFile = new File(getCacheDir(), filename);
            imagesUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, imagesFile);
        } else {
            imagesUri = currImageURI;
        }
        return imagesUri;
    }

    public String PathFinder() {

        Cursor returnCursor =
                getContentResolver().query
                        (currImageURI, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);

        if (returnCursor != null) returnCursor.moveToFirst();
        String fileNames = null;
        if (returnCursor != null) fileNames = returnCursor.getString
                (returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

        if (returnCursor != null) returnCursor.close();
        return fileNames;
    }

    public boolean savingInFile(File dir, String fileName, Bitmap bm, Bitmap.CompressFormat format,
                                int quality) {

        File imagesFile = new File(dir, fileName);
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(imagesFile);
            bm.compress(format, quality, fileOutputStream);
            fileOutputStream.close();
            return true;
        } catch (IOException e) {
            if (fileOutputStream != null) try {
                fileOutputStream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return false;
    }

    // runs when the item save button is pressed to pass the data into the SQLite database
    private boolean saveItem() {
        // Read from input fields
        // Use trin to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();

        // Check if this is supposed to be a new pet
        // and check if all of teh fields in the editor are blank
        if (mCurrentItemUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(quantityString) &&
                TextUtils.isEmpty(priceString) && TextUtils.isEmpty(supplierString)) {
            // Since no fields were modified, we can return early without creating a new item.
            // No need to create Contect Values and no need to do an ContentProvider operations.
            return true;
        }

        // check for a valid email address been used?
        if (!isEmailValid(supplierString)) {
            Toast.makeText(this, getString(R.string.invalid_email_address),
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(nameString)) {
            Toast.makeText(this, R.string.product_name_missing,
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(quantityString) || Integer.parseInt(quantityString) < 0) {
            Toast.makeText(this, R.string.product_quantity_invalid,
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(priceString) || Float.parseFloat(quantityString) < 0) {
            Toast.makeText(this, R.string.product_price_invalid,
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(imageUriString)) {
            Toast.makeText(this, R.string.image_missing,
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        // Modify user inputs into correct format
        Integer quantityInteger = Integer.parseInt(quantityString);
        Float priceInteger = Float.parseFloat(priceString.replace(",","."));

        ContentValues values = new ContentValues();
        values.put(ItemEntry.COLUMN_ITEM_NAME, nameString);
        values.put(ItemEntry.COLUMN_ITEM_QUANTITY, quantityInteger);
        values.put(ItemEntry.COLUMN_ITEM_PRICE, priceInteger);
        values.put(ItemEntry.COLUMN_ITEM_SUPPLIER, supplierString);
        values.put(ItemEntry.COLUMN_ITEM_IMAGE, imageUriString);

        // Determine if this is a new or existing item by checking if mCurrentItemUri is null or not
        if (mCurrentItemUri == null) {
            // This is a NEW item, so insert a new item into the provider,
            // returning the content URI for the new item.
            Uri newUri = getContentResolver().insert(ItemEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_item_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an EXISTING item, so update the item with content URI:
            // mCurrentItemUri and pass in the new ContentValues. Pass in null for the selection
            // and selection args because mCurrentItemUri will already identify the correct row in
            // the database that we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentItemUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_item_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        // If the pet hasn't changed, continue with handling back button press
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new pet, hide the "Delete" menu item.
        if (mCurrentItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save pet to database
                boolean saveOk = saveItem();
                // Exit activity
                if (saveOk) {
                    finish();
                }
                return false;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Define a projection that specifies the columns from the table we care about.
        String[] projection = {
                ItemEntry._ID,
                ItemEntry.COLUMN_ITEM_NAME,
                ItemEntry.COLUMN_ITEM_QUANTITY,
                ItemEntry.COLUMN_ITEM_PRICE,
                ItemEntry.COLUMN_ITEM_SUPPLIER,
                ItemEntry.COLUMN_ITEM_IMAGE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentItemUri,   // Provider content URI to query
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        Log.i("EditorActivity", cursor.toString());

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            Log.i("EditorActivity", cursor.toString());
            // Find the columns of item attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME);
            int quantityColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_PRICE);
            int supplierColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_SUPPLIER);
            int imageColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_IMAGE);

            // Read the item attributs from the Cursor for the current pet
            String itemName = cursor.getString(nameColumnIndex);
            int itemQuantity = cursor.getInt(quantityColumnIndex);
            Double itemPrice = Double.valueOf(cursor.getString(priceColumnIndex));
            String itemSupplier = cursor.getString(supplierColumnIndex);
            imageUriString = cursor.getString(imageColumnIndex);

            // formats the price to 2dp
            String itemprice_2dp = String.format("%.2f", itemPrice);

            // update the TextViews with the attributes of the current item
            mNameEditText.setText(itemName);
            mQuantityEditText.setText(Integer.toString(itemQuantity));
            mPriceEditText.setText(itemprice_2dp);
            mSupplierEditText.setText(itemSupplier);

            Uri imageUri = Uri.parse(imageUriString);
            itemImageView.setImageURI(imageUri);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mQuantityEditText.setText("");
        mPriceEditText.setText("");
        mSupplierEditText.setText("");
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);

        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        // adjust the colours of the buttons
        Button buttonPositive = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        buttonPositive.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        Button buttonNegative = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        buttonNegative.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
    }

    /**
     * Prompt the user to confirm that they want to delete this pet.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        // adjust the colours of the buttons
        Button buttonPositive = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        buttonPositive.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        Button buttonNegative = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        buttonNegative.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
    }

    private void deleteItem() {
        // Only perform the delete if this is an existing item
        if (mCurrentItemUri != null) {
            /*
             * Call the ContentResolver to delete the item at the given content URI. Pass in null
             * for the selection and selection args because the currentItemUri content URI already
             * identifies the item that we want.
             */
            int rowsDeleted = getContentResolver().delete(mCurrentItemUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            // if no rows were deleted, then there was an error with the delete
            // Otherwise, the delete was successful and we can display a toast to say so.
            if (rowsDeleted == 0)
                Toast.makeText(this, getString(R.string.editor_delete_item_failed),
                        Toast.LENGTH_SHORT).show();
            else Toast.makeText(this, getString(R.string.editor_delete_item_successful),
                    Toast.LENGTH_SHORT).show();
        }
        // Close the activity
        finish();
    }
}
