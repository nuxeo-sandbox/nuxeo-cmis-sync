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

    public static final String EP_ACE_MAPPING = "acxe-mapping";

    protected Map<String, CMISMappingDescriptor> mappings = null;

    protected Map<String, String> aceMappings = null;

    protected Map<String, CMISRepositoryDescriptor> repositories = null;

    public CMISRemoteServiceComponent() {
        super();
    }

    @Override
    public void activate(ComponentContext context) {
        mappings = new HashMap<>();
        aceMappings = new HashMap<>();
        repositories = new HashMap<>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        mappings = null;
        aceMappings = null;
        repositories = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (EP_MAPPING.equals(extensionPoint)) {
            CMISMappingDescriptor desc = (CMISMappingDescriptor) contribution;
            String name = desc.getName();
            mappings.put(name, desc);
        } else if (EP_ACE_MAPPING.equals(extensionPoint)) {
            CMISAceMappingDescriptor desc = (CMISAceMappingDescriptor) contribution;
            aceMappings.put(desc.getRemoteACE(), desc.getLocalACE());
        } else if (EP_REPO.equals(extensionPoint)) {
            CMISRepositoryDescriptor desc = (CMISRepositoryDescriptor) contribution;
            String name = desc.getName();

            log.debug("Registering repository: " + name);

            if (!desc.isEnabled()) {
                repositories.remove(name);
                log.info("Repository configured to not be enabled: " + name);
                return;
            }

            repositories.put(name, desc);
        }
    }

    @Override
    public List<CMISMappingDescriptor> getMappings(String doctype) {
        return mappings.values().stream().filter(m -> m.matches(doctype)).collect(Collectors.toList());
    }

    @Override
    public Map<String, String> getAceMappings() {
        return Collections.unmodifiableMap(aceMappings);
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
