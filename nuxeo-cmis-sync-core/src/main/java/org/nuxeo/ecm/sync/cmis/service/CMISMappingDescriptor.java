package org.nuxeo.ecm.sync.cmis.service;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("mapping")
public class CMISMappingDescriptor implements Serializable {

  private static final long serialVersionUID = 1L;

  @XNode("@name")
  protected String name;

  @XNode("@xpath")
  protected String xpath;

  @XNode("@property")
  protected String property;

  @XNode("@doctype")
  protected String doctype;

  public CMISMappingDescriptor() {
    super();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getXpath() {
    return xpath;
  }

  public void setXpath(String xpath) {
    this.xpath = xpath;
  }

  public String getProperty() {
    return property;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  public String getDoctype() {
    return doctype;
  }

  public void setDoctype(String doctype) {
    this.doctype = doctype;
  }
  
  public boolean matches(String typeOfDoc) {
    return this.doctype == null || typeOfDoc == null || typeOfDoc.equals(this.doctype);
  }

}
