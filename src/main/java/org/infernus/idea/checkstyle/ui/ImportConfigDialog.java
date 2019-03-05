package org.infernus.idea.checkstyle.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
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

  private JPanel centerPanel;
  private JComboBox<String> combo;

  private Collection<ImportSubmitListener> submissionListeners = new ArrayList<>();

  /**
   * 
   */
  protected void createWindowContent() {
    setTitle("Import Configuration");
    setMinimumSize(new Dimension(900, 100));

    this.centerPanel = new JPanel(new GridLayout(0, 1));
    this.centerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    this.combo = new JComboBox<>(new DefaultComboBoxModel<String>());

    this.centerPanel.add(this.combo);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(this.centerPanel, BorderLayout.CENTER);
    getContentPane().add(createBottomRow(), BorderLayout.SOUTH);
  }

  protected JPanel createBottomRow() {
    JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.TRAILING));

    bottomRow.add(Box.createHorizontalStrut(4));
    JButton okBtn = new JButton("OK");
    okBtn.addActionListener(new ActionListener(){
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
   * 
   */
  public void display(Collection<String> configNames) {
    this.combo.removeAllItems();
    for (String name : configNames) {
      this.combo.addItem(name);
    }

    super.arrange();
    super.display();
  }

  public void addSubmitListener(ImportSubmitListener isl) {
    this.submissionListeners.add(isl);
  }
}
