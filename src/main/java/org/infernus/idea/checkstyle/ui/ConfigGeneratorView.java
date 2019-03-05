package org.infernus.idea.checkstyle.ui;

public class ConfigGeneratorView {
  private final ConfigurationEditorWindow configEditor = new ConfigurationEditorWindow();
  private final ImportConfigDialog configDialog = new ImportConfigDialog();
  private final CheckAttributesEditorDialog attrEditor = new CheckAttributesEditorDialog();
  private final XMLPreviewDialog previewDialog = new XMLPreviewDialog();

  public ConfigurationEditorWindow getConfigEditor() {
    return this.configEditor;
  }

  public ImportConfigDialog getConfigDialog() {
    return this.configDialog;
  }

  public CheckAttributesEditorDialog getAttrEditor() {
    return this.attrEditor;
  }

  public XMLPreviewDialog getPreviewDialog() {
    return this.previewDialog;
  }
}
