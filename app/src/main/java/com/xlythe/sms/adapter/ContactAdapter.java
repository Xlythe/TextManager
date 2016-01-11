package com.xlythe.sms.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xlythe.sms.R;
import com.xlythe.sms.util.DateFormatter;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.Text;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.MessageViewHolder> {
    private static final String TAG = ContactAdapter.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final int CACHE_SIZE = 50;

    private final Context mContext;
    private Contact.ContactCursor mCursor;
    private final LruCache<Integer, Contact> mContactLruCache = new LruCache<>(CACHE_SIZE);

    public ContactAdapter(Context context, Contact.ContactCursor cursor) {
        mContext = context;
        mCursor = cursor;
    }

    public static abstract class MessageViewHolder extends RecyclerView.ViewHolder {
        private Contact mContact;
        private Context mContext;

        public MessageViewHolder(View v) {
            super(v);
        }

        public void setContact(Context context, Contact contact) {
            mContact = contact;
            mContext = context;
        }

        public Contact getContact() {
            return mContact;
        }

        public Context getContext() {
            return mContext;
        }
    }

    public static class ViewHolder extends MessageViewHolder {
        public TextView mTextView;

        public ViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.section_text);
        }

        public void setContact(Context context, Contact contact) {
            super.setContact(context, contact);
            setBodyText(contact.getDisplayName());
        }

        public void setBodyText(String body) {
            mTextView.setText(body);
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(mContext).inflate(R.layout.section, parent, false);
        return new ViewHolder(layout);
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    private Contact getContact(int position) {
        Contact contact = mContactLruCache.get(position);
        if (contact == null) {
            mCursor.moveToPosition(position);
            contact = mCursor.getContact();
            mContactLruCache.put(position, contact);
        }
        return contact;
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        holder.setContact(mContext, getContact(position));
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void destroy() {
        if (!mCursor.isClosed()) {
            mCursor.close();
        }
    }

    public void swapCursor(Contact.ContactCursor cursor) {
        if (!mCursor.isClosed()) {
            mCursor.close();
        }

        mCursor = cursor;
        mContactLruCache.evictAll();
        notifyDataSetChanged();
    }
}