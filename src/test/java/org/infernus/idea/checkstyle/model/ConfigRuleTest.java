package org.infernus.idea.checkstyle.model;

import org.junit.Test;
import java.nio.file.Paths;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ConfigRuleTest {
  @Test
  public void ConfigRuleConstructorSetNameTest() {
    String ruleName = "rule1";
    ConfigRule rule = new ConfigRule(ruleName);

    assertEquals(ruleName, rule.getRuleName());
  }

  @Test
  public void ConfigRuleSetRuleNameTest() {
    String ruleName = "rule1";
    String newRuleName = "rule2";
    ConfigRule rule = new ConfigRule(ruleName);

    rule.setRuleName(newRuleName);

    assertEquals(newRuleName, rule.getRuleName());
  }

  @Test
  public void ConfigRuleSetCatagoryTest() {
    String ruleName = "rule1";
    ConfigRule rule = new ConfigRule(ruleName);

    assertEquals(null, rule.getCatagory());

    String catagory = "a";
    rule.setCatagory(catagory);

    assertEquals(catagory, rule.getCatagory());
  }

  @Test
  public void ConfigRuleSetRuleDescriptionTest() {
    String ruleName = "rule1";
    ConfigRule rule = new ConfigRule(ruleName);

    assertEquals(null, rule.getRuleDescription());

    String description = "this is a description";
    rule.setRuleDescription(description);

    assertEquals(description, rule.getRuleDescription());
  }

  @Test
  public void ConfigRuleAddParameterTest() {
    String ruleName = "rule1";
    ConfigRule rule = new ConfigRule(ruleName);

    assertEquals(0, rule.getParameters().size());

    String propertyName = "property";
    String type = "String";
    Map<String, String> info = new HashMap<>();

    info.put("type", type);

    rule.addParameter(propertyName, info);

    assertEquals(1, rule.getParameters().size());
    assertEquals(type, rule.getParameters().get(propertyName).get("type"));
  }
}
