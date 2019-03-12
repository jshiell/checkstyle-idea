package org.infernus.idea.checkstyle.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.model.ConfigRule;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;

/**
 * This class represents a listener that responds to a keystroke in the global
 * search bar at the top of the Configuration Editor window and filters the
 * visible rules by the query.
 */
public class SearchBarListener extends ConfigGeneratorListener implements SearchListener {
  /**
   * Initializes a SearchBarListener
   * 
   * @param view  The view this listener is attached to
   * @param model The model that represents the data for this instance of the GUI
   */
  public SearchBarListener(ConfigGeneratorView view, ConfigGeneratorModel model) {
    this.view = view;
    this.model = model;
  }

  /**
   * Filters the visible rules by <code>query</code>
   * 
   * @param query The current contents of the search bar
   */
  @Override
  public void searchPerformed(String query) {
    List<ConfigRule> filteredRules = new ArrayList<>();
    this.model.getAvailableRules().values().forEach(ruleLst -> {
      filteredRules.addAll(ruleLst.stream().filter(rule -> {
        return (rule.getRuleName() != null && rule.getRuleName().toLowerCase().contains(query.toLowerCase()))
            || (rule.getRuleDescription() != null
                && rule.getRuleDescription().toLowerCase().contains(query.toLowerCase()));
      }).collect(Collectors.toList()));
    });
    this.view.getConfigEditor().setVisibleRules("Search Results", filteredRules);
  }
}