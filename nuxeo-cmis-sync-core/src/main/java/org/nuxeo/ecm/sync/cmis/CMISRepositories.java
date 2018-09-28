package org.nuxeo.ecm.sync.cmis;

import java.io.IOException;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Instantiations;
import org.nuxeo.ecm.core.io.registry.reflect.Priorities;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.sync.cmis.api.CMISRemoteService;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Synchronize individual documents
 */
@Setup(mode = Instantiations.SINGLETON, priority = Priorities.REFERENCE)
public class CMISRepositories extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "cmisRepos";

    public CMISRepositories() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel doc) throws IOException {
        CMISRemoteService cmis = Framework.getService(CMISRemoteService.class);
        jg.writeFieldName(NAME);
        jg.writeObject(cmis.getRepositoryNames());
    }

}
