# Nuxeo CMIS Synchronization

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=Sandbox/sandbox_nuxeo-cmis-sync-master)](https://qa.nuxeo.org/jenkins/view/Sandbox/job/Sandbox/job/sandbox_nuxeo-cmis-sync-master/)

Transparent CMIS synchronization by path

## Build and Install

Build with maven (at least 3.3)

```
mvn clean install
```
> Package built here: `nuxeo-cmis-sync-package/target`

> Install with `nuxeoctl mp-install <package>`

## Contributions


```
  <extension target="org.nuxeo.ecm.sync.cmis.service.CMISRemoteServiceComponent" point="connection">
    <connection name="test" enabled="true" binding="browser">
      <repository>default</repository>
      <url>http://some.server.com/nuxeo/json/cmis</url>
      <username>Administrator</username>
      <credentials>123</credentials>
      <property key="prop1">123</property>
      <property key="prop2">something</property>

      <!-- Example of a list of ACE mapping -->
      <field-mapping name="Copy Description for Everything" xpath="dc:description"
            property="dc:description" />
      <field-mapping name="Copy coverage for files" xpath="dc:coverage" property="dc:coverage"
            doctype="File" />
      <field-mapping name="Update value for picture" xpath="c:c" property="prop_c" doctype="Picture" />
      <field-mapping name="Map custom distant field for files" xpath="contractNum" property="customschema:contract"
            doctype="File" />

      <!-- Example of a list of ACE mapping -->
      <ace-mapping>
        <remoteAce value="permToRead">Read</remoteAce>
        <remoteAce value="permToWrite">ReadWrite</remoteAce>
        <remoteAce value="permFprAll">Everything</remoteAce>
      </ace-mapping>
    </connection>
  </extension>
```

## Operations

- `Document.CMISSync`: Synchronize individual pieces of content
- `Repository.CMISImport`: Import folder-based items from remote repositories

## Support

**These features are sand-boxed and not yet part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.

## Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

## About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.

More information is available at [www.nuxeo.com](http://www.nuxeo.com).

