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
package org.nuxeo.ecm.sync.cmis.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.chemistry.opencmis.client.SessionParameterMap;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.sync.cmis.api.CMISRemoteService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class CMISRemoteServiceComponent extends DefaultComponent implements CMISRemoteService {

    private static final Log log = LogFactory.getLog(CMISRemoteServiceComponent.class);

    public static final String EP_REPO = "repository";

    public static final String EP_MAPPING = "mapping";

    protected Map<String, CMISMappingDescriptor> mappings = null;

    // Name of distant repo, ace-mapping for this repo
    protected Map<String, Map<String, String>> aceMapping = null;

    protected Map<String, CMISRepositoryDescriptor> repositories = null;

    public CMISRemoteServiceComponent() {
        super();
    }

    @Override
    public void activate(ComponentContext context) {
        mappings = new HashMap<>();
        aceMapping = new HashMap<>();
        repositories = new HashMap<>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        mappings = null;
        aceMapping = null;
        repositories = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (EP_MAPPING.equals(extensionPoint)) {
            CMISMappingDescriptor desc = (CMISMappingDescriptor) contribution;
            String name = desc.getName();
            mappings.put(name, desc);
        } else if (EP_REPO.equals(extensionPoint)) {
            CMISRepositoryDescriptor desc = (CMISRepositoryDescriptor) contribution;
            String name = desc.getName();

            log.debug("Registering repository: " + name);

            if (!desc.isEnabled()) {
                repositories.remove(name);
                log.info("Repository configured to not be enabled: " + name);
                return;
            }

            Map<String, String> loadedAceMapping = desc.getAceMapping();
            aceMapping.put(name, Collections.unmodifiableMap(loadedAceMapping));

            repositories.put(name, desc);
        }
    }

    @Override
    public List<CMISMappingDescriptor> getMappings(String doctype) {
        return mappings.values().stream().filter(m -> m.matches(doctype)).collect(Collectors.toList());
    }

    @Override
    public Map<String, String> getAceMappings(String repository) {
        return aceMapping.get(repository);
    }

    @Override
    public Collection<String> getRepositoryNames() {
        return Collections.unmodifiableSet(repositories.keySet());
    }

    @Override
    public Session createSession(String repository) {
        CMISRepositoryDescriptor desc = repositories.get(repository);
        if (desc == null) {
            throw new IllegalArgumentException("No such repository: " + repository);
        }

        SessionFactory sessionFactory = SessionFactoryImpl.newInstance();

        SessionParameterMap parameter = new SessionParameterMap(desc.getProperties());
        parameter.setBasicAuthentication(desc.getUsername(), desc.getCredentials());
        parameter.put(SessionParameter.REPOSITORY_ID, desc.getRepository());

        String binding = desc.getBinding().toUpperCase();
        BindingType bt = BindingType.valueOf(binding);
        switch (bt) {
        case ATOMPUB:
            parameter.setAtomPubBindingUrl(desc.getUrl());
            break;
        case BROWSER:
            parameter.setBrowserBindingUrl(desc.getUrl());
            break;
        case WEBSERVICES:
            parameter.setWebServicesBindingUrl(desc.getUrl());
            break;
        default:
            break;
        }

        Session session = sessionFactory.createSession(parameter);
        // We want to fetch ACLs all the time. See getDefaultContext() doc. it's not
        // Thread-safe, anbd must be set immediatly at creation time and before being used.
        OperationContext oc = session.getDefaultContext();
        oc.setIncludeAcls(true);
        session.setDefaultContext(oc);

        return session;
    }

    @Override
    public CMISRepositoryDescriptor getRepositoryDescriptor(String repository) {
        return repositories.get(repository);
    }

}
