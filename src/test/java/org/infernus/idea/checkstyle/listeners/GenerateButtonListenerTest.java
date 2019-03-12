package org.infernus.idea.checkstyle.listeners;

import static org.mockito.Mockito.*;

import java.io.IOException;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;
import org.infernus.idea.checkstyle.ui.ConfigurationEditorWindow;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GenerateButtonListenerTest {
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
    when(this.configEditor.getConfigurationName()).thenReturn("bar");
    new GenerateButtonListener(this.view, this.model).actionPerformed(null);
  }

  @Test
  public void generatesFileFromConfigName() {
    try {
      verify(this.model, times(1)).generateConfig("bar");
    } catch (IOException ex) {
    }
  }
}