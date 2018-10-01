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

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.sync.cmis.api.CMISRemoteService;
import org.nuxeo.ecm.sync.cmis.api.CMISServiceConstants;
import org.nuxeo.runtime.api.Framework;

public class CMISSyncService extends CMISOperations {

    private static final Log log = LogFactory.getLog(CMISSyncService.class);

    protected CoreSession coreSession;

    protected CMISRemoteService cmis;

    protected String connectionName;

    protected String remoteRef;

    protected boolean isIdRef = false;

    protected boolean force = false;

    protected String state;

    protected boolean isContent = true;

    protected String contentXPath = "file:content";

    protected UserManager userManager = null;

    public CMISSyncService(CoreSession session, CMISRemoteService cmis) {
        super();
        coreSession = session;
        this.cmis = cmis;
    }

    public DocumentModel run(DocumentModel target) {

        // Get document, check facet
        AtomicReference<String> atomicRemoteRef = new AtomicReference<>(remoteRef);
        AtomicBoolean idRef = new AtomicBoolean(isIdRef);
        DocumentModel model = loadDocument(coreSession, target, atomicRemoteRef, idRef);
        DocumentRef docRef = model.getRef();

        // Validate repository
        Property connectionProperty = model.getProperty(CMISServiceConstants.XPATH_CONNECTION);
        connectionName = validateConnection(connectionProperty, connectionName);

        // Obtain Session from CMIS component
        Property repositoryProperty = model.getProperty(CMISServiceConstants.XPATH_REPOSITORY);
        Session repo = createSession(connectionName, repositoryProperty, cmis);

        // Retrieve object
        CmisObject remote = loadObject(repo, atomicRemoteRef.get(), idRef.get());
        checkObject(remote, model);

        // Update document
        if (requiresUpdate(remote, model, force)) {
            // Update fields
            List<CMISFieldMappingDescriptor> descs = cmis.getFieldMapping(connectionName, model.getDocumentType().getName());
            for (CMISFieldMappingDescriptor desc : descs) {
                Object val = remote.getPropertyValue(desc.getProperty());
                Property dp = model.getProperty(desc.getXpath());
                if (val != null) {
                    dp.setValue(val);
                } else {
                    dp.remove();
                }
            }

            if (isContent && remote instanceof Document) {
                try {
                    Document rdoc = (Document) remote;
                    ContentStream rstream = rdoc.getContentStream();
                    Blob blb = Blobs.createBlob(rstream.getStream());
                    blb.setFilename(rstream.getFileName());
                    blb.setMimeType(rstream.getMimeType());
                    DocumentHelper.addBlob(model.getProperty(contentXPath), blb);
                    model.setPropertyValue(CMISServiceConstants.XPATH_URI, rdoc.getContentUrl());
                } catch (IOException iox) {
                    log.warn("Unable to copy remote content", iox);
                }
            }

            // Update ACL
            // It's actually super hard to synchronize permissions because
            // of the inheritence in both sides, remote and local.
            // So, for the scope of this POC, we just
            // add the permissions if they are not already are set.
            // If a permission is not in hte mapping, it is still added?
            // IMPORTANT: If the user or the group does not exist in the system, the whole
            // thing fails, we must give up because we can't get the Principal and can't
            // that checks permission using ACL, Security policies etc. etc.
            // (just CoreSession#hasPermission)
            // This applies only for users, not groups, unfortunately
            List<Ace> remoteACEs = remote.getAcl() == null ? null : remote.getAcl().getAces();
            ACP localAcp = model.getACP();
            if (remoteACEs != null) {
                Map<String, String> aceMapping = cmis.getAceMappings(connectionName);

                // org.nuxeo.ecm.core.api.security.ACL [] acl = model.getACP().getACLs();
                for (Ace ace : remoteACEs) {
                    String principalId = ace.getPrincipalId();
                    Principal localPrincipal = getUserManager().getPrincipal(principalId);
                    boolean isGroup = false;

                    if (localPrincipal == null) {
                        isGroup = getUserManager().getGroup(principalId) != null;
                    }

                    if (localPrincipal == null && !isGroup) {
                        throw new NuxeoException("User/Group <" + principalId + "> not found");
                    }

                    for (String remotePerm : ace.getPermissions()) {
                        String localPerm = aceMapping.get(remotePerm);
                        if (localPerm == null) {
                            // No mapping, use the original as permission
                            // OR throw an error?
                            localPerm = remotePerm;
                        }

                        // Add permission if this user does not already have it
                        boolean needAddPermission = false;
                        if (localPrincipal != null) {
                            // This CoreSession#hasPermission checks all, including SecurityPolicies
                            // But actually this may be a problem. If hasPermission() returns false
                            // because of a security policy, we are still adding it (but it will
                            // be ignored because custom SecurityPolicy are called first.
                            // Complex problem, for sure :-)
                            if (!coreSession.hasPermission(localPrincipal, docRef, localPerm)) {
                                needAddPermission = true;
                            }
                        } else {
                            Access access = localAcp.getAccess(principalId, localPerm);
                            if(access == Access.UNKNOWN) { // or access != Access.GRANT...
                                needAddPermission = true;
                            }
                        }
                        if(needAddPermission) {
                            ACPImpl acp = new ACPImpl();
                            ACLImpl nuxeoAcl = new ACLImpl(CMISServiceConstants.SYNC_ACL);
                            acp.addACL(nuxeoAcl);
                            ACE nuxeoAce = new ACE(principalId, localPerm, true);
                            nuxeoAcl.add(nuxeoAce);
                            coreSession.setACP(docRef, acp, false);
                        }
                    }
                }
            }

        }

        // Set sync attributes
        updateSyncAttributes(remote, model, state);

        // Save and return
        model = coreSession.saveDocument(model);
        return model;
    }

    protected UserManager getUserManager() {
        if (userManager == null) {
            userManager = Framework.getService(UserManager.class);
        }

        return userManager;
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

    public boolean isContent() {
        return isContent;
    }

    public void setIsContent(boolean isContent) {
        this.isContent = isContent;
    }

    public String getContentXPath() {
        return contentXPath;
    }

    public void setContentXPath(String contentXPath) {
        this.contentXPath = contentXPath;
    }

}
