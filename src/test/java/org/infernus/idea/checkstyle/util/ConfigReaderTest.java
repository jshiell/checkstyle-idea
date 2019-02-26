package org.infernus.idea.checkstyle.util;

import org.infernus.idea.checkstyle.model.XMLConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ConfigReaderTest {
  private static List<File> createdFile = new ArrayList<File>();
  private static String isNotXMLFileName = "isXML.x1ml";
  private static String parseErrorXMLName = "parseError.xml";
  private static String invalidConfigName1 = "invalidConfig1.xml";
  private static String invalidConfigName2 = "invalidConfig2.xml";
  private static String invalidConfigName3 = "invalidConfig3.xml";
  private static String ignoreDTD = "ignoreDTD.xml";
  private static String onlyProperties = "onlyProperties.xml";
  private static String onlyModules = "onlyModule.xml";
  private static String haveEverything = "haveEverything.xml";

  @BeforeClass
  public static void setUpTestingFiles() throws Exception{
    File isXMLFile = new File(isNotXMLFileName);
    File parseErrorFile = new File(parseErrorXMLName);
    File invalidConfigFile1 = new File(invalidConfigName1);
    File invalidConfigFile2 = new File(invalidConfigName2);
    File invalidConfigFile3 = new File(invalidConfigName3);
    File ignoreDTDFile = new File(ignoreDTD);
    File onlyPropertiesFile = new File(onlyProperties);
    File onlyModuleFile = new File(onlyModules);
    File haveEverythingFile = new File(haveEverything);

    isXMLFile.createNewFile();
    parseErrorFile.createNewFile();
    invalidConfigFile1.createNewFile();
    invalidConfigFile2.createNewFile();
    invalidConfigFile3.createNewFile();
    ignoreDTDFile.createNewFile();
    onlyPropertiesFile.createNewFile();
    onlyModuleFile.createNewFile();
    haveEverythingFile.createNewFile();

    createdFile.add(isXMLFile);
    createdFile.add(parseErrorFile);
    createdFile.add(invalidConfigFile1);
    createdFile.add(invalidConfigFile2);
    createdFile.add(invalidConfigFile3);
    createdFile.add(ignoreDTDFile);
    createdFile.add(onlyPropertiesFile);
    createdFile.add(onlyModuleFile);
    createdFile.add(haveEverythingFile);

    // content of parseErrorFile
    PrintWriter parseErrorWriter = new PrintWriter(parseErrorFile);
    parseErrorWriter.println("<unclosed>");
    parseErrorWriter.flush();
    parseErrorWriter.close();

    PrintWriter invalidConfigWriter;
    // content of invalidConfigFile1
    invalidConfigWriter = new PrintWriter(invalidConfigFile1);
    invalidConfigWriter.println("<notmodule name=\"Checker\">\n</notmodule>");
    invalidConfigWriter.flush();
    invalidConfigWriter.close();

    // content of invalidConfigFile2
    invalidConfigWriter = new PrintWriter(invalidConfigFile2);
    invalidConfigWriter.println("<module name=\"Notchecker\"></module>");
    invalidConfigWriter.flush();
    invalidConfigWriter.close();

    // content of invalidConfigFile3
    invalidConfigWriter = new PrintWriter(invalidConfigFile3);
    invalidConfigWriter.println("<module></module>");
    invalidConfigWriter.flush();
    invalidConfigWriter.close();

    // contest of ignoreDTDFile
    PrintWriter ignoreDTDWriter = new PrintWriter(ignoreDTDFile);
    ignoreDTDWriter.println("<?xml version=\"1.0\"?>");
    ignoreDTDWriter.println("<!DOCTYPE module PUBLIC\n" +
            "          \"-//Checkstyle//DTD Checkstyle Configuration 1.3//EN\"\n" +
            "          \"https://checkstyle.org/dtds/configuration_1_3.dtd\">");
    ignoreDTDWriter.println("<module name=\"Checker\"></module>");
    ignoreDTDWriter.flush();
    ignoreDTDWriter.close();

    // content of onlyPropertiesFile
    PrintWriter onlyPropertiesWriter = new PrintWriter(onlyPropertiesFile);
    onlyPropertiesWriter.println("<?xml version=\"1.0\"?>");
    onlyPropertiesWriter.println("<!DOCTYPE module PUBLIC\n" +
            "          \"-//Checkstyle//DTD Checkstyle Configuration 1.3//EN\"\n" +
            "          \"https://checkstyle.org/dtds/configuration_1_3.dtd\">");
    onlyPropertiesWriter.println("<module name=\"Checker\">");
    onlyPropertiesWriter.println("  <property name=\"cacheFile\" value=\"${checkstyle.cache.file}\"/>");
    onlyPropertiesWriter.println("  <property name=\"severity\" value=\"error\"/>");
    onlyPropertiesWriter.println("  <property name=\"fileExtensions\" value=\"java, properties, xml, vm, g, g4, dtd\"/>");
    onlyPropertiesWriter.println("</module>");
    onlyPropertiesWriter.flush();
    onlyPropertiesWriter.close();

    // content of onlyModuleFile
    PrintWriter onlyModuleWriter = new PrintWriter(onlyModuleFile);
    onlyModuleWriter.println("<?xml version=\"1.0\"?>");
    onlyModuleWriter.println("<!DOCTYPE module PUBLIC\n" +
            "          \"-//Checkstyle//DTD Checkstyle Configuration 1.3//EN\"\n" +
            "          \"https://checkstyle.org/dtds/configuration_1_3.dtd\">");
    onlyModuleWriter.println("<module name=\"Checker\">");
    onlyModuleWriter.println("  <module name=\"SuppressWarningsFilter\"/>");
    onlyModuleWriter.println("  <module name=\"UniqueProperties\"/>");
    onlyModuleWriter.println("</module>");
    onlyModuleWriter.flush();
    onlyModuleWriter.close();

    // content of haveEverything
    PrintWriter haveEverythingWriter = new PrintWriter(haveEverythingFile);
    haveEverythingWriter.println("<?xml version=\"1.0\"?>");
    haveEverythingWriter.println("<!DOCTYPE module PUBLIC\n" +
            "          \"-//Checkstyle//DTD Checkstyle Configuration 1.3//EN\"\n" +
            "          \"https://checkstyle.org/dtds/configuration_1_3.dtd\">");
    haveEverythingWriter.println("<module name=\"Checker\">");
    haveEverythingWriter.println("  <property name=\"cacheFile\" value=\"${checkstyle.cache.file}\"/>");
    haveEverythingWriter.println("  <module name=\"SuppressWarningsFilter\"/>");
    haveEverythingWriter.println("  <module name=\"BeforeExecutionExclusionFileFilter\">");
    haveEverythingWriter.println("    <property name=\"fileNamePattern\" value=\"module\\-info\\.java$\" />");
    haveEverythingWriter.println("  </module>");
    haveEverythingWriter.println("  <module name=\"SuppressWarnings\">\n" +
            "        <property name=\"format\" value=\"^((?!unchecked|deprecation|rawtypes|resource).)*$\"/>\n" +
            "        <message key=\"regexp.filepath.mismatch\"\n" +
            "               value=\"Only java files should be located in the ''src/*/java'' folders.\"/>" +
            "    </module>");
    haveEverythingWriter.println("  <module name=\"TreeWalker\">");
    haveEverythingWriter.println("    <module name=\"AnnotationLocation\">\n" +
            "      <property name=\"tokens\" value=\"PARAMETER_DEF\"/>\n" +
            "      <property name=\"allowSamelineMultipleAnnotations\" value=\"true\"/>\n" +
            "    </module>");
    haveEverythingWriter.println("  </module>");
    haveEverythingWriter.println("</module>");
    haveEverythingWriter.flush();
    haveEverythingWriter.close();
  }


  @AfterClass
  public static void cleanUpFiles() {
    for (File f : createdFile) {
      f.delete();
    }
  }

  @Test(expected = FileNotFoundException.class)
  public void ConfigReaderReadConfigFileNotExistTest() throws Exception {
    ConfigReader.readConfig("definetely not a file");
  }

  @Test(expected = IllegalArgumentException.class)
  public void ConfigReaderReadConfigNotXMLTest() throws Exception {
    ConfigReader.readConfig(isNotXMLFileName);
  }

  @Test(expected = SAXException.class)
  public void ConfigReaderReadConfigInvalidXMLTest() throws Exception {
    ConfigReader.readConfig(parseErrorXMLName);
  }

  @Test(expected = IllegalArgumentException.class)
  public void ConfigReaderReadConfigWrongRootTagTest() throws Exception {
    ConfigReader.readConfig(invalidConfigName1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void ConfigReaderReadConfigWrongNameTest() throws Exception {
    ConfigReader.readConfig(invalidConfigName2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void ConfigReaderReadConfigNoNameTest() throws Exception {
    ConfigReader.readConfig(invalidConfigName3);
  }

  @Test
  public void ConfigReaderReadConfigIgnoreDTDTest() throws Exception {
    ConfigReader.readConfig(ignoreDTD);
  }

  @Test
  public void ConfigReaderReadConfigOnlyPropertiesXMLTest() throws Exception {
    XMLConfig result = ConfigReader.readConfig(onlyProperties);

    assertEquals("Checker", result.getName());
    assertEquals(3, result.getAttributeNames().length);
    assertEquals(0, result.getChildren().length);
    assertEquals(0, result.getMessages().size());

    assertEquals("${checkstyle.cache.file}", result.getAttribute("cacheFile"));
    assertEquals("error", result.getAttribute("severity"));
    assertEquals("java, properties, xml, vm, g, g4, dtd", result.getAttribute("fileExtensions"));
  }

  @Test
  public void ConfigReaderReadConfigOnlyModuleXMLTest() throws Exception {
    XMLConfig result = ConfigReader.readConfig(onlyModules);

    assertEquals("Checker", result.getName());
    assertEquals(0, result.getAttributeNames().length);
    assertEquals(2, result.getChildren().length);
    assertEquals(0, result.getMessages().size());
  }

  @Test
  public void ConfigReaderReadConfigHaveEverythingXMLTest() throws Exception {
    XMLConfig result = ConfigReader.readConfig(haveEverything);

    assertEquals("Checker", result.getName());
    assertEquals(1, result.getAttributeNames().length);
    assertEquals(4, result.getChildren().length);
    assertEquals(0, result.getMessages().size());

    assertEquals("${checkstyle.cache.file}", result.getAttribute("cacheFile"));

    XMLConfig[] child = result.getChildren();
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