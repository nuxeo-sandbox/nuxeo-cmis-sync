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
 *     Thibaud Arguillere
 */
package org.nuxeo.ecm.sync.cmis;

import java.io.Serializable;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.sync.cmis.api.CMISRemoteService;

import static org.nuxeo.ecm.sync.cmis.api.CMISServiceConstants.*;

public abstract class CMISOperations {

    private static final Log log = LogFactory.getLog(CMISOperations.class);

    public CMISOperations() {
        super();
    }

    protected DocumentModel loadDocument(CoreSession session, DocumentModel target, AtomicReference<String> remoteRef,
            AtomicBoolean idRef) {
        if (session == null) {
            throw new NullPointerException("session");
        }
        if (target == null) {
            throw new NullPointerException("document");
        }
        DocumentModel model = target;
        if (!model.hasFacet(SYNC_FACET)) {
            model.addFacet(SYNC_FACET);
        }

        if (remoteRef.get() == null) {
            remoteRef.set((String) model.getPropertyValue(XPATH_REMOTE_UID));
            idRef.set(true);
            if (remoteRef.get() == null) {
                throw new IllegalArgumentException("UID or path required for sync");
            }
        }
        return model;
    }

    protected String validateConnection(Property connectionProperty, String connection) {
        Property connect = connectionProperty;
        if (connect.getValue() != null && connection != null) {
            if (!connect.getValue().equals(connection)) {
                throw new IllegalArgumentException("Mis-matched repository connection");
            }
        } else if (connection != null) {
            connect.setValue(connection);
        } else {
            connection = (String) connect.getValue();
        }
        return connection;
    }

    protected Session createSession(String connection, Property repositoryProperty, CMISRemoteService cmis) {
        Session repo = cmis.createSession(connection);
        String repoId = repo.getRepositoryInfo().getId();
        if (repositoryProperty.getValue() == null) {
            repositoryProperty.setValue(repoId);
        } else if (!repositoryProperty.getValue().equals(repoId)) {
            throw new IllegalArgumentException(
                    "Mis-matched remote repository identifier: " + repositoryProperty.getValue() + " != " + repoId);
        }
        return repo;
    }

    protected CmisObject loadObject(Session repo, String remoteRef, boolean idRef) {
        if (!remoteRef.startsWith("/") && !idRef) {
            log.warn("Using ID reference for non-path like value: " + remoteRef);
            idRef = true;
        }

        CmisObject remote = null;
        try {
            if (idRef) {
                remote = repo.getObject(remoteRef);
            } else {
                remote = repo.getObjectByPath(remoteRef);
            }
        } catch (CmisObjectNotFoundException e) {
            // Nothing, remote stays null
        }

        if (remote == null) {
            throw new IllegalArgumentException("Remote reference not found");
        }

        return remote;
    }

    protected void checkObject(CmisObject remote, DocumentModel model) {
        // Set required identifying information
        if (model.getPropertyValue(XPATH_REMOTE_UID) == null) {
            model.setPropertyValue(XPATH_REMOTE_UID, remote.getId());
            model.setPropertyValue(XPATH_TYPE, remote.getBaseTypeId().value());

            if (remote instanceof FileableCmisObject) {
                FileableCmisObject pathy = (FileableCmisObject) remote;
                model.setPropertyValue(XPATH_PATHS, (Serializable) pathy.getPaths());
            }
        } else if (!model.getPropertyValue(XPATH_REMOTE_UID).equals(remote.getId())) {
            throw new IllegalArgumentException("Mis-matched remote document UUID: "
                    + model.getPropertyValue(XPATH_REMOTE_UID) + " != " + remote.getId());
        }
    }

    protected boolean requiresUpdate(CmisObject remote, DocumentModel doc, boolean force) {
        // Check Last Modified
        GregorianCalendar syncRef = (GregorianCalendar) doc.getPropertyValue(XPATH_SYNCHRONIZED);
        GregorianCalendar cal = remote.getLastModificationDate();

        return force || syncRef == null || cal.after(syncRef);
    }

    protected DocumentModel  updateSyncAttributes(CmisObject remote, DocumentModel doc, String state) {
        if (state != null) {
            doc.setPropertyValue(XPATH_STATE, state);
        }
        doc.setPropertyValue(XPATH_SYNCHRONIZED, new Date());
        doc.setPropertyValue(XPATH_MODIFIED, remote.getLastModificationDate().getTime());

        return doc;
    }

}
