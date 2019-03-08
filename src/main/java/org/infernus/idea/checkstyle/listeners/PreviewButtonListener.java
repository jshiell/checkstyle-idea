package org.infernus.idea.checkstyle.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.infernus.idea.checkstyle.model.ConfigGeneratorModel;
import org.infernus.idea.checkstyle.ui.ConfigGeneratorView;

/**
 * This class represents a listener that responds to a click of the "Preview"
 * button and opens the XML Preview dialog to display a preview of the current
 * configuration state.
 */
public class PreviewButtonListener extends ConfigGeneratorListener implements ActionListener {
  /**
   * Initializes a PreviewButtonListener
   * 
   * @param view  The view this listener is attached to
   * @param model The model that represents the data for this instance of the GUI
   */
  public PreviewButtonListener(ConfigGeneratorView view, ConfigGeneratorModel model) {
    this.view = view;
    this.model = model;
  }

  /**
   * Opens an XML Preview dialog and displays a preview of the current
   * configuration's XML (provided by the model)
   * 
   * @param e The event that triggered this function
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    this.view.getPreviewDialog().display(model.getPreview());
  }
}
