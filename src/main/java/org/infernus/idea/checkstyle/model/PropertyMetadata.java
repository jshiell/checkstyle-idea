package org.infernus.idea.checkstyle.model;

public class PropertyMetadata {
  private String name;
  private String type;
  private String defaultValue;
  private String description;
  private String parent;

  /**
   * @param name - The name of the property
   */
  public PropertyMetadata(String name) {
    this.name = name;
  }

  /**
   * @return The name of this property
   */
  public String getName() {
    return this.name;
  }

  /**
   * Sets the name of this property.
   * @param name - The name of this property.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return The type should be given to the property
   */
  public String getType() {
    return this.type;
  }

  /**
   * Set the type of this property
   * @param type - The type of this property
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * @return The default value of this property. Not necessary the
   *         exact default value, some times can be descriptions
   */
  public String getDefaultValue() {
    return this.defaultValue;
  }

  /**
   * Sets the default value of the property
   * @param defaultValue - The default value of the property. Not necessary the
   *                       exact default value, some times can be descriptions
   */
  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  /**
   * @return The description of this rule
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Sets the description for the property
   * @param description - The description of the metadata
   */
  public void setDescription(String description) {
    this.description = description;
  }
}
