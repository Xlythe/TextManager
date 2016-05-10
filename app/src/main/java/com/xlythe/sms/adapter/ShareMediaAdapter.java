package com.xlythe.sms.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xlythe.sms.R;
import com.xlythe.sms.drawable.ProfileDrawable;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.Thread;
import com.xlythe.textmanager.text.concurrency.Future;
import com.xlythe.textmanager.text.util.Utils;

import java.util.Set;

public class ShareMediaAdapter extends RecyclerView.Adapter<ShareMediaAdapter.ViewHolder> {
    private static final int CACHE_SIZE = 50;

    private final Context mContext;
    private Thread.ThreadCursor mCursor;
    private final LruCache<Integer, Thread> mThreadLruCache = new LruCache<>(CACHE_SIZE);

    public ShareMediaAdapter(Context context, Thread.ThreadCursor cursor) {
        mContext = context;
        mCursor = cursor;
    }

    public static abstract class ViewHolder extends RecyclerView.ViewHolder {
        private Thread mThread;
        private Context mContext;

        public ViewHolder(View view) {
            super(view);
        }

        public void setThread(Context context, Thread thread) {
            mThread = thread;
            mContext = context;
        }

        public Thread getThread() {
            return mThread;
        }

        public Context getContext() {
            return mContext;
        }
    }

    public static class ThreadViewHolder extends ViewHolder {
        public final TextView title;
        public final ImageView profile;

        public ThreadViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.name);
            profile = (ImageView) view.findViewById(R.id.icon);
        }

        @Override
        public void setThread(Context context, Thread thread) {
            super.setThread(context, thread);
            createView();
        }

        public void createView() {
            String address = "";
            Text latest = getThread().getLatestMessage(getContext()).get();

            if (latest != null) {
                address = Utils.join(", ", latest.getMembersExceptMe(getContext()).get(), new Utils.Rule<Contact>() {
                    @Override
                    public String toString(Contact contact) {
                        return contact.getDisplayName();
                    }
                });
            }

            title.setText(address);

            profile.setBackgroundResource(android.R.color.transparent);
            if (!TextUtils.isEmpty(address) && latest != null) {
                latest.getMembersExceptMe(getContext()).get(new Future.Callback<Set<Contact>>() {
                    @Override
                    public void get(Set<Contact> instance) {
                        profile.setImageDrawable(new ProfileDrawable(getContext(), instance));
                    }
                });
            } else {
                profile.setImageDrawable(null);
            }
        }
    }

    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(mContext).inflate(R.layout.list_item_share_media, parent, false);
        return new ThreadViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.setThread(mContext, getThread(position));
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
}
