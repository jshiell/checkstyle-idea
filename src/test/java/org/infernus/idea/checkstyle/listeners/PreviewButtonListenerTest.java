package org.infernus.idea.checkstyle.listeners;

import static org.mockito.Mockito.*;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;
import org.infernus.idea.checkstyle.ui.XMLPreviewDialog;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PreviewButtonListenerTest {
  @Mock
  ConfigGeneratorView view;
  @Mock
  XMLPreviewDialog previewDialog;

  @Mock
  ConfigGeneratorModel model;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(this.view.getPreviewDialog()).thenReturn(this.previewDialog);
    when(this.model.getPreview()).thenReturn("foobarbaz");
    new PreviewButtonListener(this.view, this.model).actionPerformed(null);
  }

  @Test
  public void opensPreviewDialogWithPreviewText() {
    verify(this.previewDialog, times(1)).display("foobarbaz");
  }
}