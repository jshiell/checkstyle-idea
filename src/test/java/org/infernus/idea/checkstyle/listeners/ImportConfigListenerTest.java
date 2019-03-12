package org.infernus.idea.checkstyle.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;
import org.infernus.idea.checkstyle.ui.ConfigurationEditorWindow;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ImportConfigListenerTest {
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
    new ImportConfigListener(this.view, this.model).configSubmitted("baz");
  }

  @Test
  public void loadsConfigIntoModel() {
    try {
      verify(this.model, times(1)).importConfig("baz");
    } catch (Exception ex) {
    }
  }

  @Test
  public void updatesActiveRulesInView() {
    verify(this.configEditor, times(1)).setActiveRules(any(List.class));
  }
}