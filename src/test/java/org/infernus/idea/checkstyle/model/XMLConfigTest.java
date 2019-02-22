package org.infernus.idea.checkstyle.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class XMLConfigTest {
  @Test
  public void XMLConfigInitSetNameTest() {
    String name = "New Config";
    XMLConfig temp = new XMLConfig(name);

    assertEquals(name, temp.getName());
  }

  @Test
  public void XMLConfigInitEmptyAttributeTest() {
    XMLConfig temp = new XMLConfig("empty");

    assertEquals(0, temp.getAttributeNames().length);
  }

  @Test
  public void XMLConfigInitEmptyMsgTest() {
    XMLConfig temp = new XMLConfig("empty");

    assertEquals(0, temp.getMessages().size());
  }

  @Test
  public void XMLConfigInitEmptyChildrenTest() {
    XMLConfig empty = new XMLConfig("NO Kids");

    assertEquals(0, empty.getChildren().length);
  }

  @Test
  public void XMLConfigGetAttributeNamesTest() {
    XMLConfig config = new XMLConfig("config");
    String attr1 = "attr1";
    String value1 = "value1";
    String attr2 = "second attr";
    String value2 = "value two";

    config.addAttribute(attr1, value1);
    config.addAttribute(attr2, value2);

    assertEquals(2, config.getAttributeNames().length);
  }

  @Test(expected = IllegalArgumentException.class)
  public void XMLConfigGetAttributeNotValidNameTest() {
    XMLConfig config = new XMLConfig("config");

    config.getAttribute("definitely not there");
  }

  @Test
  public void XMLConfigGetAddedAttributeTest() {
    XMLConfig config = new XMLConfig("config");
    String attr1 = "attr1";
    String value1 = "value1";
    String attr2 = "second attr";
    String value2 = "value two";

    config.addAttribute(attr1, value1);
    config.addAttribute(attr2, value2);

    assertEquals(value1, config.getAttribute(attr1));
    assertEquals(value2, config.getAttribute(attr2));

    config.addAttribute(attr1, value2);

    assertEquals(value2, config.getAttribute(attr1));
    assertEquals(value2, config.getAttribute(attr2));
  }

  @Test
  public void XMLConfigRemoveAttributeTest() {
    XMLConfig config = new XMLConfig("config");
    String attr1 = "attr1";
    String value1 = "value1";
    String attr2 = "second attr";
    String value2 = "value two";

    config.addAttribute(attr1, value1);
    config.addAttribute(attr2, value2);

    assertEquals(2, config.getAttributeNames().length);
    config.removeAttribute(attr1);
    assertEquals(1, config.getAttributeNames().length);
    config.removeAttribute(attr2);
    assertEquals(0, config.getAttributeNames().length);
    config.removeAttribute(attr2);
    assertEquals(0, config.getAttributeNames().length);
  }

  @Test
  public void XMLConfigAddChildrenTest() {
    XMLConfig config = new XMLConfig("Check Child");
    XMLConfig child = new XMLConfig("Child");

    assertEquals(0, config.getChildren().length);
    config.addChild(child);
    assertEquals(1, config.getChildren().length);
    assertEquals(child, config.getChildren()[0]);
  }

  @Test
  public void XMLConfigClearChildrenTest() {
    XMLConfig config = new XMLConfig("Check Child");
    XMLConfig child = new XMLConfig("Child");

    assertEquals(0, config.getChildren().length);
    config.addChild(child);
    assertEquals(1, config.getChildren().length);
    config.clearChildren();
    assertEquals(0, config.getChildren().length);
  }

  @Test
  public void XMLConfigSetNameTest() {
    String oldName = "old";
    String newName = "new";

    XMLConfig config = new XMLConfig(oldName);

    config.setName(newName);

    assertEquals(newName, config.getName());
  }

  @Test
  public void XMLConfigGetAddMessagesTest() {
    XMLConfig config = new XMLConfig("config");
    String key1 = "JOJO!!!";
    String value1 = "nani?!";

    config.addMessage(key1, value1);

    assertEquals(1, config.getMessages().size());
  }

  @Test
  public void XMLConfigRemoveMessagesTest() {
    XMLConfig config = new XMLConfig("config");
    String key1 = "JOJO!!!";
    String value1 = "nani?!";

    config.addMessage(key1, value1);
    config.removeMessage(key1);

    assertEquals(0, config.getMessages().size());
  }


}