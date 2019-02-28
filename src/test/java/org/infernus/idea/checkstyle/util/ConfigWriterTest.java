package org.infernus.idea.checkstyle.util;

import org.infernus.idea.checkstyle.model.XMLConfig;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;

import static org.junit.Assert.*;

public class ConfigWriterTest {
  private XMLConfig plainConfig;
  private static String easyConfigName = "easy.xml";
  private static String easyConfigPreviewName = "easyPreview.xml";
  private static String complicatedConfigName = "isIsComplicated.xml";
  private static String complicatedConfigPreviewName = "complicatedPreview.xml";
  private static String[] fileList = new String[]{
          easyConfigName,
          complicatedConfigName,
          easyConfigPreviewName,
          complicatedConfigPreviewName
  };

  @Before
  public void setUp() {
    this.plainConfig = new XMLConfig("Checker");
  }

  @AfterClass
  public static void cleanUp() {
    for (String fileName : fileList) {
      File f = new File(fileName);
      f.delete();
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void ConfigWriterSaveConfigNoRootCheckerTest() throws Exception {
    XMLConfig notCheckerRoot = new XMLConfig("Not Checker");

    ConfigWriter.saveConfig("dummy/dummy.xml", notCheckerRoot);
  }

  @Test(expected = IllegalArgumentException.class)
  public void ConfigWriterSaveConfigNotSaveToXMLTest() throws Exception {
    ConfigWriter.saveConfig("notxml.txt", this.plainConfig);
  }

  @Test(expected = IllegalArgumentException.class)
  public void ConfigWriterSaveConfigSaveToNonExistDirTest() throws Exception {
    ConfigWriter.saveConfig("NotExistDir/someconfig.xml", this.plainConfig);
  }

  @Test
  public void ConfigWriterSaveConfigSaveEasyConfigTest() throws Exception {
    XMLConfig module1 = new XMLConfig("RegexpMultiline");
    this.plainConfig.addAttribute("cacheFile", "${checkstyle.cache.file}");
    this.plainConfig.addAttribute("severity", "error");
    this.plainConfig.addMessage("regexp.filepath.mismatch",
            "Only java files should be located in the ''src/*/java'' folders.");

    this.plainConfig.addChild(module1);

    ConfigWriter.saveConfig(easyConfigName, this.plainConfig);

    XMLConfig read = ConfigReader.readConfig(easyConfigName);

    assertEquals("Checker", read.getName());

    assertEquals(2, read.getAttributeNames().length);
    assertEquals(1, read.getChildren().length);
    assertEquals(1, read.getMessages().size());

    assertEquals("${checkstyle.cache.file}", read.getAttribute("cacheFile"));
    assertEquals("error", read.getAttribute("severity"));

    assertEquals("Only java files should be located in the ''src/*/java'' folders.",
            read.getMessages().get("regexp.filepath.mismatch"));

    assertEquals("RegexpMultiline", read.getChildren()[0].getName());
  }

  @Test
  public void ConfigWriterSaveConfigSaveComplicateConfigTest() throws Exception {
    XMLConfig SuppressWarningsFilter = new XMLConfig("SuppressWarningsFilter");
    XMLConfig BeforeExecutionExclusionFileFilter = new XMLConfig("BeforeExecutionExclusionFileFilter");
    XMLConfig SuppressWarnings = new XMLConfig("SuppressWarnings");
    XMLConfig TreeWalker = new XMLConfig("TreeWalker");
    XMLConfig AnnotationLocation = new XMLConfig("AnnotationLocation");

    this.plainConfig.addAttribute("cacheFile", "${checkstyle.cache.file}");

    // SuppressWarningsFilter
    this.plainConfig.addChild(SuppressWarningsFilter);

    // BeforeExecutionExclusionFileFilter
    BeforeExecutionExclusionFileFilter.addAttribute("fileNamePattern", "module\\-info\\.java$");
    this.plainConfig.addChild(BeforeExecutionExclusionFileFilter);

    // SuppressWarnings
    SuppressWarnings.addAttribute("format", "^((?!unchecked|deprecation|rawtypes|resource).)*$");
    SuppressWarnings.addMessage("regexp.filepath.mismatch",
            "Only java files should be located in the ''src/*/java'' folders.");
    this.plainConfig.addChild(SuppressWarnings);

    // AnnotationLocation
    AnnotationLocation.addAttribute("tokens", "PARAMETER_DEF");
    AnnotationLocation.addAttribute("allowSamelineMultipleAnnotations", "true");
    TreeWalker.addChild(AnnotationLocation);

    // TreeWalker
    this.plainConfig.addChild(TreeWalker);

    ConfigWriter.saveConfig(complicatedConfigName, this.plainConfig);

    XMLConfig read = ConfigReader.readConfig(complicatedConfigName);

    assertEquals("Checker", read.getName());
    assertEquals(1, read.getAttributeNames().length);
    assertEquals(4, read.getChildren().length);
    assertEquals(0, read.getMessages().size());

    assertEquals("${checkstyle.cache.file}", read.getAttribute("cacheFile"));

    XMLConfig[] child = read.getChildren();
    assertEquals(4, child.length);

    for (int i = 0; i < child.length; i++) {
      if (child[i].getName().equals("SuppressWarningsFilter")) {
        assertEquals(0, child[i].getAttributeNames().length);
        assertEquals(0, child[i].getChildren().length);
        assertEquals(0, child[i].getMessages().size());
      } else if (child[i].getName().equals("BeforeExecutionExclusionFileFilter")) {
        assertEquals(1, child[i].getAttributeNames().length);
        assertEquals(0, child[i].getChildren().length);
        assertEquals(0, child[i].getMessages().size());

        assertEquals("module\\-info\\.java$", child[i].getAttribute("fileNamePattern"));
      } else if (child[i].getName().equals("SuppressWarnings")) {
        assertEquals(1, child[i].getAttributeNames().length);
        assertEquals(0, child[i].getChildren().length);
        assertEquals(1, child[i].getMessages().size());

        assertEquals("^((?!unchecked|deprecation|rawtypes|resource).)*$", child[i].getAttribute("format"));
        assertEquals("Only java files should be located in the ''src/*/java'' folders.",
                child[i].getMessages().get("regexp.filepath.mismatch"));
      } else if (child[i].getName().equals("TreeWalker")) {
        assertEquals(0, child[i].getAttributeNames().length);
        assertEquals(1, child[i].getChildren().length);
        assertEquals(0, child[i].getMessages().size());

        XMLConfig grandChild = child[i].getChildren()[0];
        assertEquals(2, grandChild.getAttributeNames().length);
        assertEquals(0, grandChild.getChildren().length);
        assertEquals(0, grandChild.getMessages().size());

        assertEquals("PARAMETER_DEF", grandChild.getAttribute("tokens"));
        assertEquals("true", grandChild.getAttribute("allowSamelineMultipleAnnotations"));
      } else {
        assertTrue("unexpected child config " + child[i].getName(), false);
      }
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void ConfigWriterXMLPreviewTest() throws Exception {
    XMLConfig notCheck = new XMLConfig("NotChecker");

    ConfigWriter.xmlPreview(notCheck);
  }

  @Test
  public void ConfigWriterXMLPreviewEasyXMLTest() throws Exception {
    XMLConfig module1 = new XMLConfig("RegexpMultiline");
    this.plainConfig.addAttribute("cacheFile", "${checkstyle.cache.file}");
    this.plainConfig.addAttribute("severity", "error");
    this.plainConfig.addMessage("regexp.filepath.mismatch",
            "Only java files should be located in the ''src/*/java'' folders.");

    this.plainConfig.addChild(module1);

    String previewString = ConfigWriter.xmlPreview(this.plainConfig);

    PrintWriter out = new PrintWriter(easyConfigPreviewName);

    out.println(previewString);
    out.flush();
    out.close();

    XMLConfig read = ConfigReader.readConfig(easyConfigPreviewName);

    assertEquals("Checker", read.getName());

    assertEquals(2, read.getAttributeNames().length);
    assertEquals(1, read.getChildren().length);
    assertEquals(1, read.getMessages().size());

    assertEquals("${checkstyle.cache.file}", read.getAttribute("cacheFile"));
    assertEquals("error", read.getAttribute("severity"));

    assertEquals("Only java files should be located in the ''src/*/java'' folders.",
            read.getMessages().get("regexp.filepath.mismatch"));

    assertEquals("RegexpMultiline", read.getChildren()[0].getName());
  }

  @Test
  public void ConfigWriterXMLPreviewComplicatedXMLTest() throws Exception {
    XMLConfig SuppressWarningsFilter = new XMLConfig("SuppressWarningsFilter");
    XMLConfig BeforeExecutionExclusionFileFilter = new XMLConfig("BeforeExecutionExclusionFileFilter");
    XMLConfig SuppressWarnings = new XMLConfig("SuppressWarnings");
    XMLConfig TreeWalker = new XMLConfig("TreeWalker");
    XMLConfig AnnotationLocation = new XMLConfig("AnnotationLocation");

    this.plainConfig.addAttribute("cacheFile", "${checkstyle.cache.file}");

    // SuppressWarningsFilter
    this.plainConfig.addChild(SuppressWarningsFilter);

    // BeforeExecutionExclusionFileFilter
    BeforeExecutionExclusionFileFilter.addAttribute("fileNamePattern", "module\\-info\\.java$");
    this.plainConfig.addChild(BeforeExecutionExclusionFileFilter);

    // SuppressWarnings
    SuppressWarnings.addAttribute("format", "^((?!unchecked|deprecation|rawtypes|resource).)*$");
    SuppressWarnings.addMessage("regexp.filepath.mismatch",
            "Only java files should be located in the ''src/*/java'' folders.");
    this.plainConfig.addChild(SuppressWarnings);

    // AnnotationLocation
    AnnotationLocation.addAttribute("tokens", "PARAMETER_DEF");
    AnnotationLocation.addAttribute("allowSamelineMultipleAnnotations", "true");
    TreeWalker.addChild(AnnotationLocation);

    // TreeWalker
    this.plainConfig.addChild(TreeWalker);

    String previewString = ConfigWriter.xmlPreview(this.plainConfig);

    PrintWriter out = new PrintWriter(complicatedConfigPreviewName);

    out.println(previewString);
    out.flush();
    out.close();

    XMLConfig read = ConfigReader.readConfig(complicatedConfigPreviewName);

    assertEquals("Checker", read.getName());
    assertEquals(1, read.getAttributeNames().length);
    assertEquals(4, read.getChildren().length);
    assertEquals(0, read.getMessages().size());

    assertEquals("${checkstyle.cache.file}", read.getAttribute("cacheFile"));

    XMLConfig[] child = read.getChildren();
    assertEquals(4, child.length);

    for (int i = 0; i < child.length; i++) {
      if (child[i].getName().equals("SuppressWarningsFilter")) {
        assertEquals(0, child[i].getAttributeNames().length);
        assertEquals(0, child[i].getChildren().length);
        assertEquals(0, child[i].getMessages().size());
      } else if (child[i].getName().equals("BeforeExecutionExclusionFileFilter")) {
        assertEquals(1, child[i].getAttributeNames().length);
        assertEquals(0, child[i].getChildren().length);
        assertEquals(0, child[i].getMessages().size());

        assertEquals("module\\-info\\.java$", child[i].getAttribute("fileNamePattern"));
      } else if (child[i].getName().equals("SuppressWarnings")) {
        assertEquals(1, child[i].getAttributeNames().length);
        assertEquals(0, child[i].getChildren().length);
        assertEquals(1, child[i].getMessages().size());

        assertEquals("^((?!unchecked|deprecation|rawtypes|resource).)*$", child[i].getAttribute("format"));
        assertEquals("Only java files should be located in the ''src/*/java'' folders.",
                child[i].getMessages().get("regexp.filepath.mismatch"));
      } else if (child[i].getName().equals("TreeWalker")) {
        assertEquals(0, child[i].getAttributeNames().length);
        assertEquals(1, child[i].getChildren().length);
        assertEquals(0, child[i].getMessages().size());

        XMLConfig grandChild = child[i].getChildren()[0];
        assertEquals(2, grandChild.getAttributeNames().length);
        assertEquals(0, grandChild.getChildren().length);
        assertEquals(0, grandChild.getMessages().size());

        assertEquals("PARAMETER_DEF", grandChild.getAttribute("tokens"));
        assertEquals("true", grandChild.getAttribute("allowSamelineMultipleAnnotations"));
      } else {
        assertTrue("unexpected child config " + child[i].getName(), false);
      }
    }
  }
}