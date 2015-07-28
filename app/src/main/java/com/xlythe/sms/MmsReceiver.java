package com.xlythe.sms;

<<<<<<< HEAD
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MmsReceiver extends BroadcastReceiver {
    public MmsReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        throw new UnsupportedOperationException("Not yet implemented");
    }
=======
public class MmsReceiver extends com.xlythe.textmanager.text.MmsReceiver {
    public MmsReceiver() {
    }
>>>>>>> mms
}
