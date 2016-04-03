package com.xlythe.sms.service;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;

import com.xlythe.sms.MessageActivity;
import com.xlythe.sms.drawable.ProfileDrawable;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.Thread;

import java.util.ArrayList;
import java.util.List;

@TargetApi(23)
public class FetchChooserTargetService extends ChooserTargetService {
    private static final int SIZE = 3;

    private TextManager mManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mManager = TextManager.getInstance(this);
    }

    @Override
    public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName, IntentFilter matchedFilter) {
        final List<ChooserTarget> targets = new ArrayList<>();

        final ComponentName componentName = new ComponentName(this, MessageActivity.class);

        List<Thread> recentThreads = getRecentThreads();
        for (Thread thread : recentThreads) {
            final String title = getTitle(thread);
            final Icon icon = getIcon(thread);
            final float score = 1.0f - ((float) recentThreads.indexOf(thread) / recentThreads.size());
            final Bundle extras = new Bundle();
            extras.putString(MessageActivity.EXTRA_THREAD_ID, thread.getId());

            targets.add(new ChooserTarget(title, icon, score, componentName, extras));
        }

        return targets;
    }

    private String getTitle(Thread thread) {
        String title = "";
        for (Contact member : thread.getLatestMessage().getMembersExceptMe(this)) {
            if (!title.isEmpty()){
                title += ", ";
            }
            title += member.getDisplayName();
        }
        return title;
    }

    private Icon getIcon(Thread thread) {
        ProfileDrawable drawable = new ProfileDrawable(this, thread.getLatestMessage().getMembersExceptMe(this));

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.draw(canvas);

        return Icon.createWithBitmap(bitmap);
    }

    private List<Thread> getRecentThreads() {
        List<Thread> recentThreads = new ArrayList<>(SIZE);
        Thread.ThreadCursor cursor = mManager.getThreadCursor();
        try {
            while (cursor.moveToNext() && recentThreads.size() < SIZE) {
                if (cursor.getThread().getLatestMessage().getMembersExceptMe(this).size() == 0) {
                    // Ignore corrupted texts
                    continue;
                }
                recentThreads.add(cursor.getThread());
            }
        } finally {
            cursor.close();
        }
        return recentThreads;
    }
}
