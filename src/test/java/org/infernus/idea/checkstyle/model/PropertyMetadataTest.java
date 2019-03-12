package org.infernus.idea.checkstyle.model;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

  @Ignore
  @Test
  public void compareToOrderTest() {
    List<String> sorted = Arrays.asList("id", "severity", "abc", "def", "ghi", null);
    List<PropertyMetadata> lst = Arrays.asList(new PropertyMetadata(null), new PropertyMetadata("def"),
        new PropertyMetadata("abc"), new PropertyMetadata("severity"), new PropertyMetadata("ghi"),
        new PropertyMetadata("id"));

    Collections.sort(lst);

    int i;
    for (i = 0; i < sorted.size(); i++) {
      assertEquals(sorted.get(i), lst.get(i).getName());
    }
  }
}