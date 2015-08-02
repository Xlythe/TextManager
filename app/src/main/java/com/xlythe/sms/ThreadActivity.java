package com.xlythe.sms;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ScrollView;

import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;


public class ThreadActivity extends FragmentActivity {
    public static String EXTRA_THREAD_ID = "threadId";
    public static String EXTRA_ADDRESS = "address";
    public static String EXTRA_NUMBER = "number";

    private ActionBar mActionBar;
    private AttachView mAttachView;
    private FrameLayout mMessages;
    private ImageButton mButton;
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    private static final int NUM_PAGES = 1;

    private TextAdapter mTextAdapter;
    private ListView mListView;
    private ImageButton mSend;
    private EditText mMessage;
    private TextManager mManager;
    private String mAddress;
    private String mNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.activity_thread);

        mManager = TextManager.getInstance(getBaseContext());
        mListView = (ListView) findViewById(R.id.messages);
        mSend = (ImageButton) findViewById(R.id.send);
        mMessage = (EditText) findViewById(R.id.message);

        // Get threadId that was clicked.
        final long mThreadId = getIntent().getLongExtra(EXTRA_THREAD_ID, -1);

        // Get address.
        mAddress = getIntent().getStringExtra(EXTRA_ADDRESS);
        mNumber = getIntent().getStringExtra(EXTRA_NUMBER);

        // Color bars to match thread color.
        Window window = getWindow();
        window.setStatusBarColor(ColorUtils.getDarkColor(mThreadId));
        getActionBar().setBackgroundDrawable(new ColorDrawable(ColorUtils.getColor(mThreadId)));
        getActionBar().setTitle(mAddress);

        // Populate Adapter with list of texts.
        mTextAdapter = new TextAdapter(getBaseContext(), R.layout.list_item_texts, mManager.getMessages(mThreadId));
        mListView.setAdapter(mTextAdapter);

        // Delete a message on long press.
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
                Text text = (Text) v.getTag();
                //mManager.delete(text);
                return true;
            }
        });

        // Send message.
        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextManager manager = TextManager.getInstance(getBaseContext());
                manager.send(new Text.Builder()
                                .message(mMessage.getText().toString())
                                .recipient(mNumber)
                                .build()
                );
                mMessage.setText("");
            }
        });

        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        mAttachView = (AttachView) findViewById(R.id.attach_view);
        mMessages = (FrameLayout) findViewById(R.id.messages_xxx);
        mAttachView.setUpperView(mMessages);
        mActionBar = getActionBar();
        mAttachView.setActionBar(mActionBar);

        mButton = (ImageButton) findViewById(R.id.attach);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAttachView.partial();
            }
        });
    }

    public void setActiveScrollView(ScrollView sv) {
        mAttachView.setScrollView(sv);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_conversation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return new com.xlythe.sms.ScreenSlidePageFragment();
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }
}
