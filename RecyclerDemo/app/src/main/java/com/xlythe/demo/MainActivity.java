package com.xlythe.demo;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    RecyclerView mRecyclerView;
    SimpleAdapter mAdapter;
    private float px;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Resources r = getResources();
        px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, r.getDisplayMetrics());

        String[] array = {"Josh Cheston","Oriana","Alex Goldstein","Will Harmon","1 (717) 332-6482",
                "Natalie","Mom","Tim Nerozzi","Alex Bourdakos","Cyrus Basseri","Mark Steffl"};

        //Your RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL, 14));

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                drawVertical(mRecyclerView);
            }
        });

        //Your RecyclerView.Adapter
        mAdapter = new SimpleAdapter(this, array);

        //This is the code to provide a sectioned list
        List<ManagerAdapter.Section> sections = new ArrayList<>();

        //Sections
        sections.add(new ManagerAdapter.Section(0,"Today"));
        sections.add(new ManagerAdapter.Section(2,"Yesterday"));

        //Add your adapter to the sectionAdapter
        ManagerAdapter.Section[] dummy = new ManagerAdapter.Section[sections.size()];
        ManagerAdapter mSectionedAdapter = new ManagerAdapter(this,R.layout.section,R.id.section_text, mAdapter);
        mSectionedAdapter.setSections(sections.toArray(dummy));

        //Apply this adapter to the RecyclerView
        mRecyclerView.setAdapter(mSectionedAdapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void drawVertical(RecyclerView parent) {

        final int childCount = parent.getChildCount();

        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            if (child instanceof TextView) {
                final View childTop = parent.getChildAt(i-1);
                final View childBottom = parent.getChildAt(i+1);
                if (childTop instanceof LinearLayout) {
                    final View card = ((LinearLayout) childTop).getChildAt(0);
                    final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
                    params.setMargins(0, 0, 0, (int)px);
                    card.setLayoutParams(params);
                }
                if (childBottom instanceof LinearLayout) {
                    final View card = ((LinearLayout) childBottom).getChildAt(0);
                    final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
                    params.setMargins(0, (int)px, 0, 0);
                    card.setLayoutParams(params);
                }
            }
            else {
                final View childTop = parent.getChildAt(i-1);
                final View childBottom = parent.getChildAt(i+1);
                if (childTop instanceof LinearLayout) {
                    final View card = ((LinearLayout) child).getChildAt(0);
                    final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
                    int bottom = params.bottomMargin;
                    params.setMargins(0, 0, 0, bottom);
                    card.setLayoutParams(params);
                }
                if (childBottom instanceof LinearLayout) {
                    final View card = ((LinearLayout) child).getChildAt(0);
                    final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
                    int top = params.topMargin;
                    params.setMargins(0, top, 0, 0);
                    card.setLayoutParams(params);
                }
            }
        }
    }
}
