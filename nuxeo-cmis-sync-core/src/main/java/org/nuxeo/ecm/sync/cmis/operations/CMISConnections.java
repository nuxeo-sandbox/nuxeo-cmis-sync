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
 */
package org.nuxeo.ecm.sync.cmis.operations;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.sync.cmis.api.CMISRemoteService;

/**
 * Synchronize individual documents
 */
@Operation(id = CMISConnections.ID, category = Constants.CAT_FETCH, label = "CMIS Connections", description = "List of CMIS connection names.")
public class CMISConnections {

    public static final String ID = "Repository.CMISConnections";

    static final Log log = LogFactory.getLog(CMISConnections.class);

    @Context
    protected CMISRemoteService cmis;

    @OperationMethod
    public String run() {
        return StringUtils.join(this.cmis.getConnectionNames(), ',');
    }

}
