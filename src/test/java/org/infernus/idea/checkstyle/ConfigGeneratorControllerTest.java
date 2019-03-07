package org.infernus.idea.checkstyle;

import static org.mockito.Mockito.*;

import org.infernus.idea.checkstyle.listeners.ActiveRuleSelectListener;
import org.infernus.idea.checkstyle.listeners.AttributeSubmissionListener;
import org.infernus.idea.checkstyle.listeners.CategorySelectListener;
import org.infernus.idea.checkstyle.listeners.ClearButtonListener;
import org.infernus.idea.checkstyle.listeners.GenerateButtonListener;
import org.infernus.idea.checkstyle.listeners.ImportButtonListener;
import org.infernus.idea.checkstyle.listeners.ImportSubmitListener;
import org.infernus.idea.checkstyle.listeners.PreviewButtonListener;
import org.infernus.idea.checkstyle.listeners.VisibleRuleSelectListener;
import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.ui.CheckAttributesEditorDialog;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;
import org.infernus.idea.checkstyle.ui.ConfigurationEditorWindow;
import org.infernus.idea.checkstyle.ui.ImportConfigDialog;
import org.infernus.idea.checkstyle.util.ConfigurationListeners;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ConfigGeneratorControllerTest {
  @Mock
  ConfigGeneratorView view;
  @Mock
  ConfigurationEditorWindow configEditor;
  @Mock
  ImportConfigDialog configDialog;
  @Mock
  CheckAttributesEditorDialog attrEditor;

  @Mock
  ConfigGeneratorModel model;

  ConfigGeneratorController controller;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(this.view.getConfigEditor()).thenReturn(this.configEditor);
    when(this.view.getConfigDialog()).thenReturn(this.configDialog);
    when(this.view.getAttrEditor()).thenReturn(this.attrEditor);

    this.controller = new ConfigGeneratorController(view, model);
  }

  @Test
  public void setsCategories() {
    verify(this.model, times(1)).getAvailableRules();
    verify(this.configEditor, times(1)).setCategories(any());
  }

  @Test
  public void addsAllSelectListeners() {
    verify(this.configEditor, times(1)).addSelectionListener(any(CategorySelectListener.class),
        any(ConfigurationListeners.class));
    verify(this.configEditor, times(1)).addSelectionListener(any(VisibleRuleSelectListener.class),
        any(ConfigurationListeners.class));
    verify(this.configEditor, times(1)).addSelectionListener(any(ActiveRuleSelectListener.class),
        any(ConfigurationListeners.class));
  }

  @Test
  public void addsAllButtonListeners() {
    verify(this.configEditor, times(1)).addButtonListener(any(ImportButtonListener.class),
        any(ConfigurationListeners.class));
    verify(this.configEditor, times(1)).addButtonListener(any(ClearButtonListener.class),
        any(ConfigurationListeners.class));
    verify(this.configEditor, times(1)).addButtonListener(any(PreviewButtonListener.class),
        any(ConfigurationListeners.class));
    verify(this.configEditor, times(1)).addButtonListener(any(GenerateButtonListener.class),
        any(ConfigurationListeners.class));
  }

  @Test
  public void addsImportSubmitListener() {
    verify(this.configDialog, times(1)).addSubmitListener(any(ImportSubmitListener.class));
  }

  @Test
  public void addsAttributeSubmitListener() {
    verify(this.attrEditor, times(1)).addSubmitListener(any(AttributeSubmissionListener.class));
  }

  @Test
  public void displaysEditorWindow() {
    this.controller.displayConfigEditor();
    verify(this.configEditor, times(1)).setVisible(true);
    ;
  }
}