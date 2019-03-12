package org.infernus.idea.checkstyle.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * This class represents the window that notifies the user that the generation
 * of their configuration was successful.
 */
public class ExportSuccessfulDialog extends ConfigGeneratorWindow {
  private static final long serialVersionUID = 19L;

  /** The label displaying the notification */
  JLabel centerLabel;

  /**
   * Sets the title and size of the window, adds a combo box to the center panel,
   * and adds the "OK" and "Cancel" buttons (via createBottomRow())
   */
  protected void createWindowContent() {
    setTitle("Generation Successful");
    setMinimumSize(IMPORT_MIN_SIZE);

    JPanel centerPanel = new JPanel(new GridLayout(0, 1));
    centerPanel.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));

    this.centerLabel = new JLabel();

    centerPanel.add(this.centerLabel);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(centerPanel, BorderLayout.CENTER);
    getContentPane().add(createBottomRow(), BorderLayout.SOUTH);
  }

  /**
   * Creates a horiontal JPanel containing an "OK" button
   * 
   * @return The panel to be used as the bottom row of the dialog
   */
  protected JPanel createBottomRow() {
    JPanel bottomRow = new JPanel(new BorderLayout());

    JButton okBtn = new JButton("OK");
    okBtn.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e) {
        setVisible(false);
      }
    });
    bottomRow.add(okBtn, BorderLayout.EAST);

    return bottomRow;
  }

  /**
   * Displays the dialog with <code>configNames</code> as the values in the combo
   * box
   * 
   * @param configNames The names of Configurations that can be imported
   */
  public void display(String configName) {
    this.centerLabel.setText("<html>" + configName + " was successfully generated! <br />"
        + "You can find it in .idea/configs/" + configName + ".xml</html>");

    super.arrange();
    super.display();
  }
}
