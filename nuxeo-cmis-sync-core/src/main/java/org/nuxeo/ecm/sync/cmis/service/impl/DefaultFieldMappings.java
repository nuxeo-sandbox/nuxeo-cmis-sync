package org.nuxeo.ecm.sync.cmis.service.impl;

import java.util.LinkedList;
import java.util.List;

public class DefaultFieldMappings {

    static final List<CMISFieldMappingDescriptor> MAPPINGS = new LinkedList<CMISFieldMappingDescriptor>();

    static {
        MAPPINGS.add(new CMISFieldMappingDescriptor("Document Name", "dc:title", "cmis:name"));
        MAPPINGS.add(new CMISFieldMappingDescriptor("Document Description", "dc:description", "cmis:description"));
        MAPPINGS.add(new CMISFieldMappingDescriptor("Creation Date", "dc:created", "cmis:creationDate"));
    }

}
