package org.infernus.idea.checkstyle.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.model.XMLConfig;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;
import org.infernus.idea.checkstyle.ui.ConfigurationEditorWindow;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ClearButtonListenerTest {
  @Mock
  ConfigGeneratorView view;
  @Mock
  ConfigurationEditorWindow configEditor;

  @Mock
  ConfigGeneratorModel model;

  List<XMLConfig> activeRules = Arrays.asList(new XMLConfig("a"), new XMLConfig("b"));

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(this.view.getConfigEditor()).thenReturn(this.configEditor);
    when(this.model.getActiveRules()).thenReturn(this.activeRules);
    new ClearButtonListener(this.view, this.model).actionPerformed(null);
  }

  @Test
  public void clearsActiveRulesFromModel() {
    this.activeRules.forEach(rule -> verify(this.model, times(1)).removeActiveRule(rule));
  }

  @Test
  public void updatesActiveRulesInView() {
    verify(this.configEditor, times(1)).setActiveRules(any(List.class));;
  }
}