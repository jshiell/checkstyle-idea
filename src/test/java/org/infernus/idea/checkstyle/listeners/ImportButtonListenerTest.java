package org.infernus.idea.checkstyle.listeners;

import static org.mockito.Mockito.*;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;
import org.infernus.idea.checkstyle.ui.ImportConfigDialog;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ImportButtonListenerTest {
  @Mock
  ConfigGeneratorView view;
  @Mock
  ImportConfigDialog configDialog;

  @Mock
  ConfigGeneratorModel model;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(this.view.getConfigDialog()).thenReturn(this.configDialog);
    new ImportButtonListener(this.view, this.model).actionPerformed(null);
  }

  @Test
  public void getsConfigNames() {
    verify(this.model, times(1)).getConfigNames();
  }

  @Test
  public void displaysConfigDialog() {
    verify(this.configDialog, times(1)).display(this.model.getConfigNames());
  }
}