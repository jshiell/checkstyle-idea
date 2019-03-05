package org.infernus.idea.checkstyle.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class PropertyMetadataTest {
  @Test
  public void PropertyMetadataInitTest() {
    String name = "property";
    PropertyMetadata property = new PropertyMetadata(name);

    assertEquals(name, property.getName());
  }

  @Test
  public void PropertyMetadataSetNameTest() {
    String name = "property";
    PropertyMetadata property = new PropertyMetadata(name);

    String newName = "new Property";
    property.setName(newName);

    assertEquals(newName, property.getName());
  }

  @Test
  public void PropertyMetadataSetTypeTest() {
    String name = "property";
    PropertyMetadata property = new PropertyMetadata(name);

    String type = "String";
    property.setType(type);

    assertEquals(type, property.getType());
  }

  @Test
  public void PropertyMetadataSetDefaultValueTest() {
    String name = "property";
    PropertyMetadata property = new PropertyMetadata(name);

    String defaultValue = "null";
    property.setDefaultValue(defaultValue);

    assertEquals(defaultValue, property.getDefaultValue());
  }

  @Test
  public void PropertyMetadataSetDescriptionTest() {
    String name = "property";
    PropertyMetadata property = new PropertyMetadata(name);

    String description = "description";
    property.setDescription(description);

    assertEquals(description, property.getDescription());
  }
}