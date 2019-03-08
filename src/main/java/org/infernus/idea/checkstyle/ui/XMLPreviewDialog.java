package org.infernus.idea.checkstyle.ui;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * This class represents the window that displays a preview of the XML
 * Configuration that will be generated.
 */
public class XMLPreviewDialog extends ConfigGeneratorWindow {
  private static final long serialVersionUID = 31L;

  /**
   * The text area that displays the XML string
   */
  private JTextArea textArea;

  /**
   * Sets the text area as uneditable, gives it a border, and adds it to the frame
   */
  protected void createWindowContent() {
    setTitle("XML Preview");
    this.textArea = new JTextArea();
    this.textArea.setEditable(false);
    this.textArea.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));

    JScrollPane scrollPane = new JScrollPane(textArea);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

    setMaximumSize(PREVIEW_MAX_SIZE);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(scrollPane, BorderLayout.CENTER);
  }

  /**
   * Opens the preview window with xml in the text area
   * 
   * @param xml The xml preview
   */
  public void display(String xml) {
    this.textArea.setLineWrap(false);
    this.textArea.setText(xml);

    super.arrange();
    this.textArea.setLineWrap(true);
    super.display();
  }
}
