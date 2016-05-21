package com.xlythe.sms.adapter;

import android.support.v7.widget.RecyclerView;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class SelectableAdapter<S, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    private final Set<S> mSelectedItems = new HashSet<>();

    public boolean selectMode() {
        return !mSelectedItems.isEmpty();
    }

    public boolean isSelected(S item) {
        return mSelectedItems.contains(item);
    }

    public void toggleSelection(S item) {
        if (mSelectedItems.contains(item)) {
            mSelectedItems.remove(item);
        } else {
            mSelectedItems.add(item);
        }
        notifyDataSetChanged();
    }

    public void clearSelection() {
        mSelectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return mSelectedItems.size();
    }

    public Set<S> getSelectedItems() {
        return Collections.unmodifiableSet(mSelectedItems);
    }
}
