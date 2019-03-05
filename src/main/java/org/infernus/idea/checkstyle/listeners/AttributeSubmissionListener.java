package org.infernus.idea.checkstyle.listeners;
import org.infernus.idea.checkstyle.model.XMLConfig;

/**
 * This class represents a listener that gets notified whenever the Attributes
 * Editor Dialog is submitted.
 */
public interface AttributeSubmissionListener {
  /**
   * The action to perform on successful submission.
   * 
   * @param xmlRule The rule, along with attribute values, that was submitted
   * @param isNewRule Whether this rule is not already active
   */
  public void attributeSubmitted(XMLConfig xmlRule, boolean isNewRule);

  /**
   * The action to perform on cancel.
   * 
   * @param xmlRule The rule, along with attribute values, that was submitted
   * @param isNewRule Whether this rule is not already active
   */
  public void attributeCancelled(XMLConfig xmlRule, boolean isNewRule);
}
