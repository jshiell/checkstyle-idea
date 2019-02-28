package org.infernus.idea.checkstyle.util;

import org.infernus.idea.checkstyle.model.XMLConfig;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Writes user define configuration to XML.
 * */
public class ConfigWriter {

  /**
   * Save user-defined configuration to path.
   * @param path - The path to save the user-defined configuration
   * @param config - user-defined configurations, with module “Checker” as root
   * @throws IllegalArgumentException - When the root module is not name "Checker"
   * @throws IllegalArgumentException - When the path is not saving to XML
   * @throws IllegalArgumentException - When the parent directory doesn't exist
   * @throws IOException - When file woulc not be created with the path
   * */
  public static void saveConfig(String path, XMLConfig config) throws IllegalArgumentException,
          IOException {
    File configFile = new File(path);
    if (!Pattern.matches(".+\\.xml$", path)) {
      throw new IllegalArgumentException("Not saving as XML file");
    }

    if (!configFile.exists()) {
      File parentDir = configFile.getParentFile();
      if (parentDir != null && !parentDir.exists()) {
        throw new IllegalArgumentException("Invalid directory");
      }
    }

    if (!config.getName().equals("Checker")) {
      throw new IllegalArgumentException("Root module not Checker");
    }

    try {
      // make XMLConfig to dom format
      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      Document output = docBuilder.newDocument();
      Element root = deepCopy(config, output);

      output.appendChild(root);

      // output to XML
      try {
        if (!configFile.exists()) {
          // create file if it is not already exist
          configFile.createNewFile();
        }

        FileOutputStream fos = new FileOutputStream(configFile);

        xmlOutput(output, new StreamResult(fos));

        fos.close(); // close the stream so other programs won't be bothered

      } catch (TransformerConfigurationException e) {
        System.out.println(e.getMessage());
        configFile.delete();
      } catch (TransformerException e) {
        System.out.println(e.getMessage());
        configFile.delete();
      }
    } catch (ParserConfigurationException e) {
      System.out.println(e.getMessage());
    }
  }

  /**
   * Outputs the String result of the config in XML format
   * @param config - The configuration to convert to XML String
   * @return The XML format of config.
   * @throws IllegalArgumentException - When root module is not Checker
   */
  public static String xmlPreview(XMLConfig config) throws IllegalArgumentException {
    if (!config.getName().equals("Checker")) {
      throw new IllegalArgumentException("root module not Checker");
    }

    // make XMLConfig to dom format
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = null;

    try {
      docBuilder = docBuilderFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      System.out.println(e.getMessage());
    }

    Document output = docBuilder.newDocument();
    Element root = deepCopy(config, output);

    output.appendChild(root);

    StringWriter previewWriter = new StringWriter();

    try {
      xmlOutput(output, new StreamResult(previewWriter));
    } catch (TransformerException e) {
      System.out.println(e.getMessage());
    }

    return previewWriter.getBuffer().toString();
  }

  /**
   * A helper function that converts XMLConfig into correct dom structure.
   * @param dom - The XMLConfig to convert from.
   * @param doc - The tool to generate Element
   * @return A Element that represents dom
   * */
  private static Element deepCopy(XMLConfig dom, Document doc) {
    Element output = doc.createElement("module");
    output.setAttribute("name", dom.getName());

    // deal with properties
    String[] propertyNames = dom.getAttributeNames();
    for (int i = 0; i < propertyNames.length; i++) {
      Element property = doc.createElement("property");
      property.setAttribute("name", propertyNames[i]);
      property.setAttribute("value", dom.getAttribute(propertyNames[i]));

      output.appendChild(property);
    }

    // deal with message
    Map<String, String> msgs = dom.getMessages();
    for (String key : msgs.keySet()) {
      Element msg = doc.createElement("message");

      msg.setAttribute("key", key);
      msg.setAttribute("value", msgs.get(key));

      output.appendChild(msg);
    }

    // deal with child modules
    XMLConfig[] children = dom.getChildren();
    for (int i = 0; i < children.length; i++) {
      output.appendChild(deepCopy(children[i], doc));
    }

    return output;
  }

  /**
   * Helper function to factor out the output process.
   * @param doc - The document that contains the XML structure
   * @param result - The StreamResult for Transformer to write data to.
   *                 Caller is responsible to close the contained steam.
   * @throws TransformerException - When transformer error out
   */
  private static void xmlOutput(Document doc, StreamResult result)
          throws TransformerException {
    TransformerFactory trf = TransformerFactory.newInstance();
    Transformer tr = trf.newTransformer();

    DOMImplementation impl = doc.getImplementation();
    DocumentType doctype = impl.createDocumentType( "doctype",
            "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN",
            "https://checkstyle.org/dtds/configuration_1_3.dtd" );

    tr.setOutputProperty(OutputKeys.INDENT, "yes");
    tr.setOutputProperty(OutputKeys.METHOD, "xml");
    tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    // both setup public and system doctype has to be there to make transformer
    // print !DOCTYPE
    tr.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
    tr.setOutputProperty( OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());

    tr.transform(new DOMSource(doc), result);

  }
}
