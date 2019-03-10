package org.infernus.idea.checkstyle.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.infernus.idea.checkstyle.listeners.ImportSubmitListener;

/**
 * This class represents the window that allows the user to select an existing
 * config to import.
 */
public class ImportConfigDialog extends ConfigGeneratorWindow {
  private static final long serialVersionUID = 19L;

  /**
   * The combo box containining the list of Configurations that can be imported
   */
  private JComboBox<String> combo;
  /**
   * Listeners registered with the "OK" button
   */
  private Collection<ImportSubmitListener> submissionListeners = new ArrayList<>();

  /**
   * Sets the title and size of the window, adds a combo box to the center panel,
   * and adds the "OK" and "Cancel" buttons (via createBottomRow())
   */
  protected void createWindowContent() {
    setTitle("Import Configuration");
    setMinimumSize(IMPORT_MIN_SIZE);

    JPanel centerPanel = new JPanel(new GridLayout(0, 1));
    centerPanel.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));

    this.combo = new JComboBox<>(new DefaultComboBoxModel<String>());

    centerPanel.add(this.combo);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(centerPanel, BorderLayout.CENTER);
    getContentPane().add(createBottomRow(), BorderLayout.SOUTH);
  }

  /**
   * Creates a horiontal JPanel containing "OK" and "Cancel" buttons
   * 
   * @return The panel to be used as the bottom row of the dialog
   */
  protected JPanel createBottomRow() {
    JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.TRAILING));

    bottomRow.add(Box.createHorizontalStrut(4));
    JButton okBtn = new JButton("OK");
    okBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        submissionListeners.forEach(sl -> sl.configSubmitted((String) combo.getSelectedItem()));
        setVisible(false);
      }
    });
    bottomRow.add(okBtn);
    bottomRow.add(Box.createHorizontalStrut(4));
    JButton cancelBtn = new JButton("Cancel");
    cancelBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setVisible(false);
      }
    });
    bottomRow.add(cancelBtn);
    bottomRow.add(Box.createHorizontalGlue());

    return bottomRow;
  }

  /**
   * Displays the dialog with <code>configNames</code> as the values in the combo
   * box
   * 
   * @param configNames The names of Configurations that can be imported
   */
  public void display(Collection<String> configNames) {
    this.combo.removeAllItems();
    for (String name : configNames) {
      this.combo.addItem(name);
    }

    super.arrange();
    super.display();
  }

  /**
   * Register a listener with the "OK" button
   */
  public void addSubmitListener(ImportSubmitListener isl) {
    this.submissionListeners.add(isl);
  }
}
