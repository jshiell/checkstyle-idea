package org.infernus.idea.checkstyle.listeners;

import java.awt.event.MouseEvent;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;

/**
 * This class represents a listener that responds to a click in the "Category"
 * selection pane and updates the visible rules in the top-right panel.
 */
public class CategorySelectListener extends SelectListener {
  /**
   * Initializes a CategorySelectListener
   * 
   * @param view  The view this listener is attached to
   * @param model The model that represents the data for this instance of the GUI
   */
  public CategorySelectListener(ConfigGeneratorView view, ConfigGeneratorModel model) {
    this.view = view;
    this.model = model;
  }

  /**
   * Sets the rules in the top-right panel to just the ones under the selected
   * category.
   * 
   * @param e The event that triggered this function
   */
  @Override
  public void mouseClicked(MouseEvent e) {
    String category = this.view.getConfigEditor().getSelectedCategory();
    this.view.getConfigEditor().setVisibleRules(category, this.model.getAvailableRules().get(category));
  }
}
