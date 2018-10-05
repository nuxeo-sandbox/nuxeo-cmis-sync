/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thibaud Arguillere
 */
package org.nuxeo.ecm.sync.cmis.service.impl;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.sync.cmis.api.CMISServiceConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 10.2
 */
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
public class CMISAceMapping implements CMISServiceConstants {

    protected String connectionName;

    protected String method;

    protected Map<String, String> mapping;

    protected CoreSession coreSession;

    protected DocumentModel doc;

    protected CmisObject remote;

    protected UserManager userManager = null;

    public CMISAceMapping(String connectionName, String method, Map<String, String> mapping) {

        this.connectionName = connectionName;
        this.method = method;
        this.mapping = mapping;
    }

    protected UserManager getUserManager() {
        if (userManager == null) {
            userManager = Framework.getService(UserManager.class);
        }

        return userManager;
    }

    public DocumentModel applyMapping(CoreSession coreSession, DocumentModel doc, CmisObject remote) {

        this.coreSession = coreSession;
        this.doc = doc;
        this.remote = remote;

        switch (method) {
        case ACE_SYNC_METHOD_REPLACE:
            return applyWithReplaceAll();

        case ACE_SYNC_METHOD_ADD_IF_NOT_SET:
            return applyWithAddIfNotSet();

        default:
            throw new IllegalArgumentException("coucou");
        }
    }

    protected DocumentModel applyWithReplaceAll() {

        DocumentRef docRef = doc.getRef();

        // Remove all permissions, block inheritance
        ACP acp = doc.getACP();

        boolean permissionChanged = acp.blockInheritance(ACL.LOCAL_ACL, coreSession.getPrincipal().getName());
        if (permissionChanged) {
            doc.setACP(acp, true);
        }

        ACL[] acls = acp.getACLs();
        for (ACL acl : acls) {
            if (!ACL.INHERITED_ACL.equals(acl.getName())) {
                acp.removeACL(acl.getName());
            }
        }
        doc.setACP(acp, true);

        // Now, add the remote permissions
        List<Ace> remoteACEs = remote.getAcl() == null ? null : remote.getAcl().getAces();
        if (remoteACEs != null) {
            //CPImpl newAcp = new ACPImpl();
            ACLImpl nuxeoAcl = new ACLImpl(SYNC_ACL);
            acp.addACL(nuxeoAcl);//newAcp.addACL(nuxeoAcl);

            for (Ace ace : remoteACEs) {
                String principalId = ace.getPrincipalId();
                Principal localPrincipal = getUserManager().getPrincipal(principalId);
                boolean isGroup = false;
                boolean isEveryone = false;

                if (localPrincipal == null) {
                    isGroup = getUserManager().getGroup(principalId) != null;
                }

                if (localPrincipal == null && !isGroup) {
                    isEveryone = SecurityConstants.EVERYONE.equals(principalId);
                }

                if (localPrincipal == null && !isGroup && !isEveryone) {
                    throw new NuxeoException("User/Group <" + principalId + "> not found");
                }

                for (String remotePerm : ace.getPermissions()) {
                    String localPerm = mapping.get(remotePerm);
                    if (localPerm == null) {
                        // No mapping, use the original as permission
                        // OR throw an error?
                        localPerm = remotePerm;
                    }

                    ACE nuxeoAce = new ACE(principalId, localPerm, true);
                    nuxeoAcl.add(nuxeoAce);
                }
            }
            coreSession.setACP(docRef, acp, false);
        }


        return doc;
    }

    protected DocumentModel applyWithAddIfNotSet() {

        DocumentRef docRef = doc.getRef();

        List<Ace> remoteACEs = remote.getAcl() == null ? null : remote.getAcl().getAces();
        ACP localAcp = doc.getACP();
        if (remoteACEs != null) {
            // org.nuxeo.ecm.core.api.security.ACL [] acl = model.getACP().getACLs();
            for (Ace ace : remoteACEs) {
                String principalId = ace.getPrincipalId();
                Principal localPrincipal = getUserManager().getPrincipal(principalId);
                boolean isGroup = false;
                boolean isEveryone = false;

                if (localPrincipal == null) {
                    isGroup = getUserManager().getGroup(principalId) != null;
                }

                if (localPrincipal == null && !isGroup) {
                    isEveryone = SecurityConstants.EVERYONE.equals(principalId);
                }

                if (localPrincipal == null && !isGroup && !isEveryone) {
                    throw new NuxeoException("User/Group <" + principalId + "> not found");
                }

                for (String remotePerm : ace.getPermissions()) {
                    String localPerm = mapping.get(remotePerm);
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
                        if (access == Access.UNKNOWN) { // or access != Access.GRANT...
                            needAddPermission = true;
                        }
                    }
                    if (needAddPermission) {
                        ACPImpl acp = new ACPImpl();
                        ACLImpl nuxeoAcl = new ACLImpl(SYNC_ACL);
                        acp.addACL(nuxeoAcl);
                        ACE nuxeoAce = new ACE(principalId, localPerm, true);
                        nuxeoAcl.add(nuxeoAce);
                        coreSession.setACP(docRef, acp, false);
                    }
                }
            }
        }

        return doc;
    }

    public String getMethod() {
        return method;
    }

}
