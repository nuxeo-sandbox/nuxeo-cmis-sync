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
package org.nuxeo.ecm.sync.cmis.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.Session;
import org.nuxeo.ecm.sync.cmis.service.CMISMappingDescriptor;
import org.nuxeo.ecm.sync.cmis.service.CMISConnectionDescriptor;

public interface CMISRemoteService {

    Session createSession(String name);

    List<CMISMappingDescriptor> getMappings(String doctype);

    /**
     *
     * @return (for the connection) an unmodifiable map of elements whose key is the remote ACE, value is the local ACE to apply
     * @since 10.2
     */
    Map<String, String> getAceMappings(String connection);

    Collection<String> getRepositoryNames();

    CMISConnectionDescriptor getConnectionDescriptor(String connection);

}
