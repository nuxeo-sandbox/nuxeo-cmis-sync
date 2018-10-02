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
package org.nuxeo.ecm.sync.cmis.service.impl;

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
import org.apache.chemistry.opencmis.client.runtime.OperationContextImpl;
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

    public static final String EP_CONNECTION = "connection";

    // Name of connection, Map of mapping name/values for this connection
    protected Map<String, Map<String, CMISFieldMappingDescriptor>> fieldMapping = null;

    // Name of connection, ace-mapping for this connection
    protected Map<String, CMISAceMapping> aceMapping = null;

    protected Map<String, CMISConnectionDescriptor> connections = null;

    public CMISRemoteServiceComponent() {
        super();
    }

    @Override
    public void activate(ComponentContext context) {
        fieldMapping = new HashMap<>();
        aceMapping = new HashMap<>();
        connections = new HashMap<>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        fieldMapping = null;
        aceMapping = null;
        connections = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (EP_CONNECTION.equals(extensionPoint)) {
            CMISConnectionDescriptor desc = (CMISConnectionDescriptor) contribution;
            String name = desc.getName();

            log.debug("Registering connection: " + name + ", repository: " + desc.getRepository());

            if (!desc.isEnabled()) {
                connections.remove(name);
                log.info("Connection configured to not be enabled: " + name);
                return;
            }

            CMISAceMapping aceMappingMap = new CMISAceMapping(name, desc.getAceMappingMethod(), desc.getAceMapping());
            aceMapping.put(name, aceMappingMap);

            List<CMISFieldMappingDescriptor> loadedFieldMapping = desc.getFieldMapping();
            Map<String, CMISFieldMappingDescriptor> fieldMappingMap = new HashMap<>();
            loadedFieldMapping.forEach(oneDesc -> {
                fieldMappingMap.put(oneDesc.getName(), oneDesc);
            });
            fieldMapping.put(name, fieldMappingMap);

            connections.put(name, desc);
        }
    }

    @Override
    public List<CMISFieldMappingDescriptor> getFieldMapping(String connectionName, String doctype) {
        Map<String, CMISFieldMappingDescriptor> fieldMappingMap = fieldMapping.get(connectionName);
        return fieldMappingMap.values().stream().filter(m -> m.matches(doctype)).collect(Collectors.toList());
    }

    @Override
    public CMISAceMapping getAceMappings(String connectionName) {
        return aceMapping.get(connectionName);
    }

    @Override
    public Collection<String> getConnectionNames() {
        return Collections.unmodifiableSet(connections.keySet());
    }

    @Override
    public Session createSession(String connectionName) {
        CMISConnectionDescriptor desc = connections.get(connectionName);
        if (desc == null) {
            throw new IllegalArgumentException("No such connection: " + connectionName);
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
        // Thread-safe, and must be set immediately at creation time and before being used.
        OperationContext oc = new OperationContextImpl();
        oc.setIncludeAcls(true);
        oc.setLoadSecondaryTypeProperties(true);
        oc.setIncludePolicies(true);
        session.setDefaultContext(oc);

        return session;
    }

    @Override
    public CMISConnectionDescriptor getConnectionDescriptor(String connectionName) {
        return connections.get(connectionName);
    }

}
