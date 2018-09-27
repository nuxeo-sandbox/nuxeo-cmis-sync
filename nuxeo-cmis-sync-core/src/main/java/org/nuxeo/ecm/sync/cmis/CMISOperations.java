package org.nuxeo.ecm.sync.cmis;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.sync.cmis.api.CMISRemoteService;

public abstract class CMISOperations {

  public static final String REMOTE_UID = "cmissync:uid";

  public static final String SYNC_DATA = "cmissync:sync";

  private static final Log log = LogFactory.getLog(CMISOperations.class);

  public CMISOperations() {
    super();
  }

  protected DocumentModel loadDocument(CoreSession session, DocumentModel target, AtomicReference<String> remoteRef,
      AtomicBoolean idRef) {
    if (session == null) {
      throw new NullPointerException("session");
    }
    if (target == null) {
      throw new NullPointerException("document");
    }
    DocumentModel model = target;
    if (!model.hasFacet("cmissync")) {
      model.addFacet("cmissync");
    }

    if (remoteRef.get() == null) {
      remoteRef.set((String) model.getPropertyValue(REMOTE_UID));
      idRef.set(true);
      if (remoteRef.get() == null) {
        throw new IllegalArgumentException("UID or path required for sync");
      }
    }
    return model;
  }

  protected String validateConnection(Property p, String connection) {
    Property connect = p.get("connection");
    if (connect.getValue() != null && connection != null) {
      if (!connect.getValue().equals(connection)) {
        throw new IllegalArgumentException("Mis-matched repository connection");
      }
    } else if (connection != null) {
      connect.setValue(connection);
    } else {
      connection = (String) connect.getValue();
    }
    return connection;
  }

  protected Session createSession(Property p, CMISRemoteService cmis) {
    Session repo = cmis.createSession((String) p.getValue("connection"));
    String repoId = repo.getRepositoryInfo().getId();
    Property syncRepo = p.get("repository");
    if (syncRepo.getValue() == null) {
      syncRepo.setValue(repoId);
    } else if (!syncRepo.getValue().equals(repoId)) {
      throw new IllegalArgumentException(
          "Mis-matched remote repository identifier: " + syncRepo.getValue() + " != " + repoId);
    }
    return repo;
  }

  protected CmisObject loadObject(Session repo, String remoteRef, boolean idRef) {
    if (!remoteRef.startsWith("/") && !idRef) {
      log.warn("Using ID reference for non-path like value: " + remoteRef);
      idRef = true;
    }
    CmisObject remote = null;
    if (idRef) {
      remote = repo.getObject(remoteRef);
    } else {
      remote = repo.getObjectByPath(remoteRef);
    }

    // Should be caught by getObject method, double-check
    if (remote == null) {
      throw new IllegalArgumentException("Remote reference not found");
    }
    return remote;
  }

  protected void checkObject(CmisObject remote, DocumentModel model, Property p) {
    // Set required identifying information
    if (model.getPropertyValue(REMOTE_UID) == null) {
      model.setPropertyValue(REMOTE_UID, remote.getId());
      p.get("type").setValue(remote.getBaseTypeId().value());
      if (remote instanceof FileableCmisObject) {
        FileableCmisObject pathy = (FileableCmisObject) remote;
        p.get("paths").setValue(pathy.getPaths());
      }
    } else if (!model.getPropertyValue(REMOTE_UID).equals(remote.getId())) {
      throw new IllegalArgumentException(
          "Mis-matched remote document UUID: " + model.getPropertyValue(REMOTE_UID) + " != " + remote.getId());
    }
  }

  protected boolean requiresUpdate(CmisObject remote, Property p, boolean force) {
    // Check Last Modified
    Property syncTime = p.get("synchronized");
    GregorianCalendar syncRef = (GregorianCalendar) syncTime.getValue();
    GregorianCalendar cal = remote.getLastModificationDate();

    return force || syncRef == null || cal.after(syncRef);
  }

  protected void updateSyncAttributes(CmisObject remote, Property p, String state) {
    if (state != null) {
      p.get("state").setValue(state);
    }
    p.get("synchronized").setValue(new Date());
    p.get("modified").setValue(remote.getLastModificationDate().getTime());

  }

}
