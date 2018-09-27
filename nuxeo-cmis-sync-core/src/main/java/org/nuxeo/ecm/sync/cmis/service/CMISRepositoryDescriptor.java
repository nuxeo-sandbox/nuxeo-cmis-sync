package org.nuxeo.ecm.sync.cmis.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("repository")
public class CMISRepositoryDescriptor implements Serializable {

  private static final long serialVersionUID = 1L;

  @XNode("@name")
  protected String name;

  @XNode("@enabled")
  protected boolean enabled = true;

  @XNode("repository")
  protected String repository;

  @XNode("url")
  protected String url = "";

  @XNode("@binding")
  protected String binding;

  @XNode("username")
  protected String username = "";

  @XNode("credentials")
  protected String credentials = "";

  @XNodeMap(value = "property", key = "@key", type = HashMap.class, componentType = String.class)
  protected Map<String, String> properties = new HashMap<>();

  public CMISRepositoryDescriptor() {
    super();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getRepository() {
    return repository;
  }

  public void setRepository(String repository) {
    this.repository = repository;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getBinding() {
    return binding;
  }

  public void setBinding(String binding) {
    this.binding = binding;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getCredentials() {
    return credentials;
  }

  public void setCredentials(String credentials) {
    this.credentials = credentials;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

}