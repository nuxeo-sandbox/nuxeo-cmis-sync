package org.nuxeo.ecm.sync.cmis.service.impl;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.sync.cmis.CMISSync;
import org.nuxeo.ecm.sync.cmis.api.CMISRemoteService;

public class CMISSyncService extends CMISSync {

    public CMISSyncService(CoreSession session, CMISRemoteService cmis) {
        super();
        this.session = session;
        this.cmis = cmis;
    }

    public CoreSession getSession() {
        return session;
    }

    public void setSession(CoreSession session) {
        this.session = session;
    }

    public CMISRemoteService getCmis() {
        return cmis;
    }

    public void setCmis(CMISRemoteService cmis) {
        this.cmis = cmis;
    }

    public String getConnection() {
        return connection;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }

    public String getRemoteRef() {
        return remoteRef;
    }

    public void setRemoteRef(String remoteRef) {
        this.remoteRef = remoteRef;
    }

    public boolean isIdRef() {
        return idRef;
    }

    public void setIdRef(boolean idRef) {
        this.idRef = idRef;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isContent() {
        return content;
    }

    public void setContent(boolean content) {
        this.content = content;
    }

    public String getContentXPath() {
        return contentXPath;
    }

    public void setContentXPath(String contentXPath) {
        this.contentXPath = contentXPath;
    }

}
