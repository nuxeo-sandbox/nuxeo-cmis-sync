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
import org.nuxeo.ecm.sync.cmis.service.impl.CMISSyncService;

/**
 *
 */
@Operation(id = CMISSync.ID, category = Constants.CAT_DOCUMENT, label = "CMIS Document Synchronization", description = "Synchronize CMIS content with a remote repository.")
public class CMISSync {

    public static final String ID = "Document.CMISSync";

    static final Log log = LogFactory.getLog(CMISSync.class);

    @Context
    protected CoreSession coreSession;

    @Context
    protected CMISRemoteService cmis;

    // Connection is the name of the main <repository></repository> object in the configuration
    // If not passed, we read it in the schema
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

    @Param(name = "content", required = false, values = "true")
    protected boolean content = true;

    @Param(name = "contentXPath", required = false, values = "file:content")
    protected String contentXPath = "file:content";

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel target) {

        CMISSyncService cmisSync = new CMISSyncService(coreSession, cmis);

        cmisSync.setConnectionName(connection);
        cmisSync.setRemoteRef(remoteRef);
        cmisSync.setIsIdRef(idRef);
        cmisSync.setForce(force);
        cmisSync.setState(state);
        cmisSync.setIsContent(content);
        cmisSync.setContentXPath(contentXPath);

        DocumentModel result = cmisSync.run(target);
        return result;

    }

}
