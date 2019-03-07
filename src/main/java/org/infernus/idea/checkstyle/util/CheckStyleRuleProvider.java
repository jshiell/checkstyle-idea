package org.infernus.idea.checkstyle.util;

import org.infernus.idea.checkstyle.model.ConfigRule;
import org.infernus.idea.checkstyle.model.PropertyMetadata;
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
import java.util.*;
import java.util.regex.Pattern;

/**
 * This class reads the pre-saved rules xml to provide editor necessary data.
 */
public class CheckStyleRuleProvider {
  private String defaultRules;
  private File customRules;
  private String typeOptions;

  private Map<String, List<ConfigRule>> defuleRuleByCategory;
  private Map<String, ConfigRule> allDefaultRule;

  private Map<String, Set<String>> defaultTypeOption;

  // TODO: read custom rules

  /**
   * Sets up the provided to self defined rule set
   * 
   * @param path - The path to the custom rule definition xml
   * @throws FileNotFoundException    - When path doesn't lead to a existing xml
   *                                  file
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
    this.defaultRules = "/org/infernus/idea/checkstyle/available-rules.xml";
    this.typeOptions = "/org/infernus/idea/checkstyle/type-options.xml";

    this.defuleRuleByCategory = new HashMap<String, List<ConfigRule>>();
    this.allDefaultRule = new HashMap<String, ConfigRule>();
    this.defaultTypeOption = new HashMap<String, Set<String>>();

    this.parseDefaultRules();
    this.parseDefaultTypeOptions();
  }

  private void parseDefaultRules() {
    try {
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      docFactory.setValidating(false);
      docFactory.setIgnoringElementContentWhitespace(true);
      docFactory.setSchema(null);
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      docBuilder.setEntityResolver(new CheckStyleRuleProvider.NullEntityResolver());
      Document configDOM = docBuilder.parse(getClass().getResourceAsStream(this.defaultRules));
      Element root = configDOM.getDocumentElement();

      NodeList children = root.getChildNodes();

      for (int i = 0; i < children.getLength(); i++) {
        if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
          Element child = (Element) children.item(i);
          if (child.getTagName().equals("module")) {
            if (!this.defuleRuleByCategory.containsKey("Checker")) {
              this.defuleRuleByCategory.put("Checker", new ArrayList<ConfigRule>());
            }

            ConfigRule rule = singleRuleMaker(child);
            this.defuleRuleByCategory.get("Checker").add(rule);
            this.allDefaultRule.put("Checker", rule); // Checker itself has no parent
          } else {
            // rules in category
            Element category = (Element) children.item(i);
            if (!this.defuleRuleByCategory.containsKey(category.getAttribute("name"))) {
              this.defuleRuleByCategory.put(category.getAttribute("name"), new ArrayList<ConfigRule>());
            }

            NodeList grandChildren = category.getElementsByTagName("module");
            for (int s = 0; s < grandChildren.getLength(); s++) {
              Element grandChild = (Element) grandChildren.item(s);
              ConfigRule rule = this.singleRuleMaker(grandChild);
              this.defuleRuleByCategory.get(category.getAttribute("name")).add(rule);
              this.allDefaultRule.put(rule.getRuleName(), rule);
            }
          }
        }
      }

    } catch (Exception e) {
      System.out.println(e.getClass());
      StackTraceElement[] eles = e.getStackTrace();
      for (int j = 0; j < 10; j++) {
        System.out.println(eles[j].toString());
      }
    }
  }

  /**
   * Helper function that makes a module DOM into ConfigRule
   * 
   * @param module - The module DOM
   * @return The ConfigRule converted from module
   */
  public ConfigRule singleRuleMaker(Element module) {
    ConfigRule output = new ConfigRule(module.getAttribute("name"));
    output.setRuleDescription(module.getAttribute("description"));
    output.setParent(module.getAttribute("parent"));

    NodeList properties = module.getElementsByTagName("property");
    for (int i = 0; i < properties.getLength(); i++) {
      Element property = (Element) properties.item(i);
      PropertyMetadata info = new PropertyMetadata(property.getAttribute("name"));

      info.setType(property.getAttribute("type"));
      info.setDefaultValue(property.getAttribute("default"));
      info.setDescription(property.getAttribute("description"));

      output.addParameter(property.getAttribute("name"), info);
    }

    PropertyMetadata id = new PropertyMetadata("id");
    id.setType("String");
    id.setDescription("Each module has a id property that can rename the module name to be a name defined by the user. This is used to differentiate two instances of the same Check. This custom module name should be unique for the entire configuration to prevent accidental overlapping. This custom name is required to be able to suppress violations of 1 of the checks in specific scenarios, while leaving the other unaffected. Without the custom module name, it is harder to differentiate one module's violations from the other.");
    id.setDefaultValue("null");

    output.addParameter("id", id);

    PropertyMetadata severty = new PropertyMetadata("severity");
    severty.setType("Severity");
    severty.setDescription("the default severity level of all violations");
    severty.setDefaultValue("error");

    if (!output.getRuleName().equals("Checker")) {
      output.addParameter(severty.getName(), severty);
    }

    return output;
  }

  private void parseDefaultTypeOptions() {
    try {
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      docFactory.setValidating(false);
      docFactory.setIgnoringElementContentWhitespace(true);
      docFactory.setSchema(null);
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      docBuilder.setEntityResolver(new CheckStyleRuleProvider.NullEntityResolver());
      Document configDOM = docBuilder.parse(getClass().getResourceAsStream(this.typeOptions));
      Element root = configDOM.getDocumentElement();

      NodeList children = root.getChildNodes();

      for (int i = 0; i < children.getLength(); i++) {
        if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
          Element child = (Element) children.item(i);
          this.defaultTypeOption.put(child.getAttribute("name"), new TreeSet<String>());

          NodeList options = child.getElementsByTagName("option");
          for (int j = 0; j < options.getLength(); j++) {
            Element optionNode = (Element)options.item(j);
            this.defaultTypeOption.get(child.getAttribute("name")).add(optionNode.getAttribute("value"));
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
   * @return The map containing all default rules, key is the category of the
   *         rules
   */
  public Map<String, List<ConfigRule>> getDefaultCategorizedRules() {
    return new HashMap<String, List<ConfigRule>>(this.defuleRuleByCategory);
  }

  /**
   * @param category - The category to search
   * @return A List of rules under one category.
   */
  public List<ConfigRule> getDefaultRulesByCategory(String category) {
    return new ArrayList<ConfigRule>(this.defuleRuleByCategory.get(category));
  }

  /**
   * @return The map containing all the default rules, key is the name of the
   *         rule.
   */
  public Map<String, ConfigRule> getDefaultRules() {
    return new HashMap<String, ConfigRule>(this.allDefaultRule);
  }

  /**
   * @param name - The name of the config
   * @return The ConfigRule having the name
   */
  public ConfigRule getDefaultRuleByName(String name) {
    return this.allDefaultRule.get(name);
  }

  /**
   * @param type - The type to retrieve its options
   * @return The set of the option of the given type. null when
   *         such set doesn't exist. The set is pre-sorted
   */
  public Set<String> getTypeOptions(String type) {
    return this.defaultTypeOption.get(type);
  }

  /**
   * A helper resolver that stops Java from trying to reach to DTD server,
   * see https://stackoverflow.com/questions/6539051/how-can-i-tell-xalan-not-to-validate-xml-retrieved-using-the-document-function
   * for full detail
   */
  private static class NullEntityResolver implements EntityResolver {

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
      return new InputSource(new ByteArrayInputStream(new byte[0]));
    }

  }
}
