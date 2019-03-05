package org.infernus.idea.checkstyle.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;

public class GenerateButtonListener implements ActionListener {
  private ConfigGeneratorView view;
  private ConfigGeneratorModel model;

  public GenerateButtonListener(ConfigGeneratorView view, ConfigGeneratorModel model) {
    this.view = view;
    this.model = model;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    try {
      model.generateConfig(this.view.getConfigEditor().getConfigurationName());
    } catch (IOException ex) {
      System.out.println("\n\n\n" + ex.getClass() + " - " + ex.getLocalizedMessage() + "\n\n\n");
    }
  }
}
