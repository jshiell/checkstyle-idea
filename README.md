# CheckStyle-IDEA

&copy; Copyright CheckStyle-IDEA Contributors

Hosted on [GitHub](https://github.com/jshiell/checkstyle-idea)

[![Build Status](https://github.com/jshiell/checkstyle-idea/workflows/Build/badge.svg)](https://github.com/jshiell/checkstyle-idea/actions?query=workflow%3A%22Build%22)

A plug-in for JetBrains' IntelliJ IDEA 2025, and 2026 which provides real-time feedback against a given
[CheckStyle 10-14](https://checkstyle.sourceforge.io) profile by way of an inspection.

Please note this is not an official part of Checkstyle - they neither endorse
    nor bear responsibility for this plugin. The logo is sourced from the [Checkstyle resources
    repository](https://github.com/checkstyle/resources/tree/master/img/cs-logos-twitter-gplus-backgrounds)
    and used under the [CC BY 4.0 licence](https://github.com/checkstyle/resources/blob/master/img/README.txt).

Released under a BSD-style licence - please see the LICENCE file for details.

## Use

Once installed, a new inspection will be available in the group 'CheckStyle'. The 'Inspections' item in the preferences
panel will allow you to turn this on and to configure it.

## Configuration

Configuration is available under the *Settings* dialogue, under *Tools* -> *Checkstyle*. This controls configuration for both the inspection and static
scanning.

### Configuration Files

The main configuration option is that of the CheckStyle file. Multiple CheckStyle file may be added, and swapped between
by using the checkbox. Files may be added using the 'Add' button, or you can use the versions of the standard Sun and 
Google configuration that are bundled with the selected version of Checkstyle.

If you need to pass authentication information for rules file accessed via HTTP then you can use the `https://user:pass@host/` form to do so.

The *Scan scope* dropdown controls which files are scanned. It offers:

* *Only Java sources (but not tests)* - Java files under source roots, excluding test source roots. This is the default.
* *Only Java sources (including tests)* - as above, but test source roots are scanned too.
* *All sources (but not tests)* - every file under source roots, whatever its type, excluding test source roots.
* *All sources (including tests)* - as above, but test source roots are scanned too.
* *All files in project* - every file in the project, regardless of whether it sits under a source root.

The *Scan modified files before commit* checkbox will run Checkstyle over the files in the changelist when you commit,
and offer to abort the commit if any problems are found. The same option appears in the commit dialogue, and under
*Settings* -> *Version Control* -> *Commit*; all three edit the same setting.

If a custom file is being used and properties are available for definition then these will accessible using the 'Edit
Properties' button.

### Detecting a Conventional Configuration File

The *Detect Checkstyle Configuration File* action (in the CheckStyle tool window's action group) scans the project
root for a Checkstyle configuration file at one of three conventional locations, in priority order:

1. `config/checkstyle/checkstyle.xml` - the Gradle/Maven Checkstyle plugin default.
2. `checkstyle.xml`
3. `etc/checkstyle.xml`

The first match wins; there is no merging of multiple matches. This is a manual action only - it does not watch the
filesystem - so invoke it again after a location appears or disappears, for example after a `git pull`. Each
invocation is idempotent: it adds a location for the first match found, removes it again if nothing matches any
more, and otherwise leaves things as they are. It only ever manages the single location it created itself, so it never overrides or removes a location you added by
hand under a different description or path. (If a location you added by hand happens to have the exact same
description, path and type as the detected one, it may be adopted as the managed location - a rare, pre-existing
edge case of how locations are identified internally.)

### Eclipse-CS Variable Support

The following variables will be available if you have not otherwise overridden their values:

* **basedir** - mapped to the external project directory of the current module (i.e. the Gradle subproject or Maven
  module directory), falling back to the module content root, and then to the project directory.
* **project_loc**, **workspace_loc** - mapped to the project directory.
* **config_loc**, **samedir** - mapped to the directory the rules file is in, or the project directory for remote rules
  files (e.g. HTTP).

These variables may also be referenced from the values you set under 'Edit Properties', which is how a single property
can take a different value in each module. For example, in a multi-module Gradle build, setting a `baseDir` property to
`${basedir}` and referencing it from your rules file:

```xml
<module name="SuppressionFilter">
    <property name="file" value="${baseDir}/gradle/checkstyle-exclude.xml"/>
    <property name="optional" value="true"/>
</module>
```

will resolve each subproject's own suppressions file. A reference to a variable that isn't set is left as-is.

### Splitting a Configuration Across Files

A rules file can pull in other files using XML entity includes, with paths resolved relative to the including file:

```xml
<!DOCTYPE module PUBLIC "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
        "https://checkstyle.org/dtds/configuration_1_3.dtd" [
        <!ENTITY additionalModules SYSTEM "./additional-modules.xml">
        ]>
<module name="Checker">
    <module name="TreeWalker">&additionalModules;</module>
</module>
```

Checkstyle does not resolve these unless you opt in, and neither does this plugin. To enable them, add

```
-Dcheckstyle.enableExternalDtdLoad=true
```

via *Help* → *Edit Custom VM Options*, and restart the IDE. This is the same opt-in the Checkstyle CLI and the
Gradle plugin require. Note that it is an IDE-wide setting rather than a per-project one, and that it enables
external entity resolution generally — including entities with absolute `http://` URLs, which are fetched without
any plugin-side resolver in front of them. Enable it only if you trust the rules files you open.

Includes are resolved relative to rules files held on the local filesystem. A rules file loaded over HTTP or from
the third-party checks classpath has nothing to resolve a relative include against, so its includes will not be
found.

Properties declared in an included file appear under 'Edit Properties' once the option is set, again for local
rules files.

### Third Party Checks 

This tab allows you to specify any third-party checks which your configuration file makes use of. All selected
directories/JAR files will be added to CheckStyle's classpath.

**Third-party checks are only loaded in trusted projects.** Because these entries are read from the project's own
`.idea/checkstyle-idea.xml`, and because loading a check means running its code, a project that has not been
trusted gets its third-party classpath skipped entirely — the built-in checks still run as normal. Trusting the
project (*File* → *Trust Project*, or the banner shown when it was opened) enables them immediately, with no IDE
restart. The same applies to a rules file that is resolved via the third-party classpath rather than as a plain
file: it cannot be loaded until the project is trusted.

Third-party classpath entries can be shared across a team via version control, without hard-coding any one
developer's machine layout, because the IDE platform substitutes portable path macros into `checkstyle-idea.xml`
automatically:

- A jar under the project directory is saved as a `$PROJECT_DIR$`-relative path.
- A jar under a developer's home directory is saved as a `$USER_HOME$`-relative path — this only works if every
  teammate has the jar at the same path relative to their own home directory.
- A jar anywhere else (a shared network path, `/opt/...`, a corporate mirror) can be made portable by defining a
  Path Variable for it (*Settings* → *Appearance & Behavior* → *Path Variables*); each developer sets the
  variable's value to match their own machine.

To check it worked, open `.idea/checkstyle-idea.xml` after saving settings and look for the macro token in the
`thirdPartyClasspath` entries.

If none of the above applies, or a teammate's copy of the referenced file is missing or at a different path, the
entry is *not* skipped — it's added to the classpath as-is regardless of whether the file exists. If your rules
file doesn't actually reference a check from that entry, nothing happens. If it does, Checkstyle fails to load
that check, and the plugin shows an error notification and blocks the affected configuration location entirely
(not just the one check) until the path resolves.

For Maven projects, prefer "Import settings from Maven" for third-party Checkstyle rule jars declared as
`maven-checkstyle-plugin` dependencies — it resolves the correct path on every machine regardless of where the
local repository lives, so no committed/shared path is needed at all.

For Gradle projects, "Import settings from Gradle" reads a project's `checkstyle {}` configuration (or, if that's
unconfigured, a raw `task checkstyle(type: Checkstyle) { ... }` such as Android Gradle Plugin's) during sync, and
imports its `configFile`, `configProperties`, and `toolVersion`. It does not currently resolve third-party check
jars from Gradle dependencies.

### Copy libraries from project directory

The option "Copy libraries from project directory" will tell Checkstyle-IDEA to do the following when creating custom
classloaders:

- scan a module's classpath and select those library entries which reside somewhere below the project directory
- copy those libraries to a separate temporary directory (normally under `.idea`, if there is no `.idea` directory,
  the system temp directory is used)

The internal classloaders will then use those copied libraries, thus preventing them from getting locked in the file
system. Since this is mainly a problem on Windows, this feature is activated by default on Windows. If you know that
all your libraries reside outside of the project (as is often the case when build tools such as Maven or Gradle are
used), then you can disable this feature. Since it slows down checker creation, you might want to keep it disabled
until necessary. After changing this option, it may be necessary to restart IDEA to see the effects.

## Adding additional pre-bundled configurations

Where you have a shared distribution it may be useful to add additional pre-bundled configurations. This can be done
by placing a JAR into the `lib` directory of the plugin. The JAR should contain:

* A class that implements the `org.infernus.idea.checkstyle.csapi.BundledConfigProvider` interface
* A text file `META-INF/service/org.infernus.idea.checkstyle.csapi.BundledConfigProvider` that contains the fully qualified classname of the `BundledConfigProvider`
* The Checkstyle rules XML file

At present the `BundledConfigProvider` interface isn't available separately; hence you'll need the plugin JAR to compile.

## Troubleshooting

If an error occurs during the check an exception will be thrown, which IDEA will then catch and display in the standard
exceptions dialogue. If you're unsure as to why things are awry this would be your best bet - chances are it's a missing
property or classpath pre-requisite.

## Notable Extensions

### [sevntu.checkstyle](http://sevntu-checkstyle.github.io/sevntu.checkstyle/)

*sevntu.checkstyle* offers a number of useful checks written by students of the Sevastopol National Technical University
(SevNTU). They're also kind enough to offer instructions on setting them up with this plugin.

### [Checkstyle Addons](http://checkstyle-addons.thomasjensen.com/)

*Checkstyle Addons* offers additional Checkstyle checks not found in other Checkstyle extensions, and it's easy to
[set up in Checkstyle-IDEA](http://checkstyle-addons.thomasjensen.com/run.html#run-intellij).

## Development

Note that the plugin has been entirely developed on OS X - while it should be fine on Linux, I've no idea what result
you'd get with Windows. YMMV.

The pre-requisites for the plugin are fairly light - you'll need Git and JDK 11. Make sure your `JAVA_HOME`
environment variable is set correctly before invoking Gradle.

    git clone https://github.com/jshiell/checkstyle-idea.git checkstyle-idea
    cd checkstyle-idea

You can then easily build via Gradle:

    ./gradlew clean build

To run it in a sandboxed IDEA, run:

    ./gradlew runIde

To debug the plugin, import the plugin into IDEA as a Gradle project, and then use the `runIdea` Gradle target in debug
mode. 

## Frequently Asked Questions

If you're on OS X, use IDEA with the bundled JVM. Otherwise, please ensure IDEA is running using Java 11 or later.
[Jetbrains offer a support document on this
subject](https://intellij-support.jetbrains.com/entries/23455956-Selecting-the-JDK-version-the-IDE-will-run-under).

## Limitations

* If you import Gradle project withs **Create separate module per source set** active in IDEA 2016 or above then the
  module source paths are truncated. This means relative paths (e.g. suppressions on `src/test/.+`) may not work as
  expected.
* The plugin will throw exceptions if used with class files targeted at a later version than that of the JDK used by
  IDEA. Please run IDEA on the latest available JVM, ideally the bundled version from JetBrains where available.
* If you change the configuration options the real-time scan will not be updated until the file is either changed or
  reopened.
* We do not check if a property definition is required for a given file. Hence you can exit configuration without
  setting required properties. Given, however, that CheckStyle files can change without the plug-in being aware this is
  something we'll always have to live with to some degree.
* CheckStyle errors and warnings from the inspection are both shown at a single level, as IDEA will only allow one
  warning level for an inspection.

## Feedback

Any comments or bug reports are most welcome - please visit
the project website on [GitHub](https://github.com/jshiell/checkstyle-idea/).

## I need debug information!

The debug logging of the plugin is arcane and not particularly well done, for which I can only thank myself. However, if 
such context is needed then it can be seen by using IDEA's **Help** -> **Debug Log Settings...** and adding:

    #org.infernus.idea.checkstyle

## Acknowledgements

This plug-in owes its existence to both the style-overlords at work mandating compliance with a CheckStyle
configuration, and the [Eclipse-CS](http://eclipse-cs.sourceforge.net/) authors for making me jealous of the real-time
scan support available for Eclipse.

Thanks to those who have contributed work and effort directly to this project:

* J. G. Christopher
* jicken
* Jonas Bergvall
* Edward Campbell
* LightGuard.JP
* Gerhard Radatz
* Benjy W
* Yuri
* Kristin Young
* Simon Billingsley
* Miel Donkers
* Dmitrij (zherebjatjew)
* Thomas Jensen
* Rustam Vishnyakov (@dyadix)
* Thomas Harning (@harningt)
* František Hartman (@frant-hartm)
* Victor Alenkov (@BorzdeG)
* Baron Roberts (@baron1405)
* George Kankava (@georgekankava)
* Thomas Jensen (@tsjensen)
* Klaus Tannenberg (@KTannenberg)
* Nikolay Bespalov (@nikolaybespalov)
* @zentol
* Joey Lee (@yeoji)
* Tim van der Lippe (@TimvdLippe)
* @tduehr
* Mark Brown (@embee1981)
* Marshall Walker (@marshallwalker)
* Alexander Schwartz (@ahus1)
* Mustapha Zorgati (@mustaphazorgati)
* Roman Karpenko (@neomoto)
* Akash Mondal (@AkMo3)
* Bruno Masetto (@bmasetto)
* Robert Kruszewski (@robert3005)
* Hyeonmin Park (@kennysoft)
* Barnesly (@andrewflbarnes)
* Jan Köper (@JanK411)
* Jeremy Ziegler (@Uschi003)
* Ali Ustek (@austek)
* Richard (@rhierlmeier)
* James Baker (@james-baker-aera)
* @LlamaLad7
* @thorpp
* @dong4j
* Leon Schenk (@leonschenk)
* Nicholas Rayburn (@nrayburn-tech)
* Andreas Schrell (@foto-andreas)

And also thanks are due to the authors and contributors of:

* Eclipse-CS, for inspiration and solutions to coding problems.
* JetStyle, for filling the area of static scanning and also giving me inspirations on coding solutions.
* CheckStyle, for without them we'd have merely void and chaos.
* JetBrains, for an IDE which is worth every penny and then some.

And a big thank-you to everyone who's sent me feedback or bug reports - both are much appreciated!


## Licence

This code is released under a BSD licence, as specified in the accompanying LICENCE file.


## Version History

Please see [the changelog](CHANGELOG.md).
