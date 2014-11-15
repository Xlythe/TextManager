package com.xlythe.demo.text;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

public class NewTextActivity extends ActionBarActivity {

    /**
     * onCreate is where the UI is created in Android. This is called when the
     * activity is first opened, when the phone is rotated, or when the system
     * has cached the app because it's been in the background for a long time.
     * savedInstanceState is null unless the UI has been cached.
     * */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout from /res/layout/activity_new_text.xml
        setContentView(R.layout.activity_new_text);

        // Update the action bar to show the home button in the top left
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * onOptionsItemSelected is called when an item in the ActionBar has been
     * clicked or, on older phones, when an item from the menu is clicked.
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case android.R.id.home:
            // Go home when the home button is clicked
            NavUtils.navigateUpTo(this, new Intent(getBaseContext(), MainActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
