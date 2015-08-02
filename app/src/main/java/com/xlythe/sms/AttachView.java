package com.xlythe.sms;

import android.app.ActionBar;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ScrollView;

/**
 * Created by Niko on 7/30/15.
 */
public class AttachView extends FrameLayout {
    private static float PARTIAL_PERCENT = 1f - 0.45f;

    private CardState mState;
    private AttachView mAttachView;
    private ScrollView mScrollView;
    private float mLastX;
    private float mLastY;
    private float mStartY;
    private boolean mIsBeingDragged;
    private float mTouchSlop;
    private VelocityTracker mVelocityTracker;
    private int mMinimumFlingVelocity;
    private int mMaximumFlingVelocity;
    private float mLastDeltaY;
    private ActionBar mActionBar;
    private FrameLayout mView;
    private float mInitPercent = -1;



    public AttachView(Context context) {
        super(context);
        setup();
    }

    public AttachView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public AttachView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    public AttachView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup();
    }

    private void setup() {
        ViewConfiguration vc = ViewConfiguration.get(getContext());
        mMinimumFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaximumFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mTouchSlop = vc.getScaledTouchSlop();
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mAttachView.setTranslationY(mAttachView.getHeight());
                setCardState(AttachView.CardState.COLLAPSED);
            }
        });
    }

    public enum CardState {
        EXPANDED, COLLAPSED, PARTIAL
    }

    private void setCardState(CardState state) {
        mState = state;
        if (state == CardState.EXPANDED) {
            // disable view pager
        } else {
            // enable view pager
        }

    }

    public void setUpperView(FrameLayout view){
        mView = view;
    }

    public void setActionBar(ActionBar actionBar){
        mActionBar = actionBar;
    }

    public void setScrollView(ScrollView sv){
        mScrollView = sv;
    }

    public void expand(){
        if(mActionBar!=null)
        mActionBar.show();
        if(mView!=null)
        mView.animate().translationY(-getHeight()).setDuration(300);
        mAttachView.animate().translationY(0f).setDuration(300);
        setCardState(CardState.EXPANDED);
    }
    public void partial(){
        if(mActionBar!=null)
            mActionBar.hide();
        if(mView!=null)
            mView.animate().translationY((PARTIAL_PERCENT - 1f) * getHeight()).setDuration(300);
        mAttachView.animate().translationY(PARTIAL_PERCENT * getHeight()).setDuration(300);
        setCardState(CardState.PARTIAL);
    }
    public void collapse(){

        if(mActionBar!=null)
            mActionBar.show();
        if(mView!=null)
            mView.animate().translationY(0f).setDuration(300);
        mAttachView.animate().translationY(getHeight()).setDuration(300);
        setCardState(CardState.COLLAPSED);
    }

    public void dragView(float percent) {
        if(mState == CardState.PARTIAL) {
            if (mInitPercent == -1 && percent < 0.6) {
                return;
            }
            else if (mInitPercent == -1 && percent > 0.6){
                mInitPercent = percent;
            }
            if (mView != null)
                mView.animate().translationY((percent - mInitPercent + (PARTIAL_PERCENT - 1f)) * getHeight()).setDuration(0);
            mAttachView.animate().translationY((percent - mInitPercent + PARTIAL_PERCENT) * getHeight()).setDuration(0);
        }
        else if(mState == CardState.EXPANDED) {
            if (mInitPercent == -1 && percent > 0 ) {
                mInitPercent = percent;
            }
            else if (percent > 0) {
                if (mView != null)
                    mView.setTranslationY((percent - mInitPercent - 1f) * getHeight());
                mAttachView.setTranslationY((percent - mInitPercent) * getHeight());
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mAttachView = (AttachView) findViewById(R.id.attach_view);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = event.getX();
                mLastY = event.getY();
                mStartY = mLastY;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mIsBeingDragged = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();
                if (mState==CardState.EXPANDED && (y-mLastY)<0){
                    return false;
                }
                if (mState==CardState.EXPANDED && (y-mLastY)>0 && mScrollView.getChildAt(0).canScrollVertically(-1)){
                    return false;
                }

                float xDelta = Math.abs(x - mLastX);
                float yDelta = Math.abs(y - mLastY);

                float yDeltaTotal = y - mStartY;
                if (yDelta > xDelta && Math.abs(yDeltaTotal) > mTouchSlop) {
                    mIsBeingDragged = true;
                    mStartY = y;
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                handleMove(event);
                break;
            case MotionEvent.ACTION_UP:
                handleUp(event);
                mInitPercent = -1;
                mLastY = 0;
                mStartY = 0;
                break;
        }
        return true;
    }

    private void handleMove(MotionEvent event) {
        float percent = getCurrentPercent();
        dragView(percent);
        mLastDeltaY = mLastY - event.getRawY();
        mLastY = event.getRawY();
    }

    private void handleUp(MotionEvent event) {
        mVelocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
        if (Math.abs(mVelocityTracker.getYVelocity()) > mMinimumFlingVelocity) {
            // the sign on velocity seems unreliable, so use last delta to determine direction
            if (mLastDeltaY > 0) {
                expand();
            } else {
                if(mState==CardState.EXPANDED)
                    partial();
                else
                    collapse();
            }
        }
        else {
            if (getCurrentPercent() < 0.5f) {
                expand();
            } else {
                partial();
            }
        }
        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

    private float getCurrentPercent() {
        float percent = (mLastY - mStartY) / getHeight();

        // Start at 100% if open
//        if (mState == CardState.EXPANDED) {
//            percent += 1f;
//        }
        percent = Math.min(Math.max(percent, 0f), 1f);
        return percent;
    }
}
