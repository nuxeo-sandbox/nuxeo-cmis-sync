package org.nuxeo.ecm.sync.cmis.service;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("ace-mapping")
public class CMISAceMappingDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@remoteACE")
    protected String remoteACE;

    @XNode("@localACE")
    protected String localACE;

    public CMISAceMappingDescriptor() {
        super();
    }

    public String getRemoteACE() {
        return remoteACE;
    }

    public void setRemoteACE(String remoteACE) {
        this.remoteACE = remoteACE;
    }

    public String getLocalACE() {
        return localACE;
    }

    public void setLocalACE(String localACE) {
        this.localACE = localACE;
    }

}
