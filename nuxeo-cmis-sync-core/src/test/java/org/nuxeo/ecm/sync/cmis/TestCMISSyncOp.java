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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.sync.cmis.api.CMISRemoteService;
import org.nuxeo.ecm.sync.cmis.api.CMISServiceConstants;
import org.nuxeo.ecm.sync.cmis.operations.CMISSync;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.sync.cmis", "org.nuxeo.ecm.sync.cmis:OSGI-INF/cmis-repository-test-contribs.xml" })
public class TestCMISSyncOp {

    static final Log log = LogFactory.getLog(TestCMISSyncOp.class);

    // WARNING: This user and group must exist in the distant repo
    public static final String TEST_USER = "john";

    public static final String TEST_GROUP = "Finance";

    // WARNING: This document must exist in the distant repo and
    // its permissions have:
    // members can ReadWrite
    // Finance can readWrite
    // john can Everything
    public static final String REMOTE_DOC_PATH = "/default-domain/workspaces/Documents/orbeon-demo.pptx";

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService service;

    @Inject
    protected EventService eventService;

    @Inject
    protected EventServiceAdmin eventServiceAdmin;

    @Inject
    protected UserManager userManager;

    @Inject
    protected CMISRemoteService cmis;

    protected DocumentModel src;

    @Before
    public void initRepo() throws Exception {

        assertNotNull(cmis);

        session.removeChildren(session.getRootDocument().getRef());
        session.save();

        src = session.createDocumentModel("/", "src", "Workspace");
        src.setPropertyValue("dc:title", "Source");
        src = session.createDocument(src);
        session.save();
        src = session.getDocument(src.getRef());

        createGroup(TEST_GROUP);
        createUser(TEST_USER);
        NuxeoPrincipal principal = userManager.getPrincipal(TEST_USER);
        principal.setGroups(Arrays.asList("members", TEST_GROUP));
        userManager.updateUser(principal.getModel());

    }

    @Test
    public void shouldCallWithParameters() throws OperationException {

        Assume.assumeTrue("No distant CMIS server can be reached", TestHelper.isTestCMISServerRunning(cmis, TestHelper.CONNECTION_NUXEO_ADD_PERMS));

        final String path = "/src/file";
        final String remote = REMOTE_DOC_PATH;

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "File").set("name", "file").set("properties", "dc:title=MyDoc");
        chain.add(CMISSync.ID).set("connection", TestHelper.CONNECTION_NUXEO_ADD_PERMS).set("remoteRef", remote);

        DocumentModel doc = (DocumentModel) service.run(ctx, chain);
        session.save();

        eventService.waitForAsyncCompletion();
        while (eventServiceAdmin.getEventsInQueueCount() > 0) {
            eventService.waitForAsyncCompletion();
            Thread.yield();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        doc.refresh();

        assertEquals(path, doc.getPathAsString());
        assertEquals("remoteNuxeo", doc.getPropertyValue(CMISServiceConstants.XPATH_CONNECTION));
        assertEquals(526154, ((Blob) doc.getProperties("file").get("content")).getLength());

        // In distant Nuxeo test repo, members have ReadWrite on this document
        // It should have created a local specific CmisSync ACL
        ACL acl = doc.getACP().getACL(CMISServiceConstants.SYNC_ACL);
        assertNotNull(acl);
        boolean membersCanReadWrite = false;
        boolean financeCanReadWrite = false;
        boolean johnCanEverything = false;
        for (ACE ace : acl.getACEs()) {
            if ("members".equals(ace.getUsername()) && "ReadWrite".equals(ace.getPermission())) {
                membersCanReadWrite = true;
            } else if (TEST_GROUP.equals(ace.getUsername()) && "ReadWrite".equals(ace.getPermission())) {
                financeCanReadWrite = true;
            } else if (TEST_USER.equals(ace.getUsername()) && "Everything".equals(ace.getPermission())) {
                johnCanEverything = true;
            }
        }
        assertTrue(membersCanReadWrite);
        assertTrue(financeCanReadWrite);
        assertTrue(johnCanEverything);

    }

    protected void createUser(String userId) {
        DocumentModel userModel = userManager.getBareUserModel();
        userModel.setProperty("user", "username", userId);
        userModel.setProperty("user", "password", userId);
        userManager.createUser(userModel);
    }

    protected void createGroup(String groupId) {
        DocumentModel groupModel = userManager.getBareGroupModel();
        groupModel.setProperty("group", "groupname", groupId);
        userManager.createGroup(groupModel);
    }
}
