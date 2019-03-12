package org.infernus.idea.checkstyle.listeners;

/**
 * This class represents a listener that gets notified whenever the "OK" button
 * on the "Import Configuration" dialog is pressed.
 */
public interface ImportSubmitListener {
  /**
   * The action to perform when the Import Configuration dialog is submitted.
   * 
   * @param configName The configuration that was selected
   */
  public void configSubmitted(String configName);
}
