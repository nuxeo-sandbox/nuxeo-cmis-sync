/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Damon Brown
 */
package org.nuxeo.ecm.sync.cmis.service.impl;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.sync.cmis.CMISImport;
import org.nuxeo.ecm.sync.cmis.api.CMISRemoteService;

public class CMISImportService extends CMISImport {

    public CMISImportService(CoreSession session, CMISRemoteService cmis) {
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

}
