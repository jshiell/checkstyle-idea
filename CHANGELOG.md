
# CheckStyle-IDEA Changelog

* **5.72.0** Now built against IDEA 2021.3.3 (was 2021.1.3).
* **5.71.1** New: Checkin dialogue now shows the number of errors and warnings found (#591).
* **5.71.0** New: Added Checkstyle 10.3.4.
* **5.70.0** New: Added Checkstyle 10.3.2.
* **5.69.1** Fix: handle URIs without userinfo (#583, #581).
* **5.69.0** New: Ensure passwords are hidden when display URIs (#581) - thanks to @austek.
* **5.68.0** New: Added Checkstyle 10.3.1. 
* **5.68.0** Fix: User info requiring escaping in rules file URIs is now handled correctly (#535) - thanks to @austek. 
* **5.67.4** Fix: Process exceptions from IDEA are now correctly rethrown during config deserialisation (#578).
* **5.67.3** Fix: Bundled configurations now have constant IDs when recreated or ported from earlier versions (#569).
* **5.67.3** Fix: Bundled configurations are restored if absent.
* **5.67.2** Fix: NPE in active modules configurations (#576).
* **5.67.1** Fix: Improve serialisation from legacy formats (#574).
* **5.67.1** Fix: Fix serialisation of property values (#573).
* **5.67.0** New: Added Checkstyle 10.3.
* **5.67.0** Fix: Project paths should now remain relative where possible (#569).
* **5.67.0** Fix: Fix NPE in module configuration (#570).
* **5.66.0** New: Added Checkstyle 10.2.
* **5.66.0** Fix: Save all modified files before reloading rules, to ensure we load what people see (#562) - thanks to @ahus1.
* **5.66.0** Fix: Improved thread safety on `ConfigurationLocation` (#568) - thanks to @ahus1.
* **5.65.0** Fix: NPE when active config lookup fails (#566).
* **5.65.0** Fix: Properties not correctly loaded for active files (#565).
* **5.65.0** New: The toolwindow is now available during indexing (#564) - thanks to @ahus1.
* **5.65.0** New: We now use SVG icons (#563) - thanks to @ahus1.
* **5.64.0** New: You can now select multiple active Checkstyle files and scope them individually (#559) - thanks to @JanK411 and @Uschi003. 
* **5.63.0** New: Added Checkstyle 10.1.
* **5.62.0** New: Added Checkstyle 10.0.
* **5.61.1** Fixed: Removed left-over log4j artefacts, to allow compatibility with newer EAPs.
* **5.61.0** New: Added Checkstyle 9.3.
* **5.60.0** New: Added Checkstyle 9.2.1.
* **5.59.1** Fixed: NPE when configuration location has been garbage collected from weak map (#547).
* **5.59.0** New: Added Checkstyle 9.2 - thanks to @andrewflbarnes (#546).
* **5.58.0** New: Added Checkstyle 9.1 - thanks to @kennysoft (#543).
* **5.57.2** Fixed: Plugin now requires a restart on update due to issues with IDEA's dynamic loading (#539).
* **5.57.1** Fixed: Incorrect lower version bound fixed.
* **5.57.0** Fixed: Configuration panel is now lazy init-ed to avoid deadlock (#538).
* **5.57.0** New: Added Checkstyle 9.0.1.
* **5.57.0** New: Now built against IDEA 2021.1.3 (was 2020.1.4).
* **5.56.0** New: Added Checkstyle 9.0 (#536).
* **5.56.0** New: Added Checkstyle 8.45.1.
* **5.55.1** Fixed: NPE when virtual file is only in memory (#533).
* **5.55.0** New: Added Checkstyle 8.45.
* **5.54.0** New: Added Checkstyle 8.44.
* **5.53.1** Fixed: Insecure HTTP locations are now saved correctly (#528).
* **5.53.0** New: Added Checkstyle 8.43.
* **5.52.0** Fixed: Inspection is now activated for whitespace changes (#513 / IDEA-265941).
* **5.52.0** New: Added Checkstyle 8.42 (#524).
* **5.51.0** New: Properties can now be overridden for the bundled Google/Sun configurations (#497).
* **5.51.0** Fixed: Added a couple of missing DTDs to the resolver.
* **5.50.0** Fixed: ImportOrder third-party/special imports are ignored if regexes are not defined (#500).
* **5.50.0** New: Replaced tool window icon with Checkstyle small icon (#519).
* **5.50.0** New: Checkstyle 6 and 7 are no longer supported.
* **5.50.0** New: Now built against IDEA 2020.1.4 (was 2019.1.4).
* **5.49.0** New: Added Checkstyle 8.41.1.
* **5.48.0** New: Added Checkstyle 8.41.
* **5.48.0** Fixed: Added a timeout to avoid deadlock on closing a project during a scan (#515).
* **5.47.0** New: Added Checkstyle 8.40.
* **5.46.0** New: Added Checkstyle 8.38, 8.39.
* **5.45.1** Fixed: optimised download size.
* **5.45.0** New: Added Checkstyle 8.37.
* **5.44.0** New: Added Checkstyle 8.36.2.
* **5.43.0** New: Added Checkstyle 8.36.1.
* **5.42.0** New: Added Checkstyle 8.36.
* **5.41.0** New: Added Checkstyle 8.35.
* **5.40.0** New: Added Checkstyle 8.34.
* **5.39.0** New: Project settings are now under "Tools".
* **5.39.0** New: Now built against IDEA 2019.1.4 (was 2018.1.8).
* **5.38.0** New: Added Checkstyle 8.33.
* **5.37.0** New: Added Checkstyle 8.32.
* **5.36.2** Fixed: avoid malformed annotations with imported code styles (#484).
* **5.36.2** Fixed: code style imports will now toggle wrapping-as-needed on when a line length is set (#487).
* **5.36.1** Fixed: injected fragments are no longer passed to Checkstyle (#485).
* **5.36.0** New: Added Checkstyle 8.31.
* **5.35.9** Fixed: concurrency issue with progress listeners (#486).
* **5.35.9** Fixed: improve catching of parse exceptions from Checkstyle (#485).
* **5.35.8** Fixed: ensure property changes won't affect scans in progress (#425).
* **5.35.8** Fixed: changed serialisation method for better compatibility with IDEA 2020.1 (#476).
* **5.35.8** Fixed: restored support for legacy project directory tokenisation (#481).
* **5.35.7** New: Added Checkstyle 8.30 - thanks to @mustaphazorgati (#478).
* **5.35.7** New: We now try and re-use the larger compatible dependencies between Checkstyle version to substantially reduce the archive size.
* **5.35.6** Fixed: parse exceptions reported by Checkstyle are no longer logged in the event log (#475).
* **5.35.6** Fixed: changed module/project icons to be compatible with EAPs of IDEA 2020.1 (#474).
* **5.35.5** New: Added Checkstyle 8.29 - thanks to @mustaphazorgati (#472).
* **5.35.4** Fixed: File cleanup should no longer throw concurrent modification exceptions if execution is cancelled (#470).
* **5.35.3** Fixed: Problems are no longer duplicated in inspection panel (#467).
* **5.35.3** New: Added Checkstyle 8.28 - thanks to @mustaphazorgati (#468).
* **5.35.2** Fixed: Reverted inspection threading due to perf degradation; reworked inspection locking (#462, #466).
* **5.35.2** New: Logo added for plugin marketplace - thanks to @ahus1 (#465).
* **5.35.1** Fixed: Changed inspection threading to avoid deadlocking (#462).
* **5.35.0** New: Added Checkstyle 8.27.
* **5.35.0** Fixed: radio button issues - thanks to @marshallwalker (#461).
* **5.34.0** New: Added Checkstyle 8.26.
* **5.33.1** Fixed: exception on scroll-to-source in newer versions of IDEA (#457).
* **5.33.0** New: Added Checkstyle 8.25. Support has been dropped for Checkstyle < 6.16.1 due to API changes.
* **5.32.0** New: Added Checkstyle 8.24.
* **5.31.0** New: Added Checkstyle 8.23.
* **5.31.0** New: Now built against IDEA 2018.1.8 (was 2017.1.3).
* **5.30.0** New: Quick fix now available to add suppressions for the current rule (#358).
* **5.29.2** Fixed: Wrapped module lookup as read action (#450).
* **5.29.2** Fixed: Parse errors are now better reflected in the log/error messages for static scans (#449).
* **5.29.1** Fixed: ClassCastExceptions thrown from Antlr during parsing were not processed properly (#449).
* **5.29.0** New: Added Checkstyle 8.22.
* **5.28.0** New: Added Checkstyle 8.21.
* **5.27.0** New: Added Checkstyle 8.20.
* **5.26.0** New: Added Checkstyle 8.19.
* **5.26.0** New: Now built against IDEA 2017.1.6 (was 2016.1).
* **5.26.0** Fixed: We no longer strip classpath file references that start with a slash (#437).
* **5.26.0** Fixed: Various UI fixes.
* **5.25.0** New: Added Checkstyle 8.17, 8.18 (#438).
* **5.24.3** Fixed: Blatant white areas in the panel when Darcula in use - thanks to @embee1981 (#432).
* **5.24.2** Fixed: Fixed a synchronisation edge-case (#425).
* **5.24.1** Fixed: Some tweaks to the supported Checkstyle versions for better compatibility - thanks to @tsjensen (#424).
* **5.24.0** Fixed: The plugin should now build & run on JDK 11.
* **5.24.0** Fixed: We now handle exclamation marks in JAR paths (#412).
* **5.24.0** Fixed: Rules files are more consistently cached in memory to hopefully alleviate Windows file-locking issues (#417).
* **5.24.0** Fixed: New version dialogue display is now tracked at application level (#415).
* **5.24.0** New: Several versions of Checkstyle that have no reported compatibility options with newer versions have been dropped so as to reduce plugin size.
* **5.24.0** New: Added Checkstyle 8.14, 8.16 (#420).
* **5.23.0** New: Added Checkstyle 8.13.
* **5.23.0** Fixed: Error highlighting should be better mapped to IDEA settings (#411).
* **5.22.1** New: Parse error messages are now displayed more sensibly (#409).
* **5.22.0** New: Added Checkstyle 8.12.
* **5.21.1** Fixed: Removed project path detokentisation, instead relying on IDEA's built-in support (#404).
* **5.21.0** Fixed: SAME_PACKAGE(n) is now handled when importing code styles (#377)
* **5.21.0** New: Rules files can now be loaded from the Checkstyle classpath (i.e. third-party JARs) (#400).
* **5.20.0** Fixed: Prevents the plugin from crashing when using SuppressionXpathFilter - thanks to @tduehr (#397).
* **5.20.0** New: Added Checkstyle 8.10.1, 8.11.
* **5.19.1** Fixed: Exception when rules file deleted (#396).
* **5.19.0** New: Added Checkstyle 8.9, 8.10.
* **5.18.6** Fixed: adding a new location should now use the Checkstyle version selected in the configuration panel.
* **5.18.5** New: extensions to API to support TestRoots Watchdog (#388).
* **5.18.4** New: added external API class (#388).
* **5.18.3** Fixed: the CustomImportOrder importer now deals with the absence of customImportOrderRules (#387).
* **5.18.2** Fixed: JavadocPackageCheck should work again (#385).
* **5.18.1** Fixed: Exceptions caught by scan action should now appear in the event log (#383).
* **5.18.1** Fixed: Added missing DTDs (#381).
* **5.18.0** New: Added Checkstyle 8.8.
* **5.17.1** Fixed: Moved update tracking to workspace.
* **5.17.0** New: Now built against IDEA 2016.1 (was 15.0.6).
* **5.16.3** New: Improved support for CustomImportOrder (#362). Thanks to Joey Lee (@yeoji).
* **5.16.3** New: Plugin will notify user on update and point at release notes (#373). 
* **5.16.2** Fixed: Violations now include rule name in the static scan only (#371).
* **5.16.1** Fixed: DTDs pointing at sourceforge no longer trigger a live lookup (#280).
* **5.16.0** New: Added CheckStyle 8.6, 8.7.
* **5.16.0** Fixed: Plugin will now work if in the pre-installed plugin dir (#368).
* **5.15.0** New: Added CheckStyle 8.5 (#366).
* **5.14.0** New: Libraries can now be copied, to solve locking issues on Windows (#263). Thanks, yet again, to Thomas Jenson (@tsjenson).
* **5.13.0** New: Project-relative configurations in the default settings aren't loaded until we're in a project (#333).
* **5.13.0** Fixed: HTTP reader now has a 5s timeout (#360).
* **5.13.0** Fixed: More errors should be logged to the event log.
* **5.12.1** Fixed: Class loading issues that broke JavaDoc checks in Android Studio 3 should now be resolved (#352).
* **5.12.0** New: Added CheckStyle 8.4.
* **5.12.0** Fixed: Improved feedback when checker cannot be created.
* **5.11.0** New: Added CheckStyle 8.3.
* **5.10.2** New: Checkstyle version numbers now descend, leaving the most useful versions at the top - thanks to @tsjensen (#351).
* **5.10.2** Fixed: Fully qualified checks with paths are now resolved properly (#349).
* **5.10.2** Fixed: File normalisation was broken on Windows - thanks to @tsjensen (#351).
* **5.10.2** Fixed: Test paths on newer versions of IntelliJ should be corrected detected - thanks to @tsjensen  (#351).
* **5.10.1** Fixed: Version check should now work on Java 9 (#346).
* **5.10.1** Fixed: Modified file results not shown when basedir was configured (#345).
* **5.10.0** New: Added CheckStyle 8.2 (#343).
* **5.9.1** New: Cleaned up patch releases of Checkstyle (#340). Thanks, again, to Thomas Jenson (@tsjenson).
* **5.9.0** New: Added CheckStyle 8.1 (#338).
* **5.8.2** Fixed: Parent of properties dialogue is now correct (#334).
* **5.8.2** Fixed: Parse stream errors now correctly show the root exception (#331).
* **5.8.2** Fixed: Property-only changes are now correctly detected when checking configuration modification state (#331).
* **5.8.2** New: We attempt to resolve rules files as project relative in the configuration dialogue (#333).
* **5.8.1** Fixed: Resolved cyclic dependency issue. Thanks to Thomas Jensen (@tsjensen) (#327).
* **5.8.0** New: Bundled Sun/Google checks are now read from selected version of Checkstyle. Thanks to Thomas Jensen (@tsjensen) (#320).
* **5.7.0** New: Added CheckStyle 8.0.
* **5.7.0** Fixed: short identifiers are used in temporary file names to assist with path limits on Windows.
* **5.6.2** New: code style importer now support avoid star imports - thanks to @zentol (#319).
* **5.6.2** New: temporary files are now created relative to the project base where possible (#321).
* **5.6.1** New: code style importer now supports import order - thanks to @zentol (#314).
* **5.6.1** Fixed: code style importer now adds an extra line between leading content and the package declaration (#315).
* **5.6.1** Fixed: improved housekeeping when Windows projects are not on the system drive (#313).
* **5.6.0** New: Added CheckStyle 7.8.1.
* **5.6.0** Fixed: we now try to use a temporary dir in the project folder when the projects is not on the system drive
  (#302).
* **5.5.1** New: File paths are now trimmed (#308).
* **5.5.0** New: Added CheckStyle 7.7 (#305).
* **5.4.0** New: Added CheckStyle 7.6.1 (#303).
* **5.3.1** Fixed: Paths from Checkstyle are now normalised (#302).
* **5.3.0** New: Added CheckStyle 7.6 (#300).
* **5.3.0** Fixed: Ignored problems are no longer counting for pre-checkin scan (#299).
* **5.2.0** New: Added CheckStyle 7.5.1 (#296).
* **5.2.0** Fixed: Ignored problems are excluded from the inspection results (#287).
* **5.1.4** Fixed: Ignored problems no longer create phantom nodes in the results view (#287).
* **5.1.3** Fixed: Supporting file lookup should be consistent when adding files (#293).
* **5.1.2** Fixed: Property defaults should now work again.
* **5.1.2** Fixed: Logging classes are now included in the local classpath to hopefully avoid oddities such as #294. 
* **5.1.1** Fixed: Improved handling of cached checkers. Thanks to Thomas Jensen (@tsjensen) (#292). 
* **5.1.0** Fixed: Third-party check now work again. Thanks to Thomas Jensen (@tsjensen) (#286). 
* **5.1.0** New: Added CheckStyle 7.5. 
* **5.0.1** Fixed: Improved handling of IOExceptions from rules files (#285).
* **5.0.0** New: You can now choose the version of Checkstyle to use. All credit to Thomas Jensen (@tsjensen) (#281).
* **4.35.0** New: Updated to CheckStyle 7.4.
* **4.35.0** Fixed: Improved error feedback when properties are missing from rules files (#275).
* **4.34.0** New: Updated to CheckStyle 7.3.
* **4.34.0** Fixed: Added (hopefully) a workaround for #278.
* **4.33.0** New: Updated to CheckStyle 7.2.
* **4.32.2** Fixed: Victim location errors are now at debug level.
* **4.32.2** Fixed: The default tab width is now set from the IDEA Java Code Style, rather than defaulting to the
  Checkstyle default.
* **4.32.1** Fixed: `tabWidth` is now read properly, improving element matching for errors (#259, #265). Thanks to Klaus
  Tannenberg (@KTannenberg).
* **4.32.0** New: Errors for which we cannot find a matching element are now displayed at the top of the file (#265).
* **4.32.0** New: Updated to CheckStyle 7.1.1.
* **4.32.0** New: Scan scopes expanded and improved (#268). Thanks to Thomas Jensen (@tsjensen).
* **4.31.1** New: Updated IDEA SDK to 15.0.6 (143.2370.31), which means no more IDEA 14 support.
* **4.31.0** New: Updated to CheckStyle 7.1 (#264).
* **4.30.1** Fixed: StringIndexOutOfBoundsExceptions are now treated as parse errors (#258).
* **4.30.1** Fixed: Style importer no longer errors on missing properties (#256).
* **4.30.0** New: Updated to CheckStyle 7.0.
* **4.29.2** Fixed: Corrected cleanup thread pooling (#239). Thanks to Baron Roberts.
* **4.29.2** Fixed: If the rules file for a configuration is deleted, we continue to show errors. It will now be
  deactivated (#240). Thanks to Victor Alenkov.
* **4.29.1** Fixed: Default property values should now be applied (#237).
* **4.29.0** New: Updated to CheckStyle 6.19.
* **4.29.0** Fixed: Optional suppression files should no longer generate errors (#231).
* **4.28.1** Fixed: IllegalStateExceptions from Checkstyle are now treated as parse exceptions (#228).
* **4.28.0** New: Updated to CheckStyle 6.18.
* **4.28.0** Fixed: Relative Header check filenames are now resolved relative to the project file (#227).
* **4.28.0** New: Allow error selection by pressing Enter key (#226). Thanks to František Hartman.
* **4.27.4** Fixed: errors are now correctly detected when the Checker property 'basedir' is defined (#183).
* **4.27.3** New: the remaining block time is now displayed in the result message.
* **4.27.2** Fixed: the *reload rules files* button now correctly clears the block (#224).
* **4.27.1** Fixed: **basedir** now maps to the directory of the current module file, when available (#223).
* **4.27.0** New: Updated to CheckStyle 6.17.
* **4.26.0** New: Added support for Eclipse-CS predefined variables (#217).
* **4.25.2** Fixed: Rule files should no longer be read every time the active configuration is queried (#212).
* **4.25.2** Fixed: Scan before checkin is now persisted across IDEA restarts (#216).
* **4.25.1** Fixed: Minimum supported build is now IDEA 14 once again.
* **4.25.0** New: Updated to CheckStyle 6.15 - thanks to Thomas Harning (@harningt).
* **4.24.0** New: Added code style importer - thanks to Rustam Vishnyakov (@dyadix).
* **4.23.0** New: Updated to CheckStyle 6.14.1.
* **4.22.2** Fixed: Static scan rewritten to use IDEA's thread pooling and block less on read actions (#11).
* **4.22.1** Fixed: Now treats NPEs and AIOOBEs as parse exceptions (#201, #203).
* **4.22.0** New: Updated to CheckStyle 6.13.
* **4.21.2** Fixed: Fixed thread access issue with module lookup in inspection (#200).
* **4.21.1** Fixed: Now properly swallows parse errors from Checkstyle 6.12.
* **4.21.0** New: Updated to CheckStyle 6.12.1.
* **4.20.1** Fixed: Inspection now checks for cancellation (#192).
* **4.20.1** Fixed: Rules accessed via HTTP no longer use temporary files (#67).
* **4.20.0** New: Updated to CheckStyle 6.11.2 (#189).
* **4.19.1** Fixed: Improved handling of unparsable files (#185).
* **4.19.0** New: Updated to CheckStyle 6.10.1.
* **4.18.1** Fixed: Prefix added to inspection messages (#181).
* **4.18.0** New: Updated to CheckStyle 6.9.
* **4.17.3** Fixed: Temporary suppression of exception in #127 until it's fixed in CheckStyle.
* **4.17.3** Fixed: Started reducing the embarrassing amount of duplicated code.
* **4.17.2** Fixed: Errors are also now entered in the event log, so they can be read after the balloon popup closes.
* **4.17.2** Fixed: Modernised how notifications are raised.
* **4.17.1** Fixed: Java source files that are marked as generated are no longer scanned (#172).
* **4.17.1** Fixed: Properties are properly reloaded after hitting previous in the add file wizard (#170).
* **4.17.0** New: Updated to CheckStyle 6.8.1.
* **4.17.0** Fixed: TreeWalker logs are now suppressed, reducing event log spam when editing files (#169).
* **4.17.0** Fixed: Inspection no longer overrides getShortName as per the IDEA source docs - this has resulted in a
  ShortName change (#173).
* **4.17.0** Fixed: Only files in the project content source are scanned (#172).
* **4.17.0** Fixed: Mirrored JAR files are now used when available (#141).
* **4.17.0** Fixed: Modernised resource bundle usage.
* **4.16.0** New: Updated to CheckStyle 6.7 (#168).
* **4.15.0** New: Updated to CheckStyle 6.6 (#150).
* **4.14.2** Fixed: Cached checkers are now cleaned by a background task (#141).
* **4.14.2** Fixed: Updated icons to more closely resemble current CheckStyle icon.
* **4.14.2** Fixed: File writes are now forced to UTF-8 (#84).
* **4.14.1** Fixed: HTTP rules files should result in fewer temporary files (#149).
* **4.14.1** Fixed: findFile now uses ReadAction (#140).
* **4.14.0** New: Moved to CheckStyle 6.5, which requires Java 7 or above.
* **4.14.0** New: Moved to Java 8. Please make sure IDEA is running on JDK 8. OS X users must use the 14.1 build with
  the bundled JDK.
* **4.14.0** New: As such, we now use the IDEA 14.1 SDK.
* **4.13.2** New: Reverted to IDEA 13 SDK, as when IDEA 14.1 drops with Java 8 for OS X users there's going to be an SDK
  update anyway.
* **4.13.2** Fixed: Third party classes were not available in dialogue editors. (#133).
* **4.13.1** Fixed: Became paranoid about ToolWindow implementation classes to avoid ClassCastExceptions (#131).
* **4.13.0** New: Moved to IDEA 14 SDK.
* **4.12.0** New: Files can now be selected from within JAR files (#125).
* **4.11.2** Fixed: Changed configuration storage to use a sorted map, to stop silly changes to project files.
* **4.11.2** Fixed: Configuration files are now sorted by description in the configuration panel.
* **4.11.2** Fixed: Filename defaults should now be taken into account (#119).
* **4.11.2** Fixed: Cell edits in properties table are now directly committed on action (#119).
* **4.11.1** Fixed: ClassCast on ToolWindow via presumed race condition (#82).
* **4.11.0** New: Updated to CheckStyle 6.1.1 (#124).
* **4.11.0** Fixed: Made DTDs for import_control 1.1 and packages 1.0 available for offline access (#123).
* **4.10.0** New: Updated to CheckStyle 6.1 (#120).
* **4.9.0** New: Updated to CheckStyle 6.0 (#116).
* **4.8.0** New: Updated to CheckStyle 5.9 (#115).
* **4.7.0** New: Updated to CheckStyle 5.8 (#114).
* **4.6.2** Removed commons-logging from plugin classpath (#113).
* **4.6.1** Rules files may now have extension 'checkstyle' (#112).
* **4.6.0** Fixed caching of project relative locations between projects (#106).
* **4.6.0** Moved to IDEA SDK 133.696 (13.0.2).
* **4.5.5** Fixed: Project base directory is no longer permanently cached (#106).
* **4.5.4** Fixed: Null check for view; thanks to Miel Donkers (#104).
* **4.5.3** Fixed: Added range check for line numbers (#100).
* **4.5.2** New: When adding a HTTPS location you can now choose to ignore certificate errors (#99).
* **4.5.2** Fixed: HTTP basic auth is now supported for URLs with a username/password (#93).
* **4.5.2** Fixed: Better handling of NPE case (#97).
* **4.5.1** Fixed: NPE on new project (#97).
* **4.5.0** New: Updated to CheckStyle 5.7 (#95).
* **4.5.0** New: Build is now performed via Gradle and hence should be consistent.
* **4.5.0** Improved: Temporary file paths are now built relative to the module rather than just the package (#92).
* **4.4.3** Fixed: Clash with IDEA's project path encoding (#88).
* **4.4.2** Fixed: NPE when DoubleCheckedLocking file is added via config panel (#86).
* **4.4.2** Fixed: Rules files are now blocked for 60s if they fail on load (#76).
* **4.4.2** New: No default rules file is applied by default (#83).
* **4.4.1** Fixed: Added a work-around for the API not respecting focus for split editors (#78).
* **4.4.1** Fixed: ScanCurrentFile now works from the editor if the tool window has not yet been shown (#82).
* **4.4.1** Fixed: File paths in ImportControl statements are now filtered (#77).
* **4.4.1** Fixed: File choosers now work with Darcula; thanks to Simon Billingsley (#74).
* **4.4.1** Fixed: Use configured third-party classpath when creating a test checker to validate the configuration;
  thanks to Simon Billingsley (#79).
* **4.4** Improved: Adding rules files now validates the file before committing.
* **4.4** Improved: Preferences UI improved.
* **4.4** New: Scan toolwindow now allows quick selection of a configured rules file.
* **4.3.1** Fixed: Error with ToolWindows on project load.
* **4.3** New: IDEA build 129.677 or above is required, due to breaking changes in the API (ProblemDescriptor in
  99b786ddb if you're nosy).
* **4.3** Fixed: Scan option should now appear but once in the check-in dialogue (#68).
* **4.3** Fixed: Better handling of IDEA exceptions during background scanning (#62).
* **4.3** Plug-in modernisation work.
* **4.2** New: There is now a specific option to make rules files project relative (#60).
* **4.2** Fixed: Suppression files accessed via HTTP(S) now work again; thanks to Kristin Young (#61).
* **4.1** New: There is now a configuration option to stop CheckStyle errors being display as IDEA errors (#50).
* **4.1** New: A warning is now displayed for rules files using DoubleCheckedLocking.
* **4.1** Fixed: Case of file prefix should now no longer randomly change (#59).
* **4.0.2** Fixed: Module classpath should only expose classes, not resources (#56).
* **4.0.2** Fixed: IllegalArgumentException sometimes thrown by CheckerFactory.getConfig (#55).
* **4.0.2** Fixed: ToolWindow icon is now 13x13 pixels (#52).
* **4.0.1** Fixed: NPE on Checker lookup (#49).
* **4.0.1** Fixed: Read Access error (#39).
* **4.0** New: You can now specify template CheckStyle configuration in the Default Project (#33).
* **4.0** New: CheckStyle errors will now be marked as errors by the inspection.
* **4.0** New: Updated to IDEA 12 API. . This means we now require IDEA 12.0.1 or above.
* **4.0** Improved: Updated plugin structure to something a little more modern, allowing goodies such as #33.
* **4.0** Fixed: Dependent modules are now probably included when searching for custom exceptions (#42).
* **3.9.1** Fixed: Default rules are invalid with CS5.6.
* **3.9** Fixed: Match on leading project path causes tokenisation silliness (#9).
* **3.9** New: Updated CheckStyle to 5.6.
* **3.8.4** Fixed: IDE error on pre-check-in scan (#39).
* **3.8.3** Fixed: Synchronisation issue introduced by earlier fix (#36).
* **3.8.2** Fixed: Library classes are now included in the Checkstyle classpath (#34).
* **3.8.2** Fixed: Removed actions not specific to the current editor from the popup menu (#31).
* **3.8.2** Fixed: Changed text to match IDEA by removing full-stops (#30).
* **3.8.1** Fixed: Inline regexp header defs no longer cause an exception (#29).
* **3.8** New: An option has been added to scan all (i.e. non-Java) files.
* **3.8** New: A button has been added to scan the current change list.
* **3.8** New: Modules can now be excluded from scans.
* **3.8** New: Add File dialogue now sets the base directory to the project directory.
* **3.8** Improved: RegexpHeader.headerFile is now resolved in the same manner as the suppression file.
* **3.8** Improved: Rewrote file tokenisation to hopefully help with #9.
* **3.7** New: Updated CheckStyle to 5.5.
* **3.7** New: Added button to trigger rules refresh (#25).
* **3.7** Fixed: Assertion errors from IDEA are now ignored during Inspections executions (#22).
* **3.7** Fixed: Scan on check-in broken in IDEA 11 (#24).
* **3.6.2** Improved: Scan results are now sorted alphabetically.
* **3.6.2** Fixed: Duplicate errors reported during project scans (#16).
* **3.6.2** Fixed: Edited rule file properties are now properly saved.
* **3.6.1** Fixed: Deadlock when trying to access editor information with IDEA 11.
* **3.6** New: Changed to work with IDEA 11. Thanks to Yuri.
* **3.5** New: Updated CheckStyle to 5.4.
* **3.5** Fixed: NPE during property handling.
* **3.5** Fixed: Property change detection improved.
* **3.5** Improved: General cleanup and deprecation removals.
* **3.4.1** Fixed: Intermittent NPE on check-in handler registration.
* **3.4** New: Support for IDEA 10.5. Older versions are no longer supported.
* **3.3.2** Fixed: Relative paths for suppression filters were not working.
* **3.3.1** Fixed: StringIndexOutOfBoundsException when creating temporary files under Windows.
* **3.3** Improved: Suppression filters now work on package paths.
* **3.3** Improved: Closed and saved files are no longer copied to temporary files.
* **3.2** New: Updated CheckStyle to 5.3.
* **3.2** Improved: Temporary files now use original filename. Thanks to Benjy W.
* **3.2** Improved: Removed deprecated calls to IDEA SDK. This may break compatibility with older versions, but as the
  SDK isn't annotated with @since, who knows?
* **3.2** Improved: Introduced setup thread to project scans to improve responsiveness.
* **3.1.2** Fixed: Property names trimmed.
* **3.1.2** Fixed: Cache is now invalidated when settings are changed.
* **3.1.2** Fixed: Files downloaded via HTTP are now scheduled for deletion of JVM termination.
* **3.1.1** Fixed: NPE on notifications.
* **3.1** New: Upgraded to CheckStyle 5.1.
* **3.1** Improved: Suppression files are now searched for relative to the project if they are not present relative to
  the config file.
* **3.1** Improved: If a suppression file is not found then it will be ignored and the user will be warned.
* **3.1** Fixed: Concurrency issues. Thanks to Gerhard Radatz.
* **3.0.13** Fixed: File location fixes galore! Thanks to Gerhard Radatz.
* **3.0.13** Fixed: Removing active configuration no longer causes an exception. Thanks to Gerhard Radatz.
* **3.0.12** Fixed: Inspection ID now correctly conforms to rules. Apologies if this breaks inspection config.
* **3.0.11** Improved: Module/Project scans are properly batched and a lot faster. Progress feedback is more limited,
  however.
* **3.0.10** Fixed: Partial property detection. Thanks to LightGuard.JP.
* **3.0.9** Fixed: Cache synchronisation problem.
* **3.0.8** Fixed: Null property in configuration hash-table resulting in NPE.
* **3.0.7** Fixed: Module scan results are no longer shared between files.
* **3.0.6** Fixed: IDEA 9 compatibility.
* **3.0.5** Fixed: Filters (e.g. SuppressionCommentFilter) now work correctly.
* **3.0.5** Fixed: JavadocPackage now respects allowLegacy. Thanks to Edward Campbell.
* **3.0.4** Fixed: CheckStyle DTD 1.3 added. Thanks to Jonas Bergvall.
* **3.0.3** Fixed: Removed dependency on Apache commons-lang to restore compatibility with IDEA 7.0.x/Linux.
* **3.0.2** Fixed: File configuration column headings were incorrect.
* **3.0.1** Fixed: Rule files added with no description cause an exception.
* **3.0** New: Module-specific configuration support.
* **3.0** New: CheckStyle 5.0 support. Thanks to jicken. Note that CheckStyle 5.0 is not entirely backwards compatible.
  A quick guide to many of the changes can be found here: http://checkstyle.sourceforge.net/releasenotes.html
* **3.0** New: Result list may be filtered on severity.
* **3.0** New: Back-end re-written to support multiple CheckStyle files. You will need to set your configuration once
  again I'm afraid.
* **2.4** New: Default CheckStyle file now has generics patch applied.
* **2.4** New: Scans in progress may be aborted.
* **2.4** Fixed: Null Pointer Exception with package HTML check.
* **2.4** Fixed: Null Pointer Exception when files have no module.
* **2.4** Fixed: Background scan thread does not wait on event thread.
* **2.3** Fixed: Property values are not lost on configuration commit.
* **2.3** Fixed: Exceptions in JAR files are not picked up.
* **2.3** Fixed: Scan Modified Files is now disabled while other scans are active.
* **2.3** Fixed: Background scan thread is now lower priority.
* **2.3** Fixed: Module scan now works again.
* **2.2** Fixed: Now works with IDEA8.0.
* **2.2** New: Ability to scan on check-in. Thanks to J. G. Christopher.
* **2.2** Experimental: Ability to load CheckStyle configuration from a URL. This is NOT robust at present.
* **2.1** New: Ability to scan only modified files if project uses VCS. If the project does not use VCS then this action
  will have no effect. Thanks to J. G. Christopher for this patch.
* **2.1** New: Ability to suppress checks on test classes.
* **2.1** New: Updated CheckStyle to 4.4.
* **2.1** Fixed: Suppression filters with relative paths now load correctly.
* **2.1** Fixed: NewlineAtEndOfFile now works on Windows.
* **2.1** Fixed: PackageHtml test will now work.
* **2.0.1** WARNING: Due to persistence changes you will need to reconfigure the plug-in after upgrading.
* **2.0.1** Fixed: Configuration dialogue now enables 'apply' when moving to default scheme from a configuration file.
* **2.0.1** Changed: Remove deprecated API calls to [hopefully] ensure forward 7.0 compatibility.
* **2.0.1** Changed: Icons improved.
* **2.0.0** This version offers no new features. However, 2.x versions will only work with IDEA 7M2 or above.
* **2.0.0** Fixed: API changes for IDEA 7M2.
* **1.0.2** Improved: Error handling
* **1.0.2** Fixed: Exception when settings opened while no project loaded.
* **1.0.2** Fixed: Exception if inspection cancelled during results processing.
* **1.0.2** Fixed: Exception if configuration panel opened while puppycrawl.com could not be accessed.
* **1.0.1** Fixed: Potentially IllegalState if project path cannot be retrieved within IDEA 7M1.
* **1.0** New: Static scanning for current module implemented.
* **1.0** New: Ability to add third-party checks.
* **1.0** New: Ability to define external properties.
* **1.0** New: Now works with IDEA 7M1.
* **1.0** Fixed: Exceptions defined in project will no longer generate a CheckStyle error once they have been compiled.
* **0.5.2** New: Settings are now stored in project, not workspace.
* **0.5.1** Fixed: NullPointer when a null PSI element is encountered during scanning.
* **0.5** Improved: Localisation support.
* **0.5** Improved: Config file locations under the project root are now stored as relative rather than the
  system-specific absolute path.
* **0.5** New: CheckStyle icons added.
* **0.4** New: Added static scanning support.
* **0.4** New: Updated CheckStyle to 4.3.
* **0.3** Improved: CheckStyle caching improved.
* **0.3** Improved: Temporary file deletion.
* **0.2** Fix: File browser for configuration file filters out directories.
* **0.1** Initial release.
