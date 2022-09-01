package com.xlythe.textmanager.text.smil;

import org.w3c.dom.DOMException;

public interface SMILRegionElement extends SMILElement, ElementLayout {
    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    String getFit();
    void setFit(String fit)
            throws DOMException;

    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    int getLeft();
    void setLeft(int top)
            throws DOMException;

    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    int getTop();
    void setTop(int top)
            throws DOMException;

    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    int getZIndex();
    void setZIndex(int zIndex)
            throws DOMException;

}