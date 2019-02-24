# Config ADT Documentation
This document explains how the config XML is represented to pass in between the configuration GUI tool.

## class ConfigXML
This class is an ADT represents the checkstyle XML, which allows the user not having to worry about parsing. For more detailed structure of the configuration structure, please see http://checkstyle.sourceforge.net/config.html

### Fields
* `Map<String, String> MetaData` - The metadata which are direct children of root module `Checker`. The key is the name of the metadata, and the value is the value of the metadata.

* `Map<String, String> Properties` - The property which are direct children of root module `Checker`. The key is the name of the property, and the value is the value of the properties.

* `List<Element> Modules` - A list of modules which are direct children of root module `Checker`.

NOTE: Maybe need new helper function or constructors, but sill add if we have time
