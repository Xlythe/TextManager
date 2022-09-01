package com.xlythe.textmanager.text.smil;

import org.w3c.dom.DOMException;

public interface ElementParallelTimeContainer extends ElementTimeContainer {
    /**
     *  Controls the end of the container.  Need to address thr id-ref value.
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    String getEndSync();
    void setEndSync(String endSync)
            throws DOMException;

    /**
     *  This method returns the implicit duration in seconds.
     * @return  The implicit duration in seconds or -1 if the implicit is
     *   unknown (indefinite?).
     */
    float getImplicitDuration();

}