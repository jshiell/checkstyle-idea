package org.infernus.idea.checkstyle.listeners;

import java.awt.event.MouseEvent;

import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;

/**
 * This class represents a listener that responds to a click in the "Visible
 * Rules" (top-right) selection pane and updates displays an Attributes Editor
 * window for the selected rule.
 */
public class VisibleRuleSelectListener extends SelectListener {
  /**
   * Initializes a VisibleRuleSelectListener
   * 
   * @param view The view this listener is attached to
   */
  public VisibleRuleSelectListener(ConfigGeneratorView view) {
    this.view = view;
  }

  /**
   * Displays an Attributes Editor window for the currently-selected rule in the
   * "Visible Rules" panel
   * 
   * @param e The event that triggered this function
   */
  @Override
  public void mouseClicked(MouseEvent e) {
    this.view.getAttrEditor().displayForCheck(this.view.getConfigEditor().getSelectedVisibleRule());
  }
}
