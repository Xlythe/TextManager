package com.xlythe.textmanager.text.smil;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

public interface Time {
    /**
     *  A boolean indicating whether the current <code>Time</code> has been
     * fully resolved to the document schedule.  Note that for this to be
     * true, the current <code>Time</code> must be defined (not indefinite),
     * the syncbase and all <code>Time</code> 's that the syncbase depends on
     * must be defined (not indefinite), and the begin <code>Time</code> of
     * all ascendent time containers of this element and all <code>Time</code>
     *  elements that this depends upon must be defined (not indefinite).
     * <br> If this <code>Time</code> is based upon an event, this
     * <code>Time</code> will only be resolved once the specified event has
     * happened, subject to the constraints of the time container.
     * <br> Note that this may change from true to false when the parent time
     * container ends its simple duration (including when it repeats or
     * restarts).
     */
    boolean getResolved();

    /**
     *  The clock value in seconds relative to the parent time container begin.
     *  This indicates the resolved time relationship to the parent time
     * container.  This is only valid if resolved is true.
     */
    double getResolvedOffset();

    // TimeTypes
    short SMIL_TIME_INDEFINITE      = 0;
    short SMIL_TIME_OFFSET          = 1;
    short SMIL_TIME_SYNC_BASED      = 2;
    short SMIL_TIME_EVENT_BASED     = 3;
    short SMIL_TIME_WALLCLOCK       = 4;
    short SMIL_TIME_MEDIA_MARKER    = 5;

    /**
     *  A code representing the type of the underlying object, as defined
     * above.
     */
    short getTimeType();

    /**
     *  The clock value in seconds relative to the syncbase or eventbase.
     * Default value is <code>0</code> .
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised on attempts to modify this
     *   readonly attribute.
     */
    double getOffset();
    void setOffset(double offset)
            throws DOMException;

    /**
     *  The base element for a sync-based or event-based time.
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised on attempts to modify this
     *   readonly attribute.
     */
    Element getBaseElement();
    void setBaseElement(Element baseElement)
            throws DOMException;

    /**
     *  If <code>true</code> , indicates that a sync-based time is relative to
     * the begin of the baseElement.  If <code>false</code> , indicates that a
     *  sync-based time is relative to the active end of the baseElement.
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised on attempts to modify this
     *   readonly attribute.
     */
    boolean getBaseBegin();
    void setBaseBegin(boolean baseBegin)
            throws DOMException;

    /**
     *  The name of the event for an event-based time. Default value is
     * <code>null</code> .
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised on attempts to modify this
     *   readonly attribute.
     */
    String getEvent();
    void setEvent(String event)
            throws DOMException;

    /**
     *  The name of the marker from the media element, for media marker times.
     * Default value is <code>null</code> .
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised on attempts to modify this
     *   readonly attribute.
     */
    String getMarker();
    void setMarker(String marker)
            throws DOMException;

}