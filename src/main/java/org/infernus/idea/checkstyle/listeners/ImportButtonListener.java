package org.infernus.idea.checkstyle.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;

/**
 * This class represents a listener that responds to a click of the "Import"
 * button and displays the configuration selection dialog.
 */
public class ImportButtonListener extends ConfigGeneratorListener implements ActionListener {
  /**
   * Initializes an ImportButtonListener
   * 
   * @param view  The view this listener is attached to
   * @param model The model that represents the data for this instance of the GUI
   */
  public ImportButtonListener(ConfigGeneratorView view, ConfigGeneratorModel model) {
    this.view = view;
    this.model = model;
  }

  /**
   * Displays the configuration selection dialog
   * 
   * @param e The event that triggered this function
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    this.view.getConfigDialog().display(this.model.getConfigNames());
  }
}