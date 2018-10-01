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

import static org.nuxeo.ecm.sync.cmis.api.CMISServiceConstants.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.sync.cmis.api.CMISRemoteService;

public class CMISImportService extends CMISOperations {

    private static final Log log = LogFactory.getLog(CMISImportService.class);

    protected CoreSession coreSession;

    protected CMISRemoteService cmis;

    protected String connectionName;

    protected String remoteRef;

    protected boolean isIdRef = false;

    protected boolean force = false;

    protected String state;

    public CMISImportService(CoreSession coreSession, CMISRemoteService cmis) {
        super();
        this.coreSession = coreSession;
        this.cmis = cmis;
    }

    public DocumentModel run(DocumentModel target) {

        if (!target.isFolder()) {
            throw new IllegalArgumentException("Cannot synchronize non-folderish documents");
        }

        if (coreSession == null) {
            coreSession = target.getCoreSession();
            if (coreSession == null) {
                throw new NuxeoException("No CoreSession available");
            }
        }

        // Get document, check facet
        AtomicReference<String> atomicRemoteRef = new AtomicReference<>(remoteRef);
        AtomicBoolean idRef = new AtomicBoolean(isIdRef);
        DocumentModel model = loadDocument(coreSession, target, atomicRemoteRef, idRef);

        // Validate repository
        Property p = model.getProperty(XPATH_CONNECTION);
        connectionName = validateConnection(p, connectionName);

        // Obtain Session from CMIS component
        Property repositoryProperty = model.getProperty(XPATH_REPOSITORY);
        Session repo = createSession(connectionName, repositoryProperty, cmis);

        // Retrieve object
        CmisObject remote = loadObject(repo, atomicRemoteRef.get(), idRef.get());
        checkObject(remote, model);

        // Import children of current path
        if (remote instanceof Folder) {
            Folder folder = (Folder) remote;
            for (CmisObject obj : folder.getChildren()) {
                importObject(model, obj);
            }
        } else {
            log.warn("Remote object is not a folder: " + remote);
            throw new IllegalArgumentException("Cannot import non-folder documents");
        }

        // Save and return
        model = coreSession.saveDocument(model);
        return model;
    }

    private void importObject(DocumentModel model, CmisObject obj) {
        String docType = "Document";
        switch (obj.getBaseTypeId()) {
        case CMIS_DOCUMENT:
            docType = "File";
            break;
        case CMIS_FOLDER:
            docType = "Folder";
            break;
        case CMIS_ITEM:
            docType = "File";
            break;
        case CMIS_POLICY:
            docType = "Policy";
            break;
        case CMIS_RELATIONSHIP:
            docType = "Relationship";
            break;
        case CMIS_SECONDARY:
            docType = "Secondary";
            break;
        default:
            break;
        }

        try {
            DocumentModel child = coreSession.createDocumentModel(model.getPathAsString(), obj.getName(), docType);
            child.addFacet(SYNC_FACET);
            child.setPropertyValue("dc:title", obj.getName());
            child.setPropertyValue(XPATH_REMOTE_UID, obj.getId());
            child.setPropertyValue(XPATH_TYPE, obj.getBaseTypeId().value());
            if (obj instanceof FileableCmisObject) {
                child.getProperty(XPATH_PATHS).setValue(((FileableCmisObject) obj).getPaths());
            }

            child.setPropertyValue(XPATH_CONNECTION, connectionName);
            child.setPropertyValue(XPATH_REPOSITORY, model.getPropertyValue(XPATH_REPOSITORY));
            child.setPropertyValue(XPATH_STATE, state);

            child = coreSession.getOrCreateDocument(child);
        } catch (Exception ex) {
            log.error("Error creating document", ex);
            throw new RuntimeException(ex);
        }
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setConnectionName(String connectioName) {
        connectionName = connectioName;
    }

    public void setRemoteRef(String remoteRef) {
        this.remoteRef = remoteRef;
    }

    public void setIsIdRef(boolean isIdRef) {
        this.isIdRef = isIdRef;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

}
