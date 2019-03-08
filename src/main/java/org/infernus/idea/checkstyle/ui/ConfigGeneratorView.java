package org.infernus.idea.checkstyle.ui;

/**
 * This class is a wrapper around the different types of windows the GUI has, so
 * as to simplify the controller's interaction with them.
 */
public class ConfigGeneratorView {
  /** The Configuration Editor window instance for this view */
  private final ConfigurationEditorWindow configEditor = new ConfigurationEditorWindow();
  /** The Import Configuration dialog instance for this view */
  private final ImportConfigDialog configDialog = new ImportConfigDialog();
  /** The Attributes Editor window instance for this view */
  private final CheckAttributesEditorDialog attrEditor = new CheckAttributesEditorDialog();
  /** The XML Preview dialog instance for this view */
  private final XMLPreviewDialog previewDialog = new XMLPreviewDialog();

  /**
   * Get the Configuration Editor window instance
   * 
   * @return The Configuration Editor window instance for this view
   */
  public ConfigurationEditorWindow getConfigEditor() {
    return this.configEditor;
  }

  /**
   * Get the Import Configuration dialog instance
   * 
   * @return The Import Configuration dialog instance for this view
   */
  public ImportConfigDialog getConfigDialog() {
    return this.configDialog;
  }

  /**
   * Get the Attributes Editor window instance
   * 
   * @return The Attributes Editor window instance for this view
   */
  public CheckAttributesEditorDialog getAttrEditor() {
    return this.attrEditor;
  }

  /**
   * Get the XML Preview dialog instance
   * 
   * @return The XML Preview dialog instance for this view
   */
  public XMLPreviewDialog getPreviewDialog() {
    return this.previewDialog;
  }
}
