package org.nuxeo.ecm.sync.cmis;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.sync.cmis.api.CMISRemoteService;

/**
 * Synchronize individual documents
 */
@Operation(id = CMISImport.ID, category = Constants.CAT_FETCH, label = "CMIS Structure Import", description = "Import CMIS content with a remote repository.")
public class CMISImport extends CMISOperations {

  public static final String ID = "Repository.CMISImport";

  private static final Log log = LogFactory.getLog(CMISImport.class);

  @Context
  protected CoreSession session;

  @Context
  protected CMISRemoteService cmis;

  @Param(name = "connection", required = false)
  protected String connection;

  @Param(name = "path", required = true)
  protected String localPath;

  @Param(name = "remoteRef", required = false)
  protected String remoteRef;

  @Param(name = "idRef", required = false, values = "false")
  protected boolean idRef = false;

  @Param(name = "force", required = false, values = "false")
  protected boolean force = false;

  @Param(name = "state", required = false)
  protected String state;

  @OperationMethod
  public DocumentModel run() {

    // Get document, check facet
    AtomicReference<String> remoteRef = new AtomicReference<>(this.remoteRef);
    AtomicBoolean idRef = new AtomicBoolean(this.idRef);
    DocumentModel model = loadDocument(this.session, this.localPath, remoteRef, idRef);
    if (!model.isFolder()) {
      throw new IllegalArgumentException("Cannot synchronize non-folderish documents");
    }

    // Validate repository
    Property p = model.getProperty(SYNC_DATA);
    this.connection = validateConnection(p, this.connection);

    // Obtain Session from CMIS component
    Session repo = createSession(p, this.cmis);

    // Retrieve object
    CmisObject remote = loadObject(repo, remoteRef.get(), idRef.get());
    checkObject(remote, model, p);

    // Import children of current path
    if (remote instanceof Folder) {
      Folder folder = (Folder) remote;
      for (CmisObject obj : folder.getChildren()) {
        importObject(model, p, obj);
      }
    } else {
      log.warn("Remote object is not a folder: " + remote);
      throw new IllegalArgumentException("Cannot import non-folder documents");
    }

    // Save and return
    model = session.saveDocument(model);
    return model;
  }

  private void importObject(DocumentModel model, Property p, CmisObject obj) {
    String docType = "Document";
    switch (obj.getBaseTypeId()) {
    case CMIS_DOCUMENT:
      docType = "File";
      break;
    case CMIS_FOLDER:
      docType = "Folder";
      break;
    case CMIS_ITEM:
      docType = "File";
      break;
    case CMIS_POLICY:
      docType = "Policy";
      break;
    case CMIS_RELATIONSHIP:
      docType = "Relationship";
      break;
    case CMIS_SECONDARY:
      docType = "Secondary";
      break;
    default:
      break;
    }

    try {
      DocumentModel child = this.session.createDocumentModel(model.getPathAsString(), obj.getName(), docType);
      child.addFacet("cmissync");
      child.setPropertyValue("dc:title", obj.getName());
      child.setPropertyValue(REMOTE_UID, obj.getId());
      child.setPropertyValue(SYNC_DATA + "/type", obj.getBaseTypeId().value());
      if (obj instanceof FileableCmisObject) {
        child.getProperty(SYNC_DATA + "/paths").setValue(((FileableCmisObject) obj).getPaths());
      }

      child.setPropertyValue(SYNC_DATA + "/connection", this.connection);
      child.setPropertyValue(SYNC_DATA + "/repository", p.getValue("repository"));
      child.setPropertyValue(SYNC_DATA + "/state", this.state);

      child = session.createDocument(child);
    } catch (Exception ex) {
      log.error("Error creating document", ex);
      throw new RuntimeException(ex);
    }
  }
}
