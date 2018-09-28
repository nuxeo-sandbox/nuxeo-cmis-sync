package org.nuxeo.ecm.sync.cmis.api;

import java.util.Collection;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.Session;
import org.nuxeo.ecm.sync.cmis.service.CMISMappingDescriptor;
import org.nuxeo.ecm.sync.cmis.service.CMISRepositoryDescriptor;

public interface CMISRemoteService {

  Session createSession(String name);

  List<CMISMappingDescriptor> getMappings(String doctype);

  Collection<String> getRepositoryNames();

  CMISRepositoryDescriptor getRepositoryDescriptor(String repository);

}
