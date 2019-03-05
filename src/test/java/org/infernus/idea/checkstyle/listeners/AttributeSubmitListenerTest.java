package org.infernus.idea.checkstyle.listeners;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.model.XMLConfig;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;
import org.infernus.idea.checkstyle.ui.ConfigurationEditorWindow;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AttributeSubmitListenerTest {
  @Mock
  ConfigGeneratorView view;
  @Mock
  ConfigurationEditorWindow configEditor;

  @Mock
  ConfigGeneratorModel model;

  AttributeSubmitListener asl;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(this.view.getConfigEditor()).thenReturn(this.configEditor);
    when(this.model.getActiveRules()).thenReturn(new ArrayList<>());
    this.asl = new AttributeSubmitListener(this.view, this.model);
  }

  @Test
  public void doesNotAddIfNotNew() {
    this.asl.attributeSubmitted(null, false);
    verify(this.model, times(0)).addActiveRule(any());
  }

  @Test
  public void addsIfNew() {
    XMLConfig xmlRule = new XMLConfig("");
    this.asl.attributeSubmitted(xmlRule, true);
    verify(this.model, times(1)).addActiveRule(xmlRule);
  }

  @Test
  public void getsActiveRulesOnSubmit() {
    XMLConfig xmlRule = new XMLConfig("");
    this.asl.attributeSubmitted(xmlRule, true);
    verify(this.model, times(1)).getActiveRules();
  }

  @Test
  public void setsActiveRulesOnSubmit() {
    XMLConfig xmlRule = new XMLConfig("");
    this.asl.attributeSubmitted(xmlRule, true);
    verify(this.configEditor, times(1)).setActiveRules(any(Collection.class));
  }

  @Test
  public void doesNotDeleteIfNew() {
    this.asl.attributeCancelled(null, true);
    verify(this.model, times(0)).removeActiveRule(any());
  }

  @Test
  public void deletesIfNotNew() {
    XMLConfig xmlRule = new XMLConfig("");
    this.asl.attributeCancelled(xmlRule, false);
    verify(this.model, times(1)).removeActiveRule(xmlRule);
  }

  @Test
  public void getsActiveRulesOnCancel() {
    XMLConfig xmlRule = new XMLConfig("");
    this.asl.attributeCancelled(xmlRule, false);
    verify(this.model, times(1)).getActiveRules();
  }

  @Test
  public void setsActiveRulesOnCancel() {
    XMLConfig xmlRule = new XMLConfig("");
    this.asl.attributeCancelled(xmlRule, false);
    verify(this.configEditor, times(1)).setActiveRules(any(Collection.class));
  }
}