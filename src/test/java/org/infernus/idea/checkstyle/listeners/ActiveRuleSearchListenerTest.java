package org.infernus.idea.checkstyle.listeners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.model.ConfigRule;
import org.infernus.idea.checkstyle.model.XMLConfig;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;
import org.infernus.idea.checkstyle.ui.ConfigurationEditorWindow;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ActiveRuleSearchListenerTest {
  @Mock
  ConfigGeneratorView view;
  @Mock
  ConfigurationEditorWindow configEditor;

  @Mock
  ConfigGeneratorModel model;

  List<XMLConfig> rules;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    initRules();
    when(this.view.getConfigEditor()).thenReturn(this.configEditor);
    when(this.model.getActiveRules()).thenReturn(this.rules);
    when(this.model.getConfigRuleforXML(any())).thenAnswer(new Answer<ConfigRule>() {
      public ConfigRule answer(InvocationOnMock invocation) {
        XMLConfig xmlRule = (XMLConfig) invocation.getArguments()[0];
        ConfigRule configRule = new ConfigRule(xmlRule.getName());
        if ("rule2".equals(configRule.getRuleName())) {
          configRule.setRuleDescription("description");
        }
        return configRule;
      }
    });
  }

  private void initRules() {
    XMLConfig ruleWithAttribute = new XMLConfig("att");
    ruleWithAttribute.addAttribute("attr1", "value1");
    ruleWithAttribute.addAttribute("attr2", "value2");
    this.rules = Arrays.asList(new XMLConfig("rule1"), new XMLConfig("rule2"), ruleWithAttribute,
        new XMLConfig("nothin"));
  }

  private Collection<ConfigRule> getFilteredRules() {
    ArgumentCaptor<Collection<ConfigRule>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(this.configEditor, times(1)).setActiveRules(captor.capture());
    return captor.getValue();
  }

  @Test
  public void setActiveRules() {
    new ActiveRuleSearchListener(this.view, this.model).searchPerformed("rule");
    verify(this.configEditor, times(1)).setActiveRules(any(Collection.class));
  }

  @Test
  public void getsRulesByName() {
    new ActiveRuleSearchListener(this.view, this.model).searchPerformed("rule");
    boolean hasName1 = false;
    boolean hasName2 = false;
    Collection<ConfigRule> rules = getFilteredRules();
    for (ConfigRule c : rules) {
      if ("rule1".equals(c.getRuleName())) {
        hasName1 = true;
      } else if ("rule2".equals(c.getRuleName())) {
        hasName2 = true;
      }
    }

    assertEquals(2, rules.size());
    assertTrue(hasName1);
    assertTrue(hasName2);
  }

  @Test
  public void getsRulesFromDescription() {
    new ActiveRuleSearchListener(this.view, this.model).searchPerformed("description");
    assertEquals("rule2", new ArrayList<>(getFilteredRules()).get(0).getRuleName());
  }

  @Test
  public void getsRulesFromFirstAttrName() {
    new ActiveRuleSearchListener(this.view, this.model).searchPerformed("attr1");
    assertEquals("att", new ArrayList<>(getFilteredRules()).get(0).getRuleName());
  }

  @Test
  public void getsRulesFromFirstAttrValue() {
    new ActiveRuleSearchListener(this.view, this.model).searchPerformed("value1");
    assertEquals("att", new ArrayList<>(getFilteredRules()).get(0).getRuleName());
  }

  @Test
  public void getsRulesFromSecondAttrName() {
    new ActiveRuleSearchListener(this.view, this.model).searchPerformed("attr2");
    assertEquals("att", new ArrayList<>(getFilteredRules()).get(0).getRuleName());
  }

  @Test
  public void getsRulesFromSecondAttrValue() {
    new ActiveRuleSearchListener(this.view, this.model).searchPerformed("value2");
    assertEquals("att", new ArrayList<>(getFilteredRules()).get(0).getRuleName());
  }

  @Test
  public void filtersUnmatchedRules() {
    new ActiveRuleSearchListener(this.view, this.model).searchPerformed("rule");
    boolean hasNothing = false;

    for (ConfigRule c : getFilteredRules()) {
      if ("nothin".equals(c.getRuleName())) {
        hasNothing = true;
      }
    }

    assertFalse(hasNothing);
  }
}
