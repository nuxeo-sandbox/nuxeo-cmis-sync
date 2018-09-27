package org.nuxeo.ecm.sync.cmis.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.chemistry.opencmis.client.SessionParameterMap;
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

  protected Map<String, CMISRepositoryDescriptor> repositories = null;

  public CMISRemoteServiceComponent() {
    super();
  }

  @Override
  public void activate(ComponentContext context) {
    this.mappings = new HashMap<>();
    this.repositories = new HashMap<>();
  }

  @Override
  public void deactivate(ComponentContext context) {
    this.mappings = null;
    this.repositories = null;
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

      repositories.put(name, desc);
    }
  }

  @Override
  public List<CMISMappingDescriptor> getMappings(String doctype) {
    return this.mappings.values().stream().filter(m -> m.matches(doctype)).collect(Collectors.toList());
  }

  @Override
  public Collection<String> getRepositoryNames() {
    return Collections.unmodifiableSet(this.repositories.keySet());
  }

  @Override
  public Session createSession(String repository) {
    CMISRepositoryDescriptor desc = this.repositories.get(repository);
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

    return sessionFactory.createSession(parameter);
  }

}
