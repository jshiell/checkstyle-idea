# CheckStyle-IDEA

&copy; Copyright 2006-2022 CheckStyle-IDEA Contributors

Hosted on [GitHub](https://github.com/jshiell/checkstyle-idea)

[![Build Status](https://github.com/jshiell/checkstyle-idea/workflows/Build/badge.svg)](https://github.com/jshiell/checkstyle-idea/actions?query=workflow%3A%22Build%22)
[![Code Quality: Java](https://img.shields.io/lgtm/grade/java/g/jshiell/checkstyle-idea.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/jshiell/checkstyle-idea/context:java)
[![Total Alerts](https://img.shields.io/lgtm/alerts/g/jshiell/checkstyle-idea.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/jshiell/checkstyle-idea/alerts)

A plug-in for JetBrains' IntelliJ IDEA 2021/2022 which provides real-time feedback against a given
[CheckStyle 8-10](https://checkstyle.sourceforge.io) profile by way of an inspection.

Please note this is not an official part of Checkstyle - they neither endorse
    nor bear responsibility for this plugin. The logo is sourced from the [Checkstyle resources
    repository](https://github.com/checkstyle/resources/tree/master/img/cs-logos-twitter-gplus-backgrounds)
    and used under the [CC BY 4.0 licence](https://github.com/checkstyle/resources/blob/master/img/README.txt).

Released under a BSD-style licence - please see the LICENCE file for details.

## Use

Once installed, a new inspection will be available in the group 'CheckStyle'. The 'Inspections' item in the preferences
panel will allow you to turn this on and to configure it.

Project exceptions are treated a little oddly. Because CheckStyle demands these to be on the current classpath, errors
will appear if these have not as yet been compiled. Further, because we cache the real-time checkers for performance
reasons, real-time scans may continue to show the errors after a compilation. A static scan will force a reload of the
Checker and should resolve this.


## Configuration

Configuration is available under the *Settings* dialogue, under *Tools* -> *Checkstyle*. This controls configuration for both the inspection and static
scanning.

### Configuration Files

The main configuration option is that of the CheckStyle file. Multiple CheckStyle file may be added, and swapped between
by using the checkbox. Files may be added using the 'Add' button, or you can use the versions of the standard Sun and 
Google configuration that are bundled with the selected version of Checkstyle.

If you need to pass authentication information for rules file accessed via HTTP then you can use the `https://user:pass@host/` form to do so.

The *Scan Test Classes* checkbox will enable scanning of Java files under test source roots. If disabled, these files
will be ignored.

If a custom file is being used and properties are available for definition then these will accessible using the 'Edit
Properties' button.

### Eclipse-CS Variable Support

The following variables will be available if you have not otherwise overridden their values:

* **basedir** - mapped to the location of the current module file, or the project directory as a fallback. 
* **project_loc**, **workspace_loc** - mapped to the project directory.
* **config_loc**, **samedir** - mapped to the directory the rules file is in, or the project directory for remote rules
  files (e.g. HTTP).

### Third Party Checks 

This tab allows you to specify any third-party checks which your configuration file makes use of. All selected
directories/JAR files will be added to CheckStyle's classpath.

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

If you're on OS X, use IDEA with the bundled JVM. Otherwise, please ensure IDEA is running using Java 8 or later.
[Jetbrains offer a support document on this
subject](https://intellij-support.jetbrains.com/entries/23455956-Selecting-the-JDK-version-the-IDE-will-run-under).

### I see 'Got an exception - java.lang.RuntimeException: Unable to get class information for <Exception Class>. (0:0)'

CheckStyle is unable to retrieve information on exceptions in your project until you have built it. Build your project
in IDEA and then rescan.


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
