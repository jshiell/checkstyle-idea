package org.infernus.idea.checkstyle.model;

public class PropertyMetadata implements Comparable<PropertyMetadata> {
  private String name;
  private String type;
  private String defaultValue;
  private String description;

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
   * 
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
   * 
   * @param type - The type of this property
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * @return The default value of this property. Not necessary the exact default
   *         value, some times can be descriptions
   */
  public String getDefaultValue() {
    return this.defaultValue;
  }

  /**
   * Sets the default value of the property
   * 
   * @param defaultValue - The default value of the property. Not necessary the
   *                     exact default value, some times can be descriptions
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
   * 
   * @param description - The description of the metadata
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Compares this PropertyMetadata object to pm by the following strategy: If the
   * names of this and pm are null, they are equal in order If only one name is
   * null, it is less than the other If both names are the same, the objects are
   * equal in order If only one object has name "id", it is greater If only one
   * object has name "severity", it is greater (but only if neither has name "id")
   * Otherwise, compare the names alphabetically
   * 
   * @param pm The PropertyMetada to compare against this
   * @return 1 if this is greater, 0 if this and pm are equal in order, and -1 if
   *         pm is greater
   */
  @Override
  public int compareTo(PropertyMetadata pm) {
    if (pm == null) {
      return -1;
    } if (this.name == null || pm.name == null) {
      if (this.name == null && pm.name == null) {
        return 0;
      } else if (this.name == null) {
        return 1;
      } else {
        return -1;
      }
    } else if (this.name.equals(pm.name)) {
      return 0;
    } else {
      if (this.name.equals("id")) {
        return -1;
      } else if (pm.name.equals("id")) {
        return 1;
      } else if (this.name.equals("severity")) {
        return -1;
      } else if (pm.name.equals("severity")) {
        return 1;
      } else {
        return this.name.compareTo(pm.name);
      }
    }
  }
}
