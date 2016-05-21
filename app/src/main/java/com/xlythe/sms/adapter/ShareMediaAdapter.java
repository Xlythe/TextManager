package com.xlythe.sms.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xlythe.sms.R;
import com.xlythe.sms.drawable.ProfileDrawable;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.Thread;
import com.xlythe.textmanager.text.util.Utils;

import java.util.Set;

public class ShareMediaAdapter extends SelectableAdapter<Set<Contact>, ShareMediaAdapter.ViewHolder> {
    private static final int CACHE_SIZE = 50;

    private final Context mContext;
    private Thread.ThreadCursor mCursor;
    private final LruCache<Integer, Thread> mThreadLruCache = new LruCache<>(CACHE_SIZE);
    private OnClickListener mOnClickListener;

    public ShareMediaAdapter(Context context, Thread.ThreadCursor cursor) {
        mContext = context;
        mCursor = cursor;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    public static abstract class ViewHolder extends RecyclerView.ViewHolder {
        private Thread mThread;
        private Context mContext;
        private OnClickListener mOnClickListener;
        private boolean mIsSelected;
        private boolean mSelectMode;

        public ViewHolder(View view) {
            super(view);
        }

        public void setThread(Context context, Thread thread, OnClickListener onClickListener, boolean isSelected, boolean selectMode) {
            mThread = thread;
            mContext = context;
            mOnClickListener = onClickListener;
            mIsSelected = isSelected;
            mSelectMode = selectMode;
        }

        public Thread getThread() {
            return mThread;
        }

        public Context getContext() {
            return mContext;
        }

        public OnClickListener getOnClickListener() {
            return mOnClickListener;
        }

        public boolean getIsSelected() {
            return mIsSelected;
        }

        public boolean getSelectMode() {
            return mSelectMode;
        }
    }

    public static class ThreadViewHolder extends ViewHolder implements View.OnClickListener {
        public final LinearLayout background;
        public final TextView title;
        public final ImageView profile;
        public final CheckBox checkBox;
        public Set<Contact> contacts;

        public ThreadViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);
            background = (LinearLayout) view.findViewById(R.id.background);
            title = (TextView) view.findViewById(R.id.name);
            profile = (ImageView) view.findViewById(R.id.icon);
            checkBox = (CheckBox) view.findViewById(R.id.checkbox);
        }

        @Override
        public void setThread(Context context, Thread thread, OnClickListener onClickListener, boolean isSelected, boolean selectMode) {
            super.setThread(context, thread, onClickListener, isSelected, selectMode);
            createView();
        }

        public void createView() {
            if (getIsSelected()) {
                checkBox.setChecked(true);
                background.setBackgroundColor(Color.rgb(229, 244, 243));
            } else {
                checkBox.setChecked(false);
                background.setBackgroundColor(Color.TRANSPARENT);
            }

            Text latest = getThread().getLatestMessage();

            contacts = TextManager.getInstance(getContext()).getMembersExceptMe(latest).get();

            String address = Utils.join(", ", contacts, new Utils.Rule<Contact>() {
                @Override
                public String toString(Contact contact) {
                    return contact.getDisplayName();
                }
            });

            title.setText(address);

            profile.setBackgroundResource(android.R.color.transparent);
            if (!TextUtils.isEmpty(address)) {
                profile.setImageDrawable(new ProfileDrawable(getContext(), contacts));
            } else {
                profile.setImageDrawable(null);
            }
        }

        @Override
        public void onClick(View v) {
            getOnClickListener().onClick(contacts);
        }
    }

    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(mContext).inflate(R.layout.list_item_share_media, parent, false);
        return new ThreadViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        Set<Contact> contacts = TextManager.getInstance(mContext).getMembersExceptMe(getThread(position).getLatestMessage()).get();
        boolean isSelected = isSelected(contacts);
        boolean selectMode = selectMode();

        holder.setThread(mContext, getThread(position), mOnClickListener, isSelected, selectMode);
    }

    @Override
    public long getItemId(int position) {
        return getThread(position).getIdAsLong();
    }

    public Thread getThread(int position) {
        Thread thread = mThreadLruCache.get(position);
        if (thread == null) {
            mCursor.moveToPosition(position);
            thread = mCursor.getThread();
            mThreadLruCache.put(position, thread);
        }
        return thread;
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    public interface OnClickListener {
        void onClick(Set<Contact> contacts);
    }
}
