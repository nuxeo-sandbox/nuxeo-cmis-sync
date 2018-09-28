package org.nuxeo.ecm.sync.cmis.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.Session;
import org.nuxeo.ecm.sync.cmis.service.CMISMappingDescriptor;
import org.nuxeo.ecm.sync.cmis.service.CMISRepositoryDescriptor;

public interface CMISRemoteService {

    Session createSession(String name);

    List<CMISMappingDescriptor> getMappings(String doctype);

    /**
     *
     * @return a map of elements whose key is the remote ACE, value is the local ACE to apply
     * @since 10.2
     */
    Map<String, String> getAceMappings();

    Collection<String> getRepositoryNames();

    CMISRepositoryDescriptor getRepositoryDescriptor(String repository);

}
