package org.infernus.idea.checkstyle.model;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This is the ADT that represents the check style XML configuration file, see
 * http://checkstyle.sourceforge.net/config.html for more detailed information.
 * In a nutshell, only tag “module” would be considered a child, and “property”
 * will be considered an attribute. For example, for a given XML as follows:
 *
 * <module name="Checker">
 *     <module name="JavadocPackage"/>
 *     <module name="NewlineAtEndOfFile"/>
 *     <module name="RegexpSingleline">
 *        <property name="format" value="\s+$"/>
 *        <property name="minimum" value="0"/>
 *        <property name="maximum" value="0"/>
 *        <property name="message" value="Line has trailing spaces."/>
 *  <message key="name.invalidPattern"
 *    value="Member ''{0}'' must start with a lowercase ''m'' (checked  pattern ''{1}'')."
 *    />
 *     </module>
 *     <module name="TreeWalker">
 *         <module name="JavadocMethod"/>
 *         <module name="JavadocType"/>
 *     </module>
 * </module>
 *
 * “JavadocPackage”, “NewlineAtEndOfFile”, “RegexpSingleline” and “TreeWalker”
 * are the children of “Checker”. “JavadocMethod” and “JavadocType” are children
 * of “TreeWalker”. However, “format”, “minimum”, “maximum” and “message” are
 * NOT children of “RegexpSingleline”, they are attributes of
 * “RegexpSingleline”. Other tags will be ignored. “message” also can not be
 * considered as a child.
 *
 * This class is to make sure that checkstyle-idea separates it service with the
 * services from checkstyle api, while we can represent any given configuration
 * in the plugin GUI. The structure is mainly taken from
 * https://github.com/checkstyle/checkstyle/blob/master/src/main/java/com/puppycrawl/tools/checkstyle/api/Configuration.java
 * to maintain a representation that is as close to the checkstyle
 * representation as possible.
 */
public class XMLConfig {
  private String name;
  private Map<String, String> attributes;
  private Map<String, String> msgs;
  private Set<XMLConfig> children;

  /**
   * Initialize with a configuration(module) name.
   * 
   * @param name - The name for this module.
   */
  public XMLConfig(String name) {
    this.name = name;
    this.attributes = new HashMap<String, String>();
    this.msgs = new HashMap<String, String>();
    this.children = new HashSet<XMLConfig>();
  }

  /**
   * The set of attribute names.
   * 
   * @return String[] - The set of attribute names, never null. Returned set
   *         separated from underlying data structure in XMLConfig
   */
  public String[] getAttributeNames() {
    Set<String> set = this.attributes.keySet();
    return set.toArray(new String[set.size()]);
  }

  /**
   * Checks whether an attribute has been set.
   * 
   * @param name - the attribute name
   * @return Whether or not this attribute has been set
   */
  public boolean isAttributeSet(String name) {
    return this.attributes.containsKey(name);
  }

  /**
   * The attribute value for an attribute name.
   * 
   * @param name - the attribute name
   * @return String - the value that is associated with name
   * @throws IllegalArgumentException - if name is not a valid attribute name
   */
  public String getAttribute(String name) throws IllegalArgumentException {
    if (!this.attributes.containsKey(name)) {
      throw new IllegalArgumentException(name + "is not existing attribute");
    }

    return this.attributes.get(name);
  }

  /**
   * The set of child configurations.
   * 
   * @return The set of child configurations, never null. Returned set separated
   *         from underlying data structure in XMLConfig
   */
  public XMLConfig[] getChildren() {
    return this.children.toArray(new XMLConfig[this.children.size()]);
  }

  /**
   * The name of this configuration.
   * 
   * @return String - The name of this configuration(module).
   */
  public String getName() {
    return this.name;
  }

  /**
   * Returns an map instance containing the custom messages for this
   * configuration. See
   * http://checkstyle.sourceforge.net/config.html#Custom_messages for more
   * information.
   * 
   * @return map containing custom messages
   */
  public Map<String, String> getMessages() {
    return new HashMap<String, String>(this.msgs);
  }

  /**
   * Sets the name of this configuration(module).
   * 
   * @param name - the name to set to this configuration(module).
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Add a new Child to XMLConfig
   * 
   * @param child - the child to add
   */
  public void addChild(XMLConfig child) {
    this.children.add(child);
  }

  /**
   * Clear all child in the XMLConfig
   */
  public void clearChildren() {
    this.children = new HashSet<XMLConfig>();
  }

  /**
   * Sets a new attribute to this configuration(module). Will overwrite value if
   * attribute with the same name exists
   * 
   * @param name  - The name of the attribute
   * @param value - The value of the attribute
   */
  public void addAttribute(@NotNull String name, @NotNull String value) {
    this.attributes.put(name, value);
  }

  /**
   * Removes an attribute
   * 
   * @param name - The name of the attribute to remove
   */
  public void removeAttribute(String name) {
    this.attributes.remove(name);
  }

  /**
   * Sets a new key value pair of message to this configuration(module). Will
   * overwrite value if key with the same name exists
   * 
   * @param key   - The name of the attribute
   * @param value - The value of the attribute
   */
  public void addMessage(String key, String value) {
    this.msgs.put(key, value);
  }

  /**
   * Removes the message with the same key
   * 
   * @param key - The key to delete
   */
  public void removeMessage(String key) {
    this.msgs.remove(key);
  }
}
