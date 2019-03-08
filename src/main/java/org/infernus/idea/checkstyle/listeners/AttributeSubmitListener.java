package org.infernus.idea.checkstyle.listeners;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.model.XMLConfig;
import org.infernus.idea.checkstyle.listeners.AttributeSubmissionListener;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;

/**
 * This class represents a listener that responds to clicks of the "OK" or
 * "Cancel/Delete" buttons in the attributes editor window and adds or removes
 * the corresponding XMLConfigs.
 */
public class AttributeSubmitListener extends ConfigGeneratorListener implements AttributeSubmissionListener {
  /**
   * Initializes an AttributeSubmissionListener
   * 
   * @param view  The view this listener is attached to
   * @param model The model that represents the data for this instance of the GUI
   */
  public AttributeSubmitListener(ConfigGeneratorView view, ConfigGeneratorModel model) {
    this.view = view;
    this.model = model;
  }

  /**
   * Adds xmlRule to the active rules.
   * 
   * @param xmlRule   The rule that was submitted
   * @param isNewRule Whether or not xmlRule was previously active
   */
  @Override
  public void attributeSubmitted(XMLConfig xmlRule, boolean isNewRule) {
    if (isNewRule) {
      this.model.addActiveRule(xmlRule);
      updateActiveRules();
    }
  }

  /**
   * Removes xmlRule from the active rules.
   * 
   * @param xmlRule   The rule that was cancelled
   * @param isNewRule Whether or not xmlRule was previously active
   */
  @Override
  public void attributeCancelled(XMLConfig xmlRule, boolean isNewRule) {
    if (!isNewRule) {
      this.model.removeActiveRule(xmlRule);
      updateActiveRules();
    }
  }
}
