package org.infernus.idea.checkstyle;

import org.infernus.idea.checkstyle.listeners.*;
import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;
import org.infernus.idea.checkstyle.util.ConfigurationListeners;

/**
 * This class represents the controller for the Checkstyle Configuration GUI. It
 * handles reponse to user input and wires up the View to the Model.
 */
public class ConfigGeneratorController {
  /** The model of the Checkstyle Configuration GUI, handles the data */
  private final ConfigGeneratorModel model;
  /** The view of the Checkstyle Configuration GUI */
  private final ConfigGeneratorView view;

  /**
   * Instantiates a new ConfigGeneratorController by setting the categories in the
   * view and adding the required listeners.
   * 
   * @param project The current IntelliJ IDEA project
   */
  public ConfigGeneratorController(ConfigGeneratorView view, ConfigGeneratorModel model) {
    this.view = view;
    this.model = model;
    this.view.getConfigEditor().setCategories(this.model.getAvailableRules().keySet());
    addListeners();
  }

  /**
   * Adds button and selection listeners to the view
   */
  protected void addListeners() {
    this.view.getConfigEditor().addButtonListener(new ImportButtonListener(this.view, this.model),
        ConfigurationListeners.IMPORT_BUTTON_LISTENER);
    this.view.getConfigEditor().addButtonListener(new PreviewButtonListener(this.view, this.model),
        ConfigurationListeners.PREVIEW_BUTTON_LISTENER);
    this.view.getConfigEditor().addButtonListener(new GenerateButtonListener(this.view, this.model),
        ConfigurationListeners.GENERATE_BUTTON_LISTENER);

    this.view.getConfigEditor().addSelectionListener(new CategorySelectListener(this.view, this.model),
        ConfigurationListeners.CATEGORY_SELECT_LISTENER);
    this.view.getConfigEditor().addSelectionListener(new VisibleRuleSelectListener(this.view),
        ConfigurationListeners.VISIBLE_RULES_SELECT_LISTENER);
    this.view.getConfigEditor().addSelectionListener(new ActiveRuleSelectListener(this.view, this.model),
        ConfigurationListeners.ACTIVE_RULES_SELECT_LISTENER);

    this.view.getAttrEditor().addSubmitListener(new AttributeSubmitListener(this.view, this.model));

    this.view.getConfigDialog().addSubmitListener(new ImportConfigListener(this.view, this.model));
  }

  /**
   * Displays the main Checkstyle Configuration GUI window
   */
  public void displayConfigEditor() {
    this.view.getConfigEditor().setVisible(true);
  }
}
