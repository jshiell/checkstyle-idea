package org.infernus.idea.checkstyle.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;

/**
 * This class represents a listener that responds to a click of the "Generate"
 * button and exports the active configuration to an XML file.
 */
public class GenerateButtonListener extends ConfigGeneratorListener implements ActionListener {
  /**
   * Initializes a GenerateButtonListener
   * 
   * @param view  The view this listener is attached to
   * @param model The model that represents the data for this instance of the GUI
   */
  public GenerateButtonListener(ConfigGeneratorView view, ConfigGeneratorModel model) {
    this.view = view;
    this.model = model;
  }

  /**
   * Exports the active configuration with the name in the text field
   * 
   * @param e The event that triggered this function
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    try {
      model.generateConfig(this.view.getConfigEditor().getConfigurationName());
    } catch (IOException ex) {
      System.out.println("\n\n\n" + ex.getClass() + " - " + ex.getLocalizedMessage() + "\n\n\n");
    }
  }
}
