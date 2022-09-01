package com.xlythe.textmanager.text.smil;

import org.w3c.dom.DOMException;

public interface ElementTime {
    /**
     *  The desired value (as a list of times) of the  begin instant of this
     * node.
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    TimeList getBegin();
    void setBegin(TimeList begin)
            throws DOMException;

    /**
     *  The list of active  ends for this node.
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    TimeList getEnd();
    void setEnd(TimeList end)
            throws DOMException;

    /**
     *  The desired simple  duration value of this node in seconds. Negative
     * value means "indefinite".
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    float getDur();
    void setDur(float dur)
            throws DOMException;

    // restartTypes
    short RESTART_ALWAYS            = 0;
    short RESTART_NEVER             = 1;
    short RESTART_WHEN_NOT_ACTIVE   = 2;

    /**
     *  A code representing the value of the  restart attribute, as defined
     * above. Default value is <code>RESTART_ALWAYS</code> .
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    short getRestart();
    void setRestart(short restart)
            throws DOMException;

    // fillTypes
    short FILL_REMOVE               = 0;
    short FILL_FREEZE               = 1;
    short FILL_AUTO                 = 2;

    /**
     *  A code representing the value of the  fill attribute, as defined
     * above. Default value is <code>FILL_REMOVE</code> .
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    short getFill();
    void setFill(short fill)
            throws DOMException;

    /**
     *  The  repeatCount attribute causes the element to play repeatedly
     * (loop) for the specified number of times. A negative value repeat the
     * element indefinitely. Default value is 0 (unspecified).
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    float getRepeatCount();
    void setRepeatCount(float repeatCount)
            throws DOMException;

    /**
     *  The  repeatDur causes the element to play repeatedly (loop) for the
     * specified duration in milliseconds. Negative means "indefinite".
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    float getRepeatDur();
    void setRepeatDur(float repeatDur)
            throws DOMException;

    /**
     *  Causes this element to begin the local timeline (subject to sync
     * constraints).
     * @return  <code>true</code> if the method call was successful and the
     *   element was begun. <code>false</code> if the method call failed.
     *   Possible reasons for failure include:  The element doesn't support
     *   the <code>beginElement</code> method. (the <code>beginEvent</code>
     *   attribute is not set to <code>"undefinite"</code> )  The element is
     *   already active and can't be restart when it is active. (the
     *   <code>restart</code> attribute is set to <code>"whenNotActive"</code>
     *    )  The element is active or has been active and can't be restart.
     *   (the <code>restart</code> attribute is set to <code>"never"</code> ).
     *
     */
    boolean beginElement();

    /**
     *  Causes this element to end the local timeline (subject to sync
     * constraints).
     * @return  <code>true</code> if the method call was successful and the
     *   element was endeed. <code>false</code> if method call failed.
     *   Possible reasons for failure include:  The element doesn't support
     *   the <code>endElement</code> method. (the <code>endEvent</code>
     *   attribute is not set to <code>"undefinite"</code> )  The element is
     *   not active.
     */
    boolean endElement();

    /**
     *  Causes this element to pause the local timeline (subject to sync
     * constraints).
     */
    void pauseElement();

    /**
     *  Causes this element to resume a paused local timeline.
     */
    void resumeElement();

    /**
     *  Seeks this element to the specified point on the local timeline
     * (subject to sync constraints).  If this is a timeline, this must seek
     * the entire timeline (i.e. propagate to all timeChildren).
     * @param seekTo  The desired position on the local timeline in
     *   milliseconds.
     */
    void seekElement(float seekTo);

    short getFillDefault();
    void setFillDefault(short fillDefault)
            throws DOMException;

}
