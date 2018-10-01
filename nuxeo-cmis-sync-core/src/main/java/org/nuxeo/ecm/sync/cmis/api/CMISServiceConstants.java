/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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

/**
 *
 * @since 10.2
 */
public interface CMISServiceConstants {

    String XPATH_REMOTE_UID = "cmissync:uid";

    String XPATH_CONNECTION = "cmissync:connection";

    String XPATH_REPOSITORY = "cmissync:repository";

    String XPATH_TYPE = "cmissync:type";

    String XPATH_PATHS = "cmissync:paths";

    String XPATH_STATE = "cmissync:state";

    String XPATH_SYNCHRONIZED = "cmissync:synchronized";

    String XPATH_MODIFIED = "cmissync:modified";

    String XPATH_URI = "cmissync:uri";

    String SYNC_ACL = "CmisSync";

    String SYNC_FACET = "CMISSync";

}
