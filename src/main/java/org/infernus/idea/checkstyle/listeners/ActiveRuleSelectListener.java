package org.infernus.idea.checkstyle.listeners;

import java.awt.event.MouseEvent;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.model.ConfigRule;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;

/**
 * This class represents a listener that responds to a click in the "Active
 * Rules" selection pane and opens the attribute editor.
 */
public class ActiveRuleSelectListener extends SelectListener {
  /** The view of the Checkstyle Configuration GUI */
  private ConfigGeneratorView view;
  /** The model of the Checkstyle Configuration GUI, handles the data */
  private ConfigGeneratorModel model;

  /**
   * Initializes an ActiveRuleSelectListener
   * 
   * @param view  The view this listener is attached to
   * @param model The model that represents the data for this instance of the GUI
   */
  public ActiveRuleSelectListener(ConfigGeneratorView view, ConfigGeneratorModel model) {
    this.view = view;
    this.model = model;
  }

  /**
   * Displays the attributes editor window.
   * 
   * @param e The event that triggered this function
   */
  @Override
  public void mouseClicked(MouseEvent e) {
    ConfigRule selected = this.view.getConfigEditor().getSelectedActiveRule();
    if (selected != null) {
      this.view.getAttrEditor().displayForCheck(selected,
          this.model.getActiveRules().get(this.view.getConfigEditor().getSelectedActiveIndex()));
    }
  }
}
