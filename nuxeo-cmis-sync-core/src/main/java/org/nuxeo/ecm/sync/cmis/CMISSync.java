package org.nuxeo.ecm.sync.cmis;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.sync.cmis.api.CMISRemoteService;
import org.nuxeo.ecm.sync.cmis.service.CMISMappingDescriptor;

/**
 *
 */
@Operation(id = CMISSync.ID, category = Constants.CAT_DOCUMENT, label = "CMIS Document Synchronization", description = "Synchronize CMIS content with a remote repository.")
public class CMISSync extends CMISOperations {

  public static final String ID = "Document.CMISSync";

  private static final Log log = LogFactory.getLog(CMISSync.class);

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

  @Param(name = "content", required = false, values = "true")
  protected boolean content = true;

  @Param(name = "contentXPath", required = false, values = "file:content")
  protected String contentXPath = "file:content";

  @OperationMethod
  public DocumentModel run() {

    // Get document, check facet
    AtomicReference<String> remoteRef = new AtomicReference<>(this.remoteRef);
    AtomicBoolean idRef = new AtomicBoolean(this.idRef);
    DocumentModel model = loadDocument(this.session, this.localPath, remoteRef, idRef);

    // Validate repository
    Property p = model.getProperty(SYNC_DATA);
    this.connection = validateConnection(p, this.connection);

    // Obtain Session from CMIS component
    Session repo = createSession(p, this.cmis);

    // Retrieve object
    CmisObject remote = loadObject(repo, remoteRef.get(), idRef.get());
    checkObject(remote, model, p);

    // Update document
    if (requiresUpdate(remote, p, this.force)) {
      List<CMISMappingDescriptor> descs = this.cmis.getMappings(model.getDocumentType().getName());
      for (CMISMappingDescriptor desc : descs) {
        Object val = remote.getPropertyValue(desc.getProperty());
        Property dp = model.getProperty(desc.getXpath());
        if (val != null) {
          dp.setValue(val);
        } else {
          dp.remove();
        }
      }

      if (this.content && remote instanceof Document) {
        try {
          Document rdoc = (Document) remote;
          ContentStream rstream = rdoc.getContentStream();
          Blob blb = Blobs.createBlob(rstream.getStream());
          blb.setFilename(rstream.getFileName());
          blb.setMimeType(rstream.getMimeType());
          DocumentHelper.addBlob(model.getProperty(this.contentXPath), blb);
          model.setPropertyValue(SYNC_DATA + "/uri", rdoc.getContentUrl());
        } catch (IOException iox) {
          log.warn("Unable to copy remote content", iox);
        }
      }
    }

    // Set sync attributes
    updateSyncAttributes(remote, p, this.state);

    // Save and return
    model = session.saveDocument(model);
    return model;
  }
}
