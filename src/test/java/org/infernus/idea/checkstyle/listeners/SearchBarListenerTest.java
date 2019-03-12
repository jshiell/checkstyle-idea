package org.infernus.idea.checkstyle.listeners;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.model.ConfigRule;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;
import org.infernus.idea.checkstyle.ui.ConfigurationEditorWindow;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SearchBarListenerTest {
  @Mock
  ConfigGeneratorView view;
  @Mock
  ConfigurationEditorWindow configEditor;

  @Mock
  ConfigGeneratorModel model;

  TreeMap<String, List<ConfigRule>> rules;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    initRules();
    when(this.view.getConfigEditor()).thenReturn(this.configEditor);
    when(this.model.getAvailableRules()).thenReturn(this.rules);
    new SearchBarListener(this.view, this.model).searchPerformed("query");
  }

  private void initRules() {
    this.rules = new TreeMap<>();
    ConfigRule queryInDesc = new ConfigRule("");
    queryInDesc.setRuleDescription("query in description");
    this.rules.put("Category 1", Arrays.asList(new ConfigRule("query in name 1"), queryInDesc));
    this.rules.put("Category 2", Arrays.asList(new ConfigRule("nothin"), new ConfigRule("query in name 2")));
  }

  private Collection<ConfigRule> getFilteredRules() {
    ArgumentCaptor<Collection<ConfigRule>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(this.configEditor, times(1)).setVisibleRules(anyString(), captor.capture());
    return captor.getValue();
  }

  @Test
  public void setsVisibleRules() {
    verify(this.configEditor, times(1)).setVisibleRules(anyString(), any(Collection.class));
  }

  @Test
  public void getsRulesFromBothCategories() {
    boolean hasName1 = false;
    boolean hasName2 = false;

    for (ConfigRule c : getFilteredRules()) {
      if ("query in name 1".equals(c.getRuleName())) {
        hasName1 = true;
      } else if ("query in name 2".equals(c.getRuleName())) {
        hasName2 = true;
      }
    }

    assertTrue(hasName1);
    assertTrue(hasName2);
  }

  @Test
  public void getsRulesFromDescription() {
    boolean hasDesc = false;

    for (ConfigRule c : getFilteredRules()) {
      if ("query in description".equals(c.getRuleDescription())) {
        hasDesc = true;
      }
    }

    assertTrue(hasDesc);
  }

  @Test
  public void filtersUnmatchedRules() {
    boolean hasNothing = false;

    for (ConfigRule c : getFilteredRules()) {
      if ("nothin".equals(c.getRuleName())) {
        hasNothing = true;
      }
    }

    assertFalse(hasNothing);
  }
}