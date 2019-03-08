package org.infernus.idea.checkstyle.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.infernus.idea.checkstyle.listeners.AttributeSubmissionListener;
import org.infernus.idea.checkstyle.model.ConfigRule;
import org.infernus.idea.checkstyle.model.PropertyMetadata;
import org.infernus.idea.checkstyle.model.XMLConfig;
import org.infernus.idea.checkstyle.util.CheckStyleRuleProvider;
import org.infernus.idea.checkstyle.util.PropertyValueValidator;

/**
 * This class represents the Attributes Editor Dialog that displays the
 * attributes associated with a given CheckStyle rule and allows the user to set
 * them.
 */
public class CheckAttributesEditorDialog extends ConfigGeneratorWindow {
  private static final long serialVersionUID = 13L;

  /** The center panel that displays attribute names and values */
  private JPanel centerPanel;
  /** The label that displays the name of the current rule */
  private JLabel nameLabel;
  /** The label that displays the description of the current rule */
  private JLabel descLabel;
  /** The "OK" button */
  private JButton okBtn;
  /** The "Cancel/Delete" button */
  private JButton cancelBtn;

  /** The state (attribute values) of the current rule */
  private XMLConfig xmlRule;
  /**
   * Whether or not this dialog was displayed for a rule that is already active
   */
  private boolean isNewRule;

  /**
   * Utility class to get data about rules, set by the controller and used for
   * attribute validation
   */
  private CheckStyleRuleProvider provider;

  /**
   * The listeners that have been registered with the "OK" and "Cancel/Delete"
   * buttons
   */
  private final Collection<AttributeSubmissionListener> submissionListeners = new ArrayList<>();

  /**
   * Sets the title and sizes of this window, initializes the JComponent fields,
   * adds the name and description labels to the window, as well as the "OK" and
   * "Cancel/Delete" buttons (via createBottomRow())
   */
  protected void createWindowContent() {
    setTitle("Attributes Editor");
    setMinimumSize(new Dimension(600, 400));
    setMaximumSize(new Dimension(1920, 1080));
    setResizable(false);

    //////// Field Initialization ///////////////
    this.centerPanel = new JPanel();
    this.nameLabel = new JLabel();
    this.descLabel = new JLabel();
    this.okBtn = new JButton("OK");
    this.cancelBtn = new JButton();
    /////////////////////////////////////////////

    this.centerPanel.setLayout(new GridLayout(0, 2));
    this.centerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    JPanel topRow = new JPanel(new BorderLayout());
    Font font = this.nameLabel.getFont();
    this.nameLabel.setFont(new Font(font.getName(), Font.BOLD, 48));
    topRow.add(this.nameLabel, BorderLayout.NORTH);
    topRow.add(this.descLabel, BorderLayout.SOUTH);
    topRow.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(topRow, BorderLayout.NORTH);
    getContentPane().add(this.centerPanel, BorderLayout.CENTER);
    getContentPane().add(createBottomRow(), BorderLayout.SOUTH);
  }

  /**
   * Adds click listeners to both bottom-row buttons and adds them to a horizontal
   * JPanel
   * 
   * @return A horizontally-aligned panel containing the "OK" and "Cancel/Delete"
   *         buttons
   */
  protected JPanel createBottomRow() {
    JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.TRAILING));

    bottomRow.add(Box.createHorizontalStrut(4));
    this.okBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        submissionListeners.forEach(sl -> sl.attributeSubmitted(xmlRule, isNewRule));
        setVisible(false);
        xmlRule = null;
      }
    });
    bottomRow.add(this.okBtn);
    bottomRow.add(Box.createHorizontalStrut(4));
    this.cancelBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        submissionListeners.forEach(sl -> sl.attributeCancelled(xmlRule, isNewRule));
        setVisible(false);
        xmlRule = null;
      }
    });
    bottomRow.add(this.cancelBtn);
    bottomRow.add(Box.createHorizontalGlue());

    return bottomRow;
  }

  /**
   * Displays the attributes editor window for <code>rule</code>. Call this
   * function if <code>rule</code> is not active (i.e. part of the current
   * configuration)
   * 
   * @param rule The rule to display the attributes for
   */
  public void displayForCheck(ConfigRule rule) {
    displayForCheck(rule, null);
  }

  /**
   * Displays the attributes editor window for
   * <code>rule</code>/<code>config</code>. If <code>config</code> is null,
   * <code>rule</code> represents a non-active rule
   * 
   * @param rule   The rule to display the attributes for
   * @param config The present state of the rule (if it is active, some attributes
   *               may have already been set)
   */
  public void displayForCheck(ConfigRule rule, XMLConfig config) {
    this.okBtn.setEnabled(true);
    this.isNewRule = config == null;

    this.xmlRule = this.isNewRule ? new XMLConfig(rule.getRuleName()) : config;
    this.nameLabel.setText(rule.getRuleName());
    this.descLabel.setText(rule.getRuleDescription());
    this.cancelBtn.setText(this.isNewRule ? "Cancel" : "Delete");

    this.centerPanel.removeAll();
    List<PropertyMetadata> properties = new ArrayList<>(rule.getParameters().values());
    Collections.sort(properties);
    for (PropertyMetadata entry : properties) {
      String attr = entry.getName();

      JLabel label = new JLabel(camelToTitle(attr) + ": ");
      label.setHorizontalAlignment(SwingConstants.LEFT);
      label.setToolTipText(entry.getDescription());
      this.centerPanel.add(label);

      // Use getInputComponent() to determine what input component to display for this
      // attribute
      // Pass null if this is a non-active rule or if this attribute has not been set
      // yet
      this.centerPanel.add(
              getInputComponent(entry, this.isNewRule || !config.isAttributeSet(attr) ? null : config.getAttribute(attr)));
    }

    super.arrange();
    super.display();
  }

  /**
   * Determines what input component is appropriate for the given rule type and
   * provides it
   * 
   * @param prop  The attribute for which to get an input component
   * @param value The current value of <code>prop</code> for this rule or null if
   *              <code>prop</code> has not been set
   * @return A JComboBox, JSpinner, JCheckBox, or JTextField to serve as the input
   *         for <code>prop</code>
   */
  protected JComponent getInputComponent(PropertyMetadata prop, String value) {
    Set<String> typeOptions = null;
    if (provider != null) {
      typeOptions = provider.getTypeOptions(prop.getType());
    }

    if (typeOptions != null) {
      return generateCombo(prop, value, typeOptions);
    } else if (prop.getType().equals("Integer")) {
      return generateSpinner(prop, value);
    } else if (prop.getType().equals("Boolean")) {
      return generateCheckBox(prop, value);
    } else {
      return generateTextField(prop, value);
    }
  }

  /**
   * Creates a JComboBox for attributes that can take on one of a finite set of
   * values.
   * 
   * @param prop        The attribute for which to generate a JComboBox
   * @param value       The current value of the attribute (or null if not set)
   * @param typeOptions All of the possible options this attribute can have as its
   *                    value
   * @return The combo box to use as an input component
   */
  protected JComboBox<String> generateCombo(PropertyMetadata prop, String value, Set<String> typeOptions) {
    String[] options = typeOptions.toArray(new String[typeOptions.size()]);
    Arrays.sort(options);
    JComboBox<String> combo = new JComboBox<>(new DefaultComboBoxModel<String>(options));

    // Allow user to not set a value (and thus use CheckStyle's default value for
    // this attribute)
    combo.insertItemAt("", 0);
    combo.setSelectedIndex(0);
    if (value != null) {
      // Select the option corresponding to the current value
      for (int i = 0; i < options.length; i++) {
        if (value.equals(options[i])) {
          combo.setSelectedIndex(i + 1);
        }
      }
    }
    combo.setToolTipText("Type: " + prop.getType());

    combo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String val = combo.getSelectedItem().toString();
        if (val.length() == 0) {
          // Don't define a value for this attribute if the user selected the empty option
          xmlRule.removeAttribute(prop.getName());
        } else {
          xmlRule.addAttribute(prop.getName(), combo.getSelectedItem().toString());
        }
      }
    });

    return combo;
  }

  /**
   * Creates a JSpinner for attributes that can only take on integer values
   * 
   * @param prop  The attribute for which to generate a JSpinner
   * @param value The current value of the attribute (or null if not set)
   * @return The spinner to be used as an input component
   */
  protected JSpinner generateSpinner(PropertyMetadata prop, String value) {
    // Figure out what to set the spinner to at the start
    int start;
    try {
      if (value != null) {
        start = Integer.parseInt(value);
      } else {
        start = 0;
      }
    } catch (NumberFormatException ex) {
      start = 0;
    }

    SpinnerModel spinnerModel = new SpinnerNumberModel(start, Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
    JSpinner spinner = new JSpinner(spinnerModel);
    spinner.setToolTipText("Default: " + prop.getDefaultValue());

    spinner.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        xmlRule.addAttribute(prop.getName(), spinner.getValue().toString());
      }
    });

    return spinner;
  }

  /**
   * Creates a JCheckBox for attributes that are either "true" or "false" (Boolean
   * attributes)
   * 
   * @param prop  The attribute for which to generate a JCheckBox
   * @param value The current value of the attribute (or null if not set)
   * @return The check box to be used as an input component
   */
  protected JCheckBox generateCheckBox(PropertyMetadata prop, String value) {
    JCheckBox checkBox = new JCheckBox();
    if (value != null) {
      checkBox.setSelected(Boolean.parseBoolean(value));
    } else {
      checkBox.setSelected(Boolean.parseBoolean(prop.getDefaultValue()));
    }

    checkBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        xmlRule.addAttribute(prop.getName(), String.valueOf(checkBox.isSelected()));
      }
    });

    return checkBox;
  }

  /**
   * Creates a JTextField for String attributes or any other attributes more
   * complex than can be represented by a combo box, spinner, or check box
   * 
   * @param prop  The attribute for which to generate a JTextField
   * @param value The current value of the attribute (or null if not set)
   * @return A panel containing a JTextField (for input) and a JLabel (to display
   *         validation errors if/when they apply)
   */
  protected JPanel generateTextField(PropertyMetadata prop, String value) {
    JPanel panel = new JPanel(new BorderLayout());
    JTextField textField = new JTextField(20);
    // For error reporting
    JLabel label = new JLabel();

    panel.add(textField);
    panel.add(label, BorderLayout.SOUTH);
    label.setForeground(Color.RED);

    if (value != null) {
      textField.setText(value);
    }
    if (prop.getType().equals("String")) {
      // Don't validate if the type is a simple string
      textField.setToolTipText("Default: " + prop.getDefaultValue());
      textField.getDocument().addDocumentListener(new DocumentListener() {
        @Override
        public void removeUpdate(DocumentEvent e) {
          handleChange();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
          handleChange();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
          handleChange();
        }

        private void handleChange() {
          xmlRule.addAttribute(prop.getName(), textField.getText());
        }
      });
    } else {
      // If the value is more complex than a simple String, use PropertyValueValidator
      // to validate the input
      textField.setToolTipText("Type: " + prop.getType() + "\nDefault: " + prop.getDefaultValue());
      textField.getDocument().addDocumentListener(new DocumentListener() {
        @Override
        public void removeUpdate(DocumentEvent e) {
          handleChange();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
          handleChange();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
          handleChange();
        }

        private void handleChange() {
          String error = PropertyValueValidator.validate(prop.getType(), textField.getText());
          if (error == null) {
            // If the input is valid, enable the "OK" button and remove any error text right
            // away (on keystroke)
            okBtn.setEnabled(true);
            label.setText("");
            arrange();
            xmlRule.addAttribute(prop.getName(), textField.getText());
          } else {
            // If input is invalid, don't update the attribute and don't allow the user to
            // press "OK"
            okBtn.setEnabled(false);
          }
        }
      });
      textField.addFocusListener(new FocusListener() {
        @Override
        public void focusLost(FocusEvent e) {
          // If the user de-focuses the text field and the input is invalid, display an
          // error message
          String error = PropertyValueValidator.validate(prop.getType(), textField.getText());
          if (error != null) {
            label.setText(error);
            arrange();
          }
        }

        @Override
        public void focusGained(FocusEvent e) {
        }
      });
    }

    return panel;
  }

  /**
   * Registers a listener with the "OK" and "Cancel/Delete" buttons
   * 
   * @param asl The listener to notify when the user clicks "OK" or
   *            "Cancel/Delete"
   */
  public void addSubmitListener(AttributeSubmissionListener asl) {
    this.submissionListeners.add(asl);
  }

  /**
   * Sets the rule provider instance for this window (intended to be used by the
   * controller)
   * 
   * @param provider The rule provider instance to set to
   */
  public void setRuleProvider(CheckStyleRuleProvider provider) {
    this.provider = provider;
  }
}
