package com.xlythe.textmanager.text.smil;

import org.w3c.dom.DOMException;

public interface ElementLayout {
    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    String getTitle();
    void setTitle(String title)
            throws DOMException;

    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    String getBackgroundColor();
    void setBackgroundColor(String backgroundColor)
            throws DOMException;

    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    int getHeight();
    void setHeight(int height)
            throws DOMException;

    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    int getWidth();
    void setWidth(int width)
            throws DOMException;

}