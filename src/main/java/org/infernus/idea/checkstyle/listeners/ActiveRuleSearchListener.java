package org.infernus.idea.checkstyle.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.model.ConfigRule;
import org.infernus.idea.checkstyle.model.XMLConfig;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;

/**
 * This class represents a listener that responds to a keystroke in the active
 * rules search bar and filters the active rules by the query.
 */
public class ActiveRuleSearchListener extends ConfigGeneratorListener implements SearchListener {
  /**
   * Initializes a ActiveRuleSearchListener
   * 
   * @param view  The view this listener is attached to
   * @param model The model that represents the data for this instance of the GUI
   */
  public ActiveRuleSearchListener(ConfigGeneratorView view, ConfigGeneratorModel model) {
    this.view = view;
    this.model = model;
  }

  /**
   * Filters the active rules by <code>query</code>
   * 
   * @param query The current contents of the search bar
   */
  public void searchPerformed(String query) {
    List<ConfigRule> filteredRules = this.model.getActiveRules().stream().filter(xmlRule -> {
      boolean containsQuery = false;
      for (String attrName : xmlRule.getAttributeNames()) {
        String attrValue = xmlRule.getAttribute(attrName);
        if (attrName != null && attrName.contains(query)) {
          containsQuery = true;
        } else if (attrValue != null && attrValue.contains(query)) {
          containsQuery = true;
        }
      }

      ConfigRule configRule = this.model.getConfigRuleforXML(xmlRule);
      System.out.println(configRule);
      if (configRule.getRuleName() != null && configRule.getRuleName().contains(query)) {
        containsQuery = true;
      } else if (configRule.getRuleDescription() != null && configRule.getRuleDescription().contains(query)) {
        containsQuery = true;
      }

      return containsQuery;
    }).map(xmlRule -> this.model.getConfigRuleforXML(xmlRule)).collect(Collectors.toList());

    this.view.getConfigEditor().setActiveRules(filteredRules);
  }
}