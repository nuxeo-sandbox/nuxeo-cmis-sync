package org.nuxeo.ecm.sync.cmis;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.sync.cmis", "org.nuxeo.ecm.sync.cmis:OSGI-INF/cmis-repository-test-contribs.xml" })
public class TestCMISImport {

  private static final Log log = LogFactory.getLog(TestCMISImport.class);

  @Inject
  protected CoreSession session;

  @Inject
  protected AutomationService service;

  protected DocumentModel src;

  @Before
  public void initRepo() throws Exception {
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
    final String remote = "/default-domain/workspaces/Documents";

    OperationContext ctx = new OperationContext(session);
    ctx.setInput(src);

    OperationChain chain = new OperationChain("folderChain");
    chain.add(FetchContextDocument.ID);
    chain.add(CreateDocument.ID).set("type", "Folder").set("name", "folder").set("properties", "dc:title=AFolder");
    chain.add(CMISSync.ID).set("connection", "test").set("remoteRef", remote);
    chain.add(CMISImport.ID).set("state", "imported");
    DocumentModel doc = (DocumentModel) service.run(ctx, chain);
    session.save();
    assertEquals("test", doc.getPropertyValue("cmissync:sync/connection"));

    DocumentModelList dml = session.getChildren(doc.getRef());
    assertEquals(4, dml.size());
    dml.forEach(dl -> log.debug(dl.getProperties("cmissync")));

    chain = new OperationChain("syncChain");
    for (DocumentModel dm : dml) {
      chain.add(CMISSync.ID).set("path", dm.getPathAsString());
    }
    service.run(ctx, chain);
    session.save();

    dml = session.getChildren(doc.getRef());
    assertEquals(4, dml.size());
    dml.forEach(dl -> log.debug(dl.getProperties("cmissync")));

  }
}
