package com.xlythe.textmanager.text.smil;

public interface TimeList {
    /**
     *  Returns the <code>index</code> th item in the collection. If
     * <code>index</code> is greater than or equal to the number of times in
     * the list, this returns <code>null</code> .
     * @param index  Index into the collection.
     * @return  The time at the <code>index</code> th position in the
     *   <code>TimeList</code> , or <code>null</code> if that is not a valid
     *   index.
     */
    public Time item(int index);

    /**
     *  The number of times in the list. The range of valid child time indices
     * is 0 to <code>length-1</code> inclusive.
     */
    public int getLength();

}