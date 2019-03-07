package org.infernus.idea.checkstyle.listeners;

import java.util.stream.Collectors;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;

public class ImportConfigListener implements ImportSubmitListener {
  ConfigGeneratorView view;
  ConfigGeneratorModel model;

  public ImportConfigListener(ConfigGeneratorView view, ConfigGeneratorModel model) {
    this.view = view;
    this.model = model;
  }

  @Override
  public void configSubmitted(String configName) {
    try {
      this.model.importConfig(configName);
      this.view.getConfigEditor().setConfigurationName(configName);
    } catch (Exception ex) {
      System.out.println("\n\n\n" + ex.getClass() + " - " + ex.getLocalizedMessage() + "\n\n\n");
    }
    this.view.getConfigEditor().setActiveRules(this.model.getActiveRules().stream()
        .map(rule -> this.model.getConfigRuleforXML(rule)).collect(Collectors.toList()));
  }
}