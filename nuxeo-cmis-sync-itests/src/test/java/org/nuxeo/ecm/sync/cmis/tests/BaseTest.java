package org.nuxeo.ecm.sync.cmis.tests;

import static org.junit.Assert.assertNotNull;
import static org.nuxeo.client.Operations.BLOB_ATTACH_ON_DOCUMENT;
import static org.nuxeo.client.Operations.ES_WAIT_FOR_INDEXING;

import java.io.File;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.client.NuxeoClient;
import org.nuxeo.client.objects.Document;
import org.nuxeo.client.objects.blob.FileBlob;
import org.nuxeo.client.objects.user.Group;
import org.nuxeo.client.objects.user.User;
import org.nuxeo.client.objects.user.UserManager;
import org.nuxeo.client.spi.NuxeoClientRemoteException;
import org.nuxeo.client.spi.auth.BasicAuthInterceptor;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

import okhttp3.Interceptor;

public abstract class BaseTest {

    // ------------------------This is config, should be in another class, but, well.
    public static final String CONNECTION_NUXEO_ADD_PERMS = "remoteNuxeo";

    public static final String CONNECTION_NUXEO_REPLACE_PERMS = "remoteNuxeoReplacePermissions";
    // ------------------------

    public static final String BASE_URL = "http://localhost:8080/nuxeo";

    public static final String LOGIN = "Administrator";

    public static final String PASSWORD = "Administrator";

    static final String REST_API_URL = "http://localhost:8080/nuxeo";

    public static final String GROUP_MEMBERS = "members";

    public static final String TEST_FILE_PATH = "/folder_2/file1";

    public static final String TEST_FILE_TITLE = "Test File 1";

    public static final String TEST_FILE_BLOB_NAME = "blob.json";

    public static final int TEST_FILE_BLOB_SIZE = 1012;

    public static final String DEFAULT_PASSWORD = "123";

    public static final String USER1 = "john";

    public static final String GROUP1 = "Finance";

    public static final String CUSTOM_PERM_NOT_MAPPED = "CanDoThis";

    public static final int COUNT_TEST_FOLDERS = 2;

    protected final NuxeoClient nuxeoClient = createClient().schemas("*");

    public BaseTest() {
        super();
    }

    /**
     * @return A {@link NuxeoClient} filled with Nuxeo Server URL and default basic authentication.
     */
    public static NuxeoClient createClient() {
        return createClient(LOGIN, PASSWORD);
    }

    /**
     * @return A {@link NuxeoClient} filled with Nuxeo Server URL and input basic authentication.
     */
    public static NuxeoClient createClient(String login, String password) {
        return createClientBuilder(login, password).connect();
    }

    /**
     * @return A {@link NuxeoClient.Builder} filled with Nuxeo Server URL and input basic authentication.
     */
    public static NuxeoClient.Builder createClientBuilder(String login, String password) {
        return createClientBuilder(new BasicAuthInterceptor(login, password));
    }

    /**
     * @return A {@link NuxeoClient.Builder} filled with Nuxeo Server URL and given authentication.
     */
    protected static NuxeoClient.Builder createClientBuilder(Interceptor authenticationMethod) {
        return new NuxeoClient.Builder().url(BASE_URL).authentication(authenticationMethod).timeout(60);
    }

    public void resetRemoteDocuments() {
        // see COUNT_TEST_FOLDERS() +> must delete the folders created there.
        for (int i = 1; i <= COUNT_TEST_FOLDERS; i++) {
            try {
                Document folder = nuxeoClient.repository().fetchDocumentByPath("/folder_" + i);
                nuxeoClient.repository().deleteDocument(folder);
            } catch (NuxeoClientRemoteException e) {
                if (e.getStatus() == 404) {
                    // All good, we can ignore that
                } else {
                    throw e;
                }
            }
        }

    }

