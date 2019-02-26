package org.infernus.idea.checkstyle.util;

import org.infernus.idea.checkstyle.model.XMLConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Pattern;


/**
 * Reads the XML configuration from rules
 * */
public class ConfigReader {

  /**
   * Reads an existing checkstyle configuration XML in path.
   * @param path - The file path to the XML file.
   * @return a set of configuration(modules) with module “Checker” as root
   * @throws FileNotFoundException - When the passed in file doesn’t exist.
   * @throws IllegalArgumentException - When the passed in file is not XML,
   *         or doesn’t have “Checker” as root module.
   * @throws ParserConfigurationException - DocumentFactory config error, please
   *         report when this error is thrown
   * @throws SAXException - When parsing error occur
   * @throws IOException - If any IO errors occur.
   * */
  public static XMLConfig readConfig(String path) throws IOException,
          IllegalArgumentException,
          ParserConfigurationException,
          SAXException {
    File configFile = new File(path);

    if (!configFile.exists()) {
      throw new FileNotFoundException(path + " does not exist");
    }

    if (!Pattern.matches(".+\\.xml$", path)) {
      throw new IllegalArgumentException(path + "is not a xml file");
    }

    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    docFactory.setValidating(false);
    docFactory.setIgnoringElementContentWhitespace(true);
    docFactory.setSchema(null);
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();;
    docBuilder.setEntityResolver(new NullEntityResolver());
    Document configDOM = docBuilder.parse(configFile);
    Element root = configDOM.getDocumentElement();

    if (!root.getTagName().equals("module") || !root.getAttribute("name").equals("Checker")) {
      throw new IllegalArgumentException("XML root should be <module name\"Checker\">");
    }

    // start converting
    XMLConfig output = deepCopier(root);

    return output;
  }

  /**
   * Helper function that converts the Elements in the XML file into XMLConfig format.
   * The Element should have tag name "module"
   * @param child The DOM elements with the tag name module
   * @return a tree of checkstyle configuration, see overview in XMLConfig for more
   *         detailed structure.
   * */
  private static XMLConfig deepCopier(Element child) {
    XMLConfig output = new XMLConfig(child.getAttribute("name"));

    NodeList directChildren = child.getChildNodes();

    for (int i = 0; i < directChildren.getLength(); i++) {
      if (directChildren.item(i).getNodeType() == Node.ELEMENT_NODE) {
        Element grandChild = (Element)directChildren.item(i);

        if (grandChild.getTagName().equals("property")) {
          output.addAttribute(grandChild.getAttribute("name"), grandChild.getAttribute("value"));
        } else if (grandChild.getTagName().equals("message")) {
          output.addMessage(grandChild.getAttribute("key"), grandChild.getAttribute("value"));
        } else if (grandChild.getTagName().equals("module")) {
          output.addChild(deepCopier(grandChild));
        }
      }
    }


    return output;
  }

  /**
   * A helper resolver that stops Java from trying to reach to DTD server,
   * see https://stackoverflow.com/questions/6539051/how-can-i-tell-xalan-not-to-validate-xml-retrieved-using-the-document-function
   * for full detail
   * */
  private static class NullEntityResolver implements EntityResolver {

    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
      return new InputSource(new ByteArrayInputStream(new byte[0]));
    }

  }
}
