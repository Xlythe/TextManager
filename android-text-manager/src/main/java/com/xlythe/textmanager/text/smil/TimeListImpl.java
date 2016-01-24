package com.xlythe.textmanager.text.smil;

import java.util.ArrayList;

public class TimeListImpl implements TimeList {
    private final ArrayList<Time> mTimes;

    /*
     * Internal Interface
     */
    TimeListImpl(ArrayList<Time> times) {
        mTimes = times;
    }

    /*
     * TimeList Interface
     */

    public int getLength() {
        return mTimes.size();
    }

    public Time item(int index) {
        Time time = null;
        try {
            time = mTimes.get(index);
        } catch (IndexOutOfBoundsException e) {
            // Do nothing and return null
        }
        return time;
    }

}
