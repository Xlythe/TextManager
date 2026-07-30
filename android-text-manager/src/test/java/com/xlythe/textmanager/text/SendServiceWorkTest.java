package com.xlythe.textmanager.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.work.Configuration;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class SendServiceWorkTest {
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        Configuration configuration = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration);
    }

    @Test
    public void scheduleExecutesPersistentSendWork() throws Exception {
        Text text = new Text.DebugBuilder()
                .setSender("+15551234567")
                .message("hello")
                .build();

        SendService.schedule(context, text);

        List<WorkInfo> work = WorkManager.getInstance(context)
                .getWorkInfosByTag(SendService.WORK_TAG)
                .get(5, TimeUnit.SECONDS);
        assertEquals(1, work.size());
        assertEquals(WorkInfo.State.SUCCEEDED, work.get(0).getState());
        assertTrue(work.get(0).getTags().contains(SendService.class.getName()));
    }
}