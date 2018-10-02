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
 *     Thibaud Arguillere
 */
package org.nuxeo.ecm.sync.cmis;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

import org.nuxeo.ecm.sync.cmis.api.CMISRemoteService;
import org.nuxeo.ecm.sync.cmis.service.impl.CMISConnectionDescriptor;

/**
 * @since 10.2
 */
public class TestHelper {

    public static final String CONNECTION_NUXEO_RESET_PERMS = "remoteNuxeo";

    public static final String CONNECTION_NUXEO_REPLACE_PERMS = "remoteNuxeoReplacePermissions";

    public static boolean isTestCMISServerRunning(CMISRemoteService cmis, String connection) {

        CMISConnectionDescriptor desc = cmis.getConnectionDescriptor(connection);
        if (desc != null) {
            String urlStr = desc.getUrl();
            try {
                URL url = new URL(urlStr);

                HttpURLConnection huc = (HttpURLConnection) url.openConnection();
                huc.setRequestMethod("HEAD");
                // Should receive 401 (unauthenticated), but any code is ok, means the server is responding
                /* int ignore = */ huc.getResponseCode();

                return true;
            } catch (UnknownHostException e) {
                // This is the error we should have if the server is not running
                System.out.println("Test server URL <" + urlStr + "> can't be reached");
                return false;
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

}
