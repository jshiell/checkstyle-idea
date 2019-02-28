package org.infernus.idea.checkstyle.util;

import org.infernus.idea.checkstyle.model.ConfigRule;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * This class reads the pre-saved rules xml to provide editor
 * necessary data.
 */
public class CheckStyleRuleProvider {
  private File defaultRules;
  private File customRules;

  private Map<String, List<ConfigRule>> defuleRuleByCatagory;
  private Map<String, ConfigRule> allDefaultRule;

  // TODO: read custom rules

  /**
   * Sets up the provided to self defined rule set
   * @param path - The path to the custom rule definition xml
   * @throws FileNotFoundException - When path doesn't lead to a existing xml file
   * @throws IllegalArgumentException - When path doesn't lead to xml file.
   */
  public CheckStyleRuleProvider(String path) throws FileNotFoundException, IllegalArgumentException {
    this();
    this.customRules = new File(path);

    if (!this.customRules.exists() || !this.customRules.isFile()) {
      throw new FileNotFoundException(path + " not found");
    }

    if (!Pattern.matches(".+\\.xml$", path)) {
      throw new IllegalArgumentException(path + "is not a xml file");
    }
  }

  public CheckStyleRuleProvider() {
    this.defaultRules = new File("src/main/resources/org/infernus/idea/checkstyle/available-rules.xml");

    this.defuleRuleByCatagory = new HashMap<String, List<ConfigRule>>();
    this.allDefaultRule = new HashMap<String, ConfigRule>();

    this.parseDefaultRules();
  }

  private void parseDefaultRules() {
    try {
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      docFactory.setValidating(false);
      docFactory.setIgnoringElementContentWhitespace(true);
      docFactory.setSchema(null);
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      docBuilder.setEntityResolver(new CheckStyleRuleProvider.NullEntityResolver());
      Document configDOM = docBuilder.parse(this.defaultRules);
      Element root = configDOM.getDocumentElement();

      NodeList children = root.getChildNodes();

      for (int i = 0; i < children.getLength(); i++) {
        if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
          Element child = (Element) children.item(i);
          if (child.getTagName().equals("module")) {
            if (!this.defuleRuleByCatagory.containsKey("Checker")) {
              this.defuleRuleByCatagory.put("Checker", new ArrayList<ConfigRule>());
            }

            ConfigRule rule = singleRuleMaker(child);
            this.defuleRuleByCatagory.get("Checker").add(rule);
            this.allDefaultRule.put("Checker", rule);
          } else {
            // rules in catagory
            Element catagory = (Element) children.item(i);
            if (!this.defuleRuleByCatagory.containsKey(catagory.getAttribute("name"))) {
              this.defuleRuleByCatagory.put(catagory.getAttribute("name"), new ArrayList<ConfigRule>());
            }

            NodeList grandChildren = catagory.getElementsByTagName("module");
            for (int s = 0; s < grandChildren.getLength(); s++) {
              Element grandChild = (Element) grandChildren.item(s);
              ConfigRule rule = this.singleRuleMaker(grandChild);
              this.defuleRuleByCatagory.get(catagory.getAttribute("name")).add(rule);
              this.allDefaultRule.put(rule.getRuleName(), rule);
            }
          }
        }
      }

    } catch (Exception e) {
      System.out.println("In here");
      System.out.println(e.getClass());
      StackTraceElement[] eles = e.getStackTrace();
      for (int j = 0; j < eles.length; j++) {
        System.out.println(eles[j].toString());
      }
    }
  }

  /**
   * Helper function that makes a module DOM into ConfigRule
   * @param module - The module DOM
   * @return The ConfigRule converted from module
   */
  public ConfigRule singleRuleMaker(Element module) {
    ConfigRule output = new ConfigRule(module.getAttribute("name"));
    Element description = (Element) module.getElementsByTagName("description").item(0);
    output.setRuleDescription(description.getAttribute("value"));

    NodeList properties = module.getElementsByTagName("property");
    for (int i = 0; i < properties.getLength(); i++) {
      Element property = (Element) properties.item(i);
      Map<String, String> info = new HashMap<String, String>();

      info.put("name", property.getAttribute("name"));
      info.put("type", property.getAttribute("type"));
      info.put("default", property.getAttribute("default"));
      info.put("id", "Not Set");

      output.addParameter(property.getAttribute("name"), info);
    }

    return output;
  }

  /**
   * @return The map containing all default rules, key is the catagory of the rules
   */
  public Map<String, List<ConfigRule>> getDefaultRuleByCatatog() {
    return new HashMap<String, List<ConfigRule>>(this.defuleRuleByCatagory);
  }

  /**
   * @param catagory - The catagory to search
   * @return A List of rules under one catagory.
   */
  public List<ConfigRule> getDefaultRulesByCatagory(String catagory) {
    return new ArrayList<ConfigRule>(this.defuleRuleByCatagory.get(catagory));
  }

  /**
   * @return The map containing all the default rules, key is the name of the rule.
   */
  public Map<String, ConfigRule> getAllConfigRuleByName() {
    return new HashMap<String, ConfigRule>(this.allDefaultRule);
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
