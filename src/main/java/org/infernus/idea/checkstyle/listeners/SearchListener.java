package org.infernus.idea.checkstyle.listeners;

/**
 * This class represents a listener that gets notified whenever a keystroke in a
 * search bar is performed.
 */
public interface SearchListener {
  /**
   * The action to perform when a keystroke is captured.
   * 
   * @param query The present contents of the search bar
   */
  public void searchPerformed(String query);
}
