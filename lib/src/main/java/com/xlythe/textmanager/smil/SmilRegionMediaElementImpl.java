package com.xlythe.textmanager.smil;

import org.w3c.dom.NodeList;

public class SmilRegionMediaElementImpl extends SmilMediaElementImpl implements
        SMILRegionMediaElement {
    private SMILRegionElement mRegion;

    SmilRegionMediaElementImpl(SmilDocumentImpl owner, String tagName) {
        super(owner, tagName);
    }

    public SMILRegionElement getRegion() {
        if (mRegion == null) {
            SMILDocument doc = (SMILDocument)this.getOwnerDocument();
            NodeList regions = doc.getLayout().getElementsByTagName("region");
            SMILRegionElement region = null;
            for (int i = 0; i < regions.getLength(); i++) {
                region = (SMILRegionElement)regions.item(i);
                if (region.getId().equals(this.getAttribute("region"))) {
                    mRegion = region;
                }
            }
        }
        return mRegion;
    }

    public void setRegion(SMILRegionElement region) {
        this.setAttribute("region", region.getId());
        mRegion = region;
    }

}
