package com.xlythe.textmanager.smil;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

public interface SMILElement extends Element {
    /**
     *  The unique id.
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    public String getId();
    public void setId(String id)
            throws DOMException;

}