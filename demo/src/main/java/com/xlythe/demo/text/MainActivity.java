package com.xlythe.demo.text;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity {

    /**
     * onCreate is where the UI is created in Android. This is called when the
     * activity is first opened, when the phone is rotated, or when the system
     * has cached the app because it's been in the background for a long time.
     * savedInstanceState is null unless the UI has been cached.
     * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout from /res/layout/activity_main.xml
        setContentView(R.layout.activity_main);

        if(savedInstanceState == null) {
            // Creates and starts the fragment for the List
            getSupportFragmentManager().beginTransaction().add(R.id.content_frame, new ThreadListFragment()).commit();
        }
    }

    /**
     * onOptionsItemSelected is called when an item in the ActionBar has been
     * clicked or, on older phones, when an item from the menu is clicked.
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Iterate over the items, returning true if we handle the action.
        switch(item.getItemId()) {
        case R.id.add:
            // Add has been clicked.

            // Create a new intent that points to the NewRoomActivity
            Intent intent = new Intent(getBaseContext(), NewTextActivity.class);

            // Launch the activity
            startActivity(intent);

            // Return true because we handled the action
            return true;
        default:
            // Do the default action
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * onCreateOptionsMenu is called when loading items for the ActionBar or, on
     * older phones, when the menu button has been clicked.
     * */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Adds the "Add" button to the ActionBar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        // Return true because we have buttons
        return true;
    }
}
