package org.infernus.idea.checkstyle.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;

/**
 * This class represents a listener that responds to a click of the "Clear"
 * button and clears the active rules.
 */
public class ClearButtonListener extends ConfigGeneratorListener implements ActionListener {
  /**
   * Initializes a ClearButtonListener
   * 
   * @param view  The view this listener is attached to
   * @param model The model that represents the data for this instance of the GUI
   */
  public ClearButtonListener(ConfigGeneratorView view, ConfigGeneratorModel model) {
    this.view = view;
    this.model = model;
  }

  /**
   * Clears the active rules
   * 
   * @param e The event that triggered this function
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    this.model.getActiveRules().forEach(rule -> this.model.removeActiveRule(rule));
    updateActiveRules();
  }
}
