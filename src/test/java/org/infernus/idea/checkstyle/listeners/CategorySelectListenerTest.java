package org.infernus.idea.checkstyle.listeners;

import static org.mockito.Mockito.*;

import java.util.TreeMap;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;
import org.infernus.idea.checkstyle.ui.ConfigurationEditorWindow;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CategorySelectListenerTest {
  @Mock
  ConfigGeneratorView view;
  @Mock
  ConfigurationEditorWindow configEditor;

  @Mock
  ConfigGeneratorModel model;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(this.view.getConfigEditor()).thenReturn(this.configEditor);
    when(this.model.getAvailableRules()).thenReturn(new TreeMap<>());
    when(this.configEditor.getSelectedCategory()).thenReturn("foo");
    new CategorySelectListener(this.view, this.model).mouseClicked(null);
  }

  @Test
  public void updatesVisibleRules() {
    verify(this.configEditor, times(1)).setVisibleRules("foo", null);
  }
}