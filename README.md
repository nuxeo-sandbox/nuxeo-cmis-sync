# Nuxeo CMIS Synchronization

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=Sandbox/sandbox_nuxeo-cmis-sync-master)](https://qa.nuxeo.org/jenkins/view/Sandbox/job/Sandbox/job/sandbox_nuxeo-cmis-sync-master/)

Transparent CMIS synchronization by path. Current Nuxeo server is the _local_ server and it can fetch and synchronize with a remote repository, via CMIS.

## Overview

The plugin provides a **configurable service** (with mappings for fields, doc types, and permissions) and some **operations**, and the typical way it works is the following:

1. Synchronize a remote folder with a local one (using the path to this remote folder). This imports the children, not yet fully synchronized
2. The plugin installs listeners: When a document is created in the context of a CMIS Import, the listeners then automatically synchronize the local document with the remote one, applying:
  * A field mapping
  * A permission mapping
3. At any time later a document can be synchronized with its remote "sibling", fetching changes applied to the remote
  * **IMPORTANT**: The local document fetches changes in the remote, it does not push any metadata, it is not a bi-way synchronization.

The plugin contributes WebUI action buttons allowing a user to perform the initial importation, and — when needed — the individual updates.

### Technical Overview
When importing a folder, the plugin adds a [`CMISSync`](nuxeo-cmis-sync-core/src/main/resources/OSGI-INF/CoreExtensions.xml) facet to the parent and to every imported child. This facet comes with a [`cmissync`](nuxeo-cmis-sync-core/src/main/resources/schema/cmissync.xsd) schema that gives information about the remote document linked to the local one (remote UID, URI, ...).

## Configuration / XML Contribution

### Overview
The `connection` point of the `org.nuxeo.ecm.sync.cmis.service.CMISRemoteServiceComponent` component allows for configuring:

* Information about the remote server (URL, login, ...)
* Mapping for document types:
  * Map the remote doc type to a local one
  * When no mapping is provided, the plugin uses `File` or `Folder`
* Mapping for fields:
  * Which local field (`xpath`) to use to get the value stored in the remote one (`property`)
  * Can be made either specific to a document type, or available for every imported document
* Mapping for permissions:
  * Set the synchronization method:
    * `addIfNotSet` will add the permission, without changing the existing one.
    * `replaceAll` will replace all permissions with the new one(s)
  * And the mapping: NAme of the remote permission -> value in Nuxeo
  * **WARNING**: In current implementation, if a user referenced in the remote document does not exist in current repository, the synchronization will fail.

**it is possible to define as many as remote repository as needed, each of them with their own mappings**. The `name` property of the extension point is used as unique identifier for each connection.


### Example
```
<extension target="org.nuxeo.ecm.sync.cmis.service.CMISRemoteServiceComponent"
           point="connection">
  <connection name="remoteNuxeo" enabled="true" binding="browser">
    <repository>default</repository>
    <url>http://localhost:8080/nuxeo/json/cmis</url>
    <username>Administrator</username>
    <credentials>Administrator</credentials>

    <!-- Example of a list of Document Types mapping -->
    <doctype-mapping>
      <!--  Example when mapping between 2 Nuxeo repositories -->
      <doctype value="File">File</doctype>
      <doctype value="Note">Note</doctype>
      <doctype value="Picture">Picture</doctype>
      <doctype value="CustomContract">Contract</doctype>
      <doctype value="Claim">Claim</doctype>
      
      <!--  Example with custom doc types remote/local -->
      <doctype value="basecontract">Contract</doctype>
      <doctype value="pdfdoc">File</doctype>
      <doctype value="claim_image">Picture</doctype>       
    </doctype-mapping>

    <!-- Example of a list of field mapping -->
    <field-mapping name="Copy Description for Everything" xpath="dc:description" property="dc:description" />
    <field-mapping name="Copy coverage for files" xpath="dc:coverage" property="dc:coverage"
      doctype="File" />
    <field-mapping name="Update value for picture" xpath="c:c" property="prop_c" doctype="Picture" />

    <!-- Example of a list of ACE mapping -->
    <ace-mapping>
      <method>addIfNotSet</method>
      <remoteAce value="cmis:read">Read</remoteAce>
      <remoteAce value="cmis:write">ReadWrite</remoteAce>
      <remoteAce value="cmis:all">Everything</remoteAce>
      <remoteAce value="Everything">Everything</remoteAce>
    </ace-mapping>

  </connection>

  <!-- Here, another connection -->
  <connection name="remoteRepository" enabled="true" binding="browser">
    . . . etc . . .
  </connection>
  
</extension>
```


## Operations

- `Repository.CMISConnections`: Return the list of connections set up in the XML configuration
- `Repository.CMISImport`: Import folder-based items from remote repositories
- `Document.CMISSync`: Synchronize individual pieces of content


## Build and Install (and Test)

Build with maven (at least 3.3)

```
mvn clean install
```
> Package built here: `nuxeo-cmis-sync-package/target`

> Install with `nuxeoctl mp-install <package>`


### Testing/Unit-Testing
For unit test with maven, the plugin deploys a Nuxeo distribution, starts it on port 8080, performs the tests, then stop the distribution. This means **the unit tests will fail if you already have a Nuxeo (or any application for that mater) running on localhost:8080**.

To debug the unit tests from an IDE (Eclipse, ...); just start a nuxeo and make sur the test contribution can reach it (URL, login, password)

### Known Limitations
* Support only for BASIC authentication
* No fine-tuning of the mappings, no callbacks to adapt it dynamically (maybe based on the value of fields for example)

## Support

**These features are sand-boxed and not yet part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.

## Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

## About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.

More information is available at [www.nuxeo.com](http://www.nuxeo.com).

