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
 package org.nuxeo.ecm.sync.cmis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.sync.cmis.api.CMISRemoteService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.sync.cmis", "org.nuxeo.ecm.sync.cmis:OSGI-INF/cmis-repository-test-contribs.xml" })
// @Ignore
public class TestCMISImport {

    private static final Log log = LogFactory.getLog(TestCMISImport.class);

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService service;

    @Inject
    protected EventService eventService;

    @Inject
    protected EventServiceAdmin eventServiceAdmin;

    @Inject
    protected CMISRemoteService cmis;

    protected DocumentModel src;

    @Before
    public void initRepo() throws Exception {

        assertNotNull(cmis);

        session.removeChildren(session.getRootDocument().getRef());
        session.save();

        src = session.createDocumentModel("/", "sync", "Workspace");
        src.setPropertyValue("dc:title", "Sync");
        src = session.createDocument(src);
        session.save();
        src = session.getDocument(src.getRef());
    }

    @Test
    public void shouldCallWithParameters() throws OperationException {

        Assume.assumeTrue("No distant CMIS server can be reached", TestHelper.isTestCMISServerRunning(cmis));

        final String remote = "/default-domain/workspaces/Documents";

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("folderChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "Folder").set("name", "folder").set("properties", "dc:title=AFolder");
        chain.add(CMISSync.ID).set("connection", "remoteNuxeo").set("remoteRef", remote);
        chain.add(CMISImport.ID).set("state", "imported");
        DocumentModel doc = (DocumentModel) service.run(ctx, chain);
        session.save();
        assertEquals("remoteNuxeo", doc.getPropertyValue("cmissync:sync/connection"));

        DocumentModelList dml = session.getChildren(doc.getRef());
        assertEquals(4, dml.size());
        dml.forEach(dl -> log.debug(dl.getProperties("cmissync")));

        chain = new OperationChain("syncChain");
        for (DocumentModel dm : dml) {
            chain.add(CMISSync.ID).set("path", dm.getPathAsString());
        }
        service.run(ctx, chain);
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

        dml = session.getChildren(doc.getRef());
        assertEquals(4, dml.size());
        dml.forEach(dl -> log.debug(dl.getProperties("cmissync")));

    }
}
