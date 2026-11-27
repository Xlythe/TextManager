package com.xlythe.textmanager.text;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class TelephonyPermissionsTest {
    private Context context;
    private ShadowApplication application;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        application = (ShadowApplication) Shadow.extract(RuntimeEnvironment.getApplication());
        application.denyPermissions(
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_MMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_PHONE_NUMBERS);
    }

    @Test
    public void sendSmsRequiresRuntimePermission() {
        assertFalse(TelephonyPermissions.canSendSms(context));

        application.grantPermissions(Manifest.permission.SEND_SMS);

        assertTrue(TelephonyPermissions.canSendSms(context));
    }

    @Test
    public void phoneNumberLookupIsOptionalForSending() {
        application.grantPermissions(Manifest.permission.SEND_SMS);
        assertFalse(TelephonyPermissions.canReadPhoneNumber(context));

        application.grantPermissions(Manifest.permission.READ_PHONE_NUMBERS);

        assertTrue(TelephonyPermissions.canReadPhoneNumber(context));
    }

    @Test
    public void receiveMmsRequiresRuntimePermission() {
        assertFalse(TelephonyPermissions.canReceiveMms(context));

        application.grantPermissions(Manifest.permission.RECEIVE_MMS);

        assertTrue(TelephonyPermissions.canReceiveMms(context));
    }
}
