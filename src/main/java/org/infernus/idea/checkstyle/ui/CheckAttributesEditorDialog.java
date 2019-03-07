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

  /**
   * The center panel that displays attribute names and values
   */
  private JPanel centerPanel;
  private JLabel nameLabel;
  private JLabel descLabel;
  private JButton okBtn;
  private JButton cancelBtn;

  private XMLConfig xmlRule;
  private boolean isNewRule;

  private CheckStyleRuleProvider provider;

  /**
   * The listeners that have been registered with the "OK" button
   */
  private final Collection<AttributeSubmissionListener> submissionListeners = new ArrayList<>();

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

  public void displayForCheck(ConfigRule rule) {
    displayForCheck(rule, null);
  }

  public void displayForCheck(ConfigRule rule, XMLConfig config) {
    this.okBtn.setEnabled(true);
    this.isNewRule = config == null;

    this.xmlRule = this.isNewRule ? new XMLConfig(rule.getRuleName()) : config;
    this.nameLabel.setText(rule.getRuleName());
    this.descLabel.setText(rule.getRuleDescription());
    this.cancelBtn.setText(this.isNewRule ? "Cancel" : "Delete");

    this.centerPanel.removeAll();
    for (PropertyMetadata entry : rule.getParameters().values()) {
      String attr = entry.getName();

      JLabel label = new JLabel(camelToTitle(attr) + ": ");
      label.setHorizontalAlignment(SwingConstants.LEFT);
      label.setToolTipText(entry.getDescription());
      this.centerPanel.add(label);

      this.centerPanel.add(
          getInputComponent(entry, this.isNewRule || !config.isAttributeSet(attr) ? null : config.getAttribute(attr)));
    }

    super.arrange();
    super.display();
  }

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

  protected JComboBox<String> generateCombo(PropertyMetadata prop, String value, Set<String> typeOptions) {
    String[] options = typeOptions.toArray(new String[typeOptions.size()]);
    Arrays.sort(options);
    JComboBox<String> combo = new JComboBox<>(new DefaultComboBoxModel<String>(options));
    combo.insertItemAt("", 0);
    combo.setSelectedIndex(0);
    if (value != null) {
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
          xmlRule.removeAttribute(prop.getName());
        } else {
          xmlRule.addAttribute(prop.getName(), combo.getSelectedItem().toString());
        }
      }
    });

    return combo;
  }

  protected JSpinner generateSpinner(PropertyMetadata prop, String value) {
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

  protected JPanel generateTextField(PropertyMetadata prop, String value) {
    JPanel panel = new JPanel(new BorderLayout());
    JTextField textField = new JTextField(20);
    JLabel label = new JLabel();

    panel.add(textField);
    panel.add(label, BorderLayout.SOUTH);
    label.setForeground(Color.RED);
    // Font font = label.getFont();
    // label.setFont(new Font(font.getFontName(), ));

    if (value != null) {
      textField.setText(value);
    }
    if (prop.getType().equals("String")) {
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
            okBtn.setEnabled(true);
            label.setText("");
            arrange();
            xmlRule.addAttribute(prop.getName(), textField.getText());
          } else {
            okBtn.setEnabled(false);
          }
        }
      });
      textField.addFocusListener(new FocusListener() {
        @Override
        public void focusLost(FocusEvent e) {
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

  public void addSubmitListener(AttributeSubmissionListener asl) {
    this.submissionListeners.add(asl);
  }

  public void setRuleProvider(CheckStyleRuleProvider provider) {
    this.provider = provider;
  }
}
