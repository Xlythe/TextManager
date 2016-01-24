package com.xlythe.textmanager.text.smil;

import org.w3c.dom.Document;

public interface SMILDocument extends Document, ElementSequentialTimeContainer {

    /**
     * Returns the element that contains the layout node of this document,
     * i.e. the <code>HEAD</code> element.
     */
    public SMILElement getHead();

    /**
     * Returns the element that contains the par's of the document, i.e. the
     * <code>BODY</code> element.
     */
    public SMILElement getBody();

    /**
     * Returns the element that contains the layout information of the presentation,
     * i.e. the <code>LAYOUT</code> element.
     */
    public SMILLayoutElement getLayout();
}