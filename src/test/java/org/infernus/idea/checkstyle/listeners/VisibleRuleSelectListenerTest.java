package org.infernus.idea.checkstyle.listeners;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.infernus.idea.checkstyle.model.ConfigRule;
import org.infernus.idea.checkstyle.ui.CheckAttributesEditorDialog;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;
import org.infernus.idea.checkstyle.ui.ConfigurationEditorWindow;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class VisibleRuleSelectListenerTest {
  @Mock
  ConfigGeneratorView view;
  @Mock
  ConfigurationEditorWindow configEditor;
  @Mock
  CheckAttributesEditorDialog attrEditor;

  ConfigRule rule = new ConfigRule("foobar");

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(this.view.getConfigEditor()).thenReturn(this.configEditor);
    when(this.view.getAttrEditor()).thenReturn(this.attrEditor);
    when(this.configEditor.getSelectedVisibleRule()).thenReturn(this.rule);
    new VisibleRuleSelectListener(this.view).mouseClicked(null);
  }

  @Test
  public void displaysAttrEditor() {
    verify(this.attrEditor, times(1)).displayForCheck(this.rule);
  }
}