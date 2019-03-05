package org.infernus.idea.checkstyle.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;

public class PreviewButtonListener implements ActionListener {
  private ConfigGeneratorView view;
  private ConfigGeneratorModel model;

  public PreviewButtonListener(ConfigGeneratorView view, ConfigGeneratorModel model) {
    this.view = view;
    this.model = model;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    this.view.getPreviewDialog().display(model.getPreview());
  }
}
