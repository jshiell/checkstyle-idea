package org.infernus.idea.checkstyle.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;

public class ClearButtonListener implements ActionListener {
  private ConfigGeneratorView view;

  public ClearButtonListener(ConfigGeneratorView view) {
    this.view = view;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    this.view.getConfigEditor().setActiveRules(new ArrayList<>());
  }
}
