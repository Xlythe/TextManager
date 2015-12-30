package com.xlythe.textmanager.smil;

import org.w3c.dom.DOMException;

public interface SMILRegionElement extends SMILElement, ElementLayout {
    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    public String getFit();
    public void setFit(String fit)
            throws DOMException;

    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    public int getLeft();
    public void setLeft(int top)
            throws DOMException;

    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    public int getTop();
    public void setTop(int top)
            throws DOMException;

    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    public int getZIndex();
    public void setZIndex(int zIndex)
            throws DOMException;

}