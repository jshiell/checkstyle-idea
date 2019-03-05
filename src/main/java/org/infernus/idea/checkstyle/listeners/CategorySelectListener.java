package org.infernus.idea.checkstyle.listeners;

import java.awt.event.MouseEvent;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;

public class CategorySelectListener extends SelectListener {
  private ConfigGeneratorView view;
  private ConfigGeneratorModel model;

  public CategorySelectListener(ConfigGeneratorView view, ConfigGeneratorModel model) {
    this.view = view;
    this.model = model;
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    String category = this.view.getConfigEditor().getSelectedCategory();
    this.view.getConfigEditor().setVisibleRules(category, this.model.getAvailableRules().get(category));
  }
}
