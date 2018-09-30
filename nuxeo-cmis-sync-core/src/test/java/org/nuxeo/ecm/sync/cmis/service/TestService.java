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
package org.nuxeo.ecm.sync.cmis.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.apache.chemistry.opencmis.client.api.Session;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.sync.cmis.TestHelper;
import org.nuxeo.ecm.sync.cmis.api.CMISRemoteService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Test service and EPs.
 *
 * @author dbrown@nuxeo.com
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy({ "org.nuxeo.ecm.sync.cmis", "org.nuxeo.ecm.sync.cmis:OSGI-INF/cmis-repository-test-contribs.xml" })
// @Ignore
public class TestService {

    @Inject
    protected CMISRemoteService cmis;

    @Test
    public void testRepositoryConnection() throws Exception {

        Assume.assumeTrue("No distant CMIS server can be reached", TestHelper.isTestCMISServerRunning(cmis, TestHelper.TEST_CONNECTION_REMOTE_NUXEO));

        Session ses = cmis.createSession(TestHelper.TEST_CONNECTION_REMOTE_NUXEO);
        assertNotNull(ses);
        assertEquals("default", ses.getRepositoryInfo().getId());
    }

    @Test
    public void testRepositoryMapping() throws Exception {
        // We are testing the configuration. See cmis-repository-test-contribs.xml
        // 3 mappings in total. 1 for all, 1 for File, 1 for Picture
        assertEquals(3, cmis.getFieldMapping(TestHelper.TEST_CONNECTION_REMOTE_NUXEO, null).size());
        assertEquals(1, cmis.getFieldMapping(TestHelper.TEST_CONNECTION_REMOTE_NUXEO, "Document").size());
        assertEquals(1, cmis.getFieldMapping(TestHelper.TEST_CONNECTION_REMOTE_NUXEO, "Folder").size());
        assertEquals(2, cmis.getFieldMapping(TestHelper.TEST_CONNECTION_REMOTE_NUXEO, "File").size());
        assertEquals(2, cmis.getFieldMapping(TestHelper.TEST_CONNECTION_REMOTE_NUXEO, "Picture").size());
    }

}
