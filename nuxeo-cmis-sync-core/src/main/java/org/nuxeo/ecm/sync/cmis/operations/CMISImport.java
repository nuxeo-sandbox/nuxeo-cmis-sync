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
package org.nuxeo.ecm.sync.cmis.operations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.sync.cmis.api.CMISRemoteService;
import org.nuxeo.ecm.sync.cmis.service.impl.CMISImportService;

/**
 * Synchronize individual documents
 */
@Operation(id = CMISImport.ID, category = Constants.CAT_FETCH, label = "CMIS Structure Import", description = "Import CMIS content with a remote repository.")
public class CMISImport {

    public static final String ID = "Repository.CMISImport";

    private static final Log log = LogFactory.getLog(CMISImport.class);

    @Context
    protected CoreSession session;

    @Context
    protected CMISRemoteService cmis;

    @Param(name = "connection", required = false)
    protected String connection;

    @Param(name = "remoteRef", required = false)
    protected String remoteRef;

    @Param(name = "idRef", required = false, values = "false")
    protected boolean idRef = false;

    @Param(name = "force", required = false, values = "false")
    protected boolean force = false;

    @Param(name = "state", required = false)
    protected String state;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel target) {

        CMISImportService cmisImport = new CMISImportService(session, cmis);

        cmisImport.setConnectionName(connection);
        cmisImport.setRemoteRef(remoteRef);
        cmisImport.setIsIdRef(idRef);
        cmisImport.setForce(force);
        cmisImport.setState(state);

        DocumentModel result = cmisImport.run(target);

        return result;

        /*
        // Get document, check facet
        AtomicReference<String> remoteRef = new AtomicReference<>(this.remoteRef);
        AtomicBoolean idRef = new AtomicBoolean(this.idRef);
        DocumentModel model = loadDocument(session, target, remoteRef, idRef);
        if (!model.isFolder()) {
            throw new IllegalArgumentException("Cannot synchronize non-folderish documents");
        }

        // Validate repository
        Property p = model.getProperty(XPATH_CONNECTION);
        connection = validateConnection(p, connection);

        // Obtain Session from CMIS component
        Property repositoryProperty = model.getProperty(XPATH_REPOSITORY);
        Session repo = createSession(connection, repositoryProperty, cmis);

        // Retrieve object
        CmisObject remote = loadObject(repo, remoteRef.get(), idRef.get());
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
        model = session.saveDocument(model);
        return model;
        */
    }

}
