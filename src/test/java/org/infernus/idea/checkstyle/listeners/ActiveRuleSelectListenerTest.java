package org.infernus.idea.checkstyle.listeners;

import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.model.ConfigRule;
import org.infernus.idea.checkstyle.model.XMLConfig;
import org.infernus.idea.checkstyle.ui.CheckAttributesEditorDialog;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;
import org.infernus.idea.checkstyle.ui.ConfigurationEditorWindow;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ActiveRuleSelectListenerTest {
  @Mock
  ConfigGeneratorView view;
  @Mock
  ConfigurationEditorWindow configEditor;
  @Mock
  CheckAttributesEditorDialog attrEditor;
  
  @Mock
  ConfigGeneratorModel model;

  ActiveRuleSelectListener arsl;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(this.view.getConfigEditor()).thenReturn(this.configEditor);
    when(this.view.getAttrEditor()).thenReturn(this.attrEditor);
    when(this.configEditor.getSelectedActiveIndex()).thenReturn(0);
    when(this.model.getActiveRules()).thenReturn(Arrays.asList(new XMLConfig("")));
    this.arsl = new ActiveRuleSelectListener(this.view, this.model);
  }
  
  @Test
  public void displaysAttrEditor() {
    when(this.configEditor.getSelectedActiveRule()).thenReturn(new ConfigRule(""));
    this.arsl.mouseClicked(null);
    verify(this.attrEditor, times(1)).displayForCheck(any(ConfigRule.class), any(XMLConfig.class));
  }
  
  @Test
  public void doesNothingIfNoneSelected() {
    this.arsl.mouseClicked(null);
    verify(this.attrEditor, times(0)).displayForCheck(any());
  }
}