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
import org.nuxeo.ecm.sync.cmis.service.impl.CMISImportService;
import org.nuxeo.runtime.api.Framework;

public class FolderishListener implements EventListener, PostCommitEventListener {

    static final Log log = LogFactory.getLog(FolderishListener.class);

    protected AutomationService service;

    protected CMISRemoteService cmis;

    public FolderishListener() {
        super();
    }

    private void checkServices() {
        if (service == null) {
            service = Framework.getService(AutomationService.class);
            cmis = Framework.getService(CMISRemoteService.class);
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
        return model != null && model.hasFacet("cmissync") && model.hasFacet("Folderish")
                && model.getPropertyValue("cmissync:uid") != null
                && model.getPropertyValue("cmissync:sync/state") != null
                && model.getPropertyValue("cmissync:sync/state").equals("sync");
    }

    private void execute(DocumentEventContext context) {
        DocumentModel model = context.getSourceDocument();
        model.setPropertyValue("cmissync:sync/state", "traversed");

        CMISImportService imp = new CMISImportService(context.getCoreSession(), cmis);
        imp.setState("queued");
        imp.run(model);
    }

}
