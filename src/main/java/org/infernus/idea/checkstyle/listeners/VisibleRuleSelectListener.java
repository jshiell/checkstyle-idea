package org.infernus.idea.checkstyle.listeners;

import java.awt.event.MouseEvent;

import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;

public class VisibleRuleSelectListener extends SelectListener {
  ConfigGeneratorView view;

  public VisibleRuleSelectListener(ConfigGeneratorView view) {
    this.view = view;
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    this.view.getAttrEditor().displayForCheck(this.view.getConfigEditor().getSelectedVisibleRule());
  }
}
