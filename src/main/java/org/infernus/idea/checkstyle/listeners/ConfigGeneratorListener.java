package org.infernus.idea.checkstyle.listeners;

import java.util.stream.Collectors;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;

/**
 * This class defines common fields and functions for all listeners that belong
 * to the ConfigGenerator GUI
 */
public class ConfigGeneratorListener {
  /** The view of the Checkstyle Configuration GUI */
  protected ConfigGeneratorView view;
  /** The model of the Checkstyle Configuration GUI, handles the data */
  protected ConfigGeneratorModel model;

  /**
   * Updates the active rules in the Configuration Editor Window with those
   * presently in the model
   */
  protected void updateActiveRules() {
    this.view.getConfigEditor().setActiveRules(this.model.getActiveRules().stream()
        .map(rule -> this.model.getConfigRuleforXML(rule)).collect(Collectors.toList()));
  }
}