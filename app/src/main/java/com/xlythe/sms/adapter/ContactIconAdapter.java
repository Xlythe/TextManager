package com.xlythe.sms.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.xlythe.sms.R;
import com.xlythe.sms.drawable.ProfileDrawable;
import com.xlythe.textmanager.text.Contact;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ContactIconAdapter extends RecyclerView.Adapter<ContactIconAdapter.ViewHolder> {

    private final Context mContext;
    private final List<Set<Contact>> mContactsSet = new ArrayList<>();
    private OnClickListener mOnClickListener;

    public ContactIconAdapter(Context context, Set<Set<Contact>> contactsSet) {
        mContext = context;
        mContactsSet.addAll(contactsSet);
    }

    public void updateData(Set<Set<Contact>> contactsSet) {
        for (Set<Contact> contacts: contactsSet) {
            if (!mContactsSet.contains(contacts)) {
                mContactsSet.add(contacts);
            }
        }

        Iterator<Set<Contact>> iter = mContactsSet.iterator();

        while (iter.hasNext()) {
            Set<Contact> contacts = iter.next();

            if (!contactsSet.contains(contacts))
                iter.remove();
        }

        notifyDataSetChanged();
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    public static abstract class ViewHolder extends RecyclerView.ViewHolder implements ContactIconAdapter.OnClickListener {
        private Set<Contact> mContacts;
        private Context mContext;
        private OnClickListener mOnClickListener;

        public ViewHolder(View view) {
            super(view);
        }

        public void setContacts(Context context, Set<Contact> contacts, OnClickListener onClickListener) {
            mContacts = contacts;
            mContext = context;
            mOnClickListener = onClickListener;
        }

        public Set<Contact> getContacts() {
            return mContacts;
        }

        public Context getContext() {
            return mContext;
        }

        public OnClickListener getOnClickListener() {
            return mOnClickListener;
        }
    }

    public static class ContactViewHolder extends ViewHolder {
        public final ImageView profile;

        public ContactViewHolder(View view) {
            super(view);
            profile = (ImageView) view.findViewById(R.id.icon);
        }

        @Override
        public void setContacts(Context context, Set<Contact> contacts, OnClickListener onClickListener) {
            super.setContacts(context, contacts, onClickListener);
            createView();
        }

        public void createView() {
            profile.setImageDrawable(new ProfileDrawable(getContext(), getContacts()));
            profile.setOnClickListener(v -> getOnClickListener().onClick(getContacts()));
        }

        @Override
        public void onClick(Set<Contact> contacts) {

        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(mContext).inflate(R.layout.list_item_contact_icon, parent, false);
        return new ContactViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.setContacts(mContext, mContactsSet.get(position), mOnClickListener);
    }

    @Override
    public int getItemCount() {
        return mContactsSet.size();
    }

    public interface OnClickListener {
        void onClick(Set<Contact> contacts);
    }
}

