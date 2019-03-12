package org.infernus.idea.checkstyle.listeners;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;

/**
 * This class represents a listener that responds to submission of the "Import
 * Configuration" dialog and adds all of the rules from the selected
 * configuration to "Active Rules".
 */
public class ImportConfigListener extends ConfigGeneratorListener implements ImportSubmitListener {
  /**
   * Initializes an ImportConfigListener
   * 
   * @param view  The view this listener is attached to
   * @param model The model that represents the data for this instance of the GUI
   */
  public ImportConfigListener(ConfigGeneratorView view, ConfigGeneratorModel model) {
    this.view = view;
    this.model = model;
  }

  /**
   * Loads the Configuration associated with configName into the model, sets the
   * "Configuration Name" text field to configName, and updates the "Active Rules"
   * panel with the rules of the imported configuration
   * 
   * @param configName The name of the configuration to import
   */
  @Override
  public void configSubmitted(String configName) {
    try {
      this.model.importConfig(configName);
      this.view.getConfigEditor().setConfigurationName(configName);
    } catch (Exception ex) {
      System.out.println("\n\n\n" + ex.getClass() + " - " + ex.getLocalizedMessage() + "\n\n\n");
    }
    updateActiveRules();
  }
}