package com.xlythe.sms.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ScrollView;

import com.xlythe.sms.R;
import com.xlythe.sms.ThreadActivity;
import com.xlythe.sms.adapter.CursorImageAdapter;

public class ScreenSlidePageFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_screen_slide_page, container, false);

        String[] projection = {MediaStore.Images.Thumbnails._ID, MediaStore.Images.Thumbnails.DATA};
        Cursor c = getActivity().getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, projection, null, null, "image_id DESC");

        GridView gridview = (GridView) rootView.findViewById(R.id.gridview);
        gridview.setAdapter(new CursorImageAdapter(getActivity(), c));

        ThreadActivity activity = (ThreadActivity) getActivity();

        ScrollView sv = (ScrollView) rootView.findViewById(R.id.scroll_view);
        activity.setActiveScrollView(sv);

        return rootView;
    }
}