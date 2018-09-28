package org.nuxeo.ecm.sync.cmis.listeners;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.sync.cmis.api.CMISRemoteService;
import org.nuxeo.ecm.sync.cmis.service.impl.CMISSyncService;
import org.nuxeo.runtime.api.Framework;

public class DocumentListener implements EventListener, PostCommitEventListener {

  static final Log log = LogFactory.getLog(DocumentListener.class);

  protected AutomationService service;

  protected CMISRemoteService cmis;

  public DocumentListener() {
    super();
  }

  private void checkServices() {
    if (this.service == null) {
      this.service = Framework.getService(AutomationService.class);
      this.cmis = Framework.getService(CMISRemoteService.class);
    }
  }

  @Override
  public void handleEvent(EventBundle events) {
    checkServices();
    for (Event evt : events) {
      if (evt.getContext() instanceof DocumentEventContext) {
        DocumentEventContext context = (DocumentEventContext) evt.getContext();
        if (filterDoc(context.getSourceDocument())) {
          execute(context);
        }
      }
    }
  }

  @Override
  public void handleEvent(Event evt) {
    checkServices();
    if (evt.getContext() instanceof DocumentEventContext) {
      DocumentEventContext context = (DocumentEventContext) evt.getContext();
      if (filterDoc(context.getSourceDocument())) {
        execute(context);
      }
    }
  }

  private boolean filterDoc(DocumentModel model) {
    return model != null && model.hasFacet("cmissync") && model.getPropertyValue("cmissync:uid") != null
        && model.getPropertyValue("cmissync:sync/state") != null
        && model.getPropertyValue("cmissync:sync/state").equals("queued");
  }

  private void execute(DocumentEventContext context) {
    CMISSyncService sync = new CMISSyncService(context.getCoreSession(), this.cmis);
    sync.setState("sync");
    sync.run(context.getSourceDocument());
  }

}
