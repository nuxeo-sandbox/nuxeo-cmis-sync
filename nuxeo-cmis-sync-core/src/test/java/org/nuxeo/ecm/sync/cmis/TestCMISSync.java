package org.nuxeo.ecm.sync.cmis;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Ignore;
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
@Ignore
public class TestCMISSync {

  private static final Log log = LogFactory.getLog(TestCMISSync.class);

  @Inject
  protected CoreSession session;

  @Inject
  protected AutomationService service;

  protected DocumentModel src;

  @Before
  public void initRepo() throws Exception {
    session.removeChildren(session.getRootDocument().getRef());
    session.save();

    src = session.createDocumentModel("/", "src", "Workspace");
    src.setPropertyValue("dc:title", "Source");
    src = session.createDocument(src);
    session.save();
    src = session.getDocument(src.getRef());
  }

  @Test
  public void shouldCallWithParameters() throws OperationException {
    final String path = "/src/file";
    final String remote = "/default-domain/workspaces/Documents/orbeon-demo.pptx";

    OperationContext ctx = new OperationContext(session);
    ctx.setInput(src);

    OperationChain chain = new OperationChain("testChain");
    chain.add(FetchContextDocument.ID);
    chain.add(CreateDocument.ID).set("type", "File").set("name", "file").set("properties", "dc:title=MyDoc");
    chain.add(CMISSync.ID).set("connection", "test").set("remoteRef", remote);

    DocumentModel doc = (DocumentModel) service.run(ctx, chain);
    assertEquals(path, doc.getPathAsString());
    assertEquals("test", doc.getPropertyValue("cmissync:sync/connection"));
    assertEquals(526154, ((Blob) doc.getProperties("file").get("content")).getLength());
    log.debug(doc.getProperties("cmissync"));
    log.debug(doc.getProperties("dublincore"));
  }
}