    public void initRemoteDocuments() {

        // Create user(s)/Group(s)
        createRemoteUser(nuxeoClient, USER1);
        createRemoteGroup(nuxeoClient, GROUP1);

        // Reset previous data
        resetRemoteDocuments();

        // Create documents
        for (int i = 1; i <= COUNT_TEST_FOLDERS; i++) {
            Document doc = Document.createWithName("folder_" + i, "Folder");
            doc.setPropertyValue("dc:title", "Folder " + i);
            nuxeoClient.repository().createDocumentByPath("/", doc);
        }

        // ----------> Create 3 Note documents
        Document doc;
        for (int i = 0; i < 3; i++) {
            doc = Document.createWithName("note_" + i, "Note");
            doc.setPropertyValue("dc:title", "Note " + i);
            doc.setPropertyValue("note:note", "Note " + i);
            nuxeoClient.repository().createDocumentByPath("/folder_1", doc);
        }
        // ----------> Add one Picture
        doc = Document.createWithName("picture_1", "Picture");
        doc.setPropertyValue("dc:title", "Picture 1");
        nuxeoClient.repository().createDocumentByPath("/folder_1", doc);
        File file = FileUtils.getResourceFileFromContext("sample.jpg");
        org.nuxeo.client.objects.blob.FileBlob fileBlob = new FileBlob(file, file.getName(), "image/jpeg");
        nuxeoClient.operation(BLOB_ATTACH_ON_DOCUMENT)
                   .voidOperation(true)
                   .param("document", "/folder_1/picture_1")
                   .input(fileBlob)
                   .execute();

        // ----------> Add an empty Video Picture
        doc = Document.createWithName("video_1", "Video");
        doc.setPropertyValue("dc:title", "Video 1");
        nuxeoClient.repository().createDocumentByPath("/folder_1", doc);

        // ----------> Create documents with permissions
        // /folder_2/file1
        doc = createDocument("/folder_2", "File", "file1", TEST_FILE_TITLE, "blob.json", "text/plain");
        nuxeoClient.operation("Document.AddToFavorites").input(TEST_FILE_PATH).execute();
        // ----------------------- We block inheritance so we only have the permissions set later
        nuxeoClient.operation("Document.BlockPermissionInheritance").input(TEST_FILE_PATH).execute();
        // --------------------------------------------
        nuxeoClient.operation("Document.AddPermission")
                   .param("permission", "Everything")
                   .param("username", USER1)
                   .input(TEST_FILE_PATH)
                   .execute();
        nuxeoClient.operation("Document.AddPermission")
                   .param("permission", SecurityConstants.READ_WRITE)
                   .param("username", GROUP1)
                   .input(TEST_FILE_PATH)
                   .execute();
        nuxeoClient.operation("Document.AddPermission")
                   .param("permission", CUSTOM_PERM_NOT_MAPPED)
                   .param("username", GROUP1)
                   .input(TEST_FILE_PATH)
                   .execute();

        // page providers can leverage Elasticsearch so wait for indexing before starting tests
        nuxeoClient.operation(ES_WAIT_FOR_INDEXING).param("refresh", true).param("waitForAudit", true).execute();
    }

    public Document createDocument(String destPath, String docType, String name, String title, String testFilePath,
            String fileMimeType) {
        Document doc = null;

        doc = Document.createWithName(name, docType);
        doc.setPropertyValue("dc:title", title);
        doc = nuxeoClient.repository().createDocumentByPath(destPath, doc);

        if (StringUtils.isNotBlank(testFilePath)) {
            File file = FileUtils.getResourceFileFromContext(testFilePath);
            FileBlob fileBlob = new FileBlob(file, file.getName(), fileMimeType);

            nuxeoClient.operation(BLOB_ATTACH_ON_DOCUMENT)
                       .voidOperation(true)
                       .param("document", destPath + "/" + name)
                       .input(fileBlob)
                       .execute();
        }

        return doc;
    }

    public static User createRemoteUser(NuxeoClient nxClient, String userId) {

        UserManager userManager = nxClient.userManager();
        assertNotNull(userManager);

        User user = null;
        // fetcUser() does not just return a null value if the userId does ot exists, it throws an exception
        try {
            user = userManager.fetchUser(userId);
        } catch (NuxeoClientRemoteException e) {
            if (!"user does not exist".equals(e.getMessage())) {
                throw e;
            }
        }

        if (user == null) {
            user = new User();
            user.setUserName(userId);
            user.setPassword(DEFAULT_PASSWORD);
            user.setGroups(Collections.singletonList(GROUP_MEMBERS));

            user = userManager.createUser(user);
        }

        return user;
    }

    public static Group createRemoteGroup(NuxeoClient nxClient, String groupId) {

        UserManager userManager = nxClient.userManager();
        assertNotNull(userManager);

        Group group = null;
        // fetchGroup() does not just return a null value if the userId does ot exists, it throws an exception
        try {
            group = userManager.fetchGroup(groupId);
        } catch (NuxeoClientRemoteException e) {
            if (!"group does not exist".equals(e.getMessage())) {
                throw e;
            }
        }

        if (group == null) {
            group = new Group();
            group.setGroupName(groupId);
            group.setGroupLabel(groupId);

            group = userManager.createGroup(group);
        }

        return group;
    }

}
