package com.xlythe.sms;

public class SmsReceiver extends com.xlythe.textmanager.text.SmsReceiver {
    public SmsReceiver() {
    }
<<<<<<< HEAD

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        //throw new UnsupportedOperationException("Not yet implemented");

        final Bundle bundle = intent.getExtras();

        try {
            if (bundle != null) {
                final Object[] pdusObj = (Object[]) bundle.get("pdus");
                SmsMessage[] messages = new SmsMessage[pdusObj.length];

                for (int i = 0; i < pdusObj.length; i++) {
                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);

                    messages[i] = currentMessage;

                    String number = currentMessage.getDisplayOriginatingAddress();
                    String message = currentMessage.getDisplayMessageBody();

                    Log.i("SmsReciver", number + ": " + message);
                    Toast.makeText(context, number + ": " + message, Toast.LENGTH_LONG).show();
                }

                if(Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction())) {
                    Receive.storeMessage(context, messages, 0);
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            Log.e("SmsReciver", "Exception smsReciver" + e);
        }
    }
=======
>>>>>>> mms
}
