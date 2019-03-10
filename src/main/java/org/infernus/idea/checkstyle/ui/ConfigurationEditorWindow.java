package org.infernus.idea.checkstyle.ui;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.intellij.ui.JBSplitter;

import org.infernus.idea.checkstyle.listeners.SelectListener;
import org.infernus.idea.checkstyle.model.ConfigRule;
import org.infernus.idea.checkstyle.util.ConfigurationListeners;

/**
 * This class represents the main Configuration Editor Window. Its layout is
 * divided into a "Top Row", a "Category Panel", a "Visible Rules Panel", an
 * "Active Rules Panel", and a "Bottom Row". The Top Row has buttons for
 * importing XML configurations. The Bottom Row has a text field for defining a
 * Configuration name and buttons for previewing and generating XML
 * configurations. Additionally, there are lists of checks and their categories,
 * and the checks attached to the current configuration in each of their
 * corresponding panels.
 */
public class ConfigurationEditorWindow extends ConfigGeneratorWindow {
  private static final long serialVersionUID = 17L;

  /** The text field that holds the user-inputted name of the configuration. */
  private JTextField configNameField;
  /** The label in the "Visible Rules" panel. */
  private JLabel categoryLabel;

  /** The list of selectable categories in the "Categories" panel. */
  private JList<String> categoryList;
  /** The list of selectable rules in the "Visible Rules" panel. */
  private List<ConfigRule> visibleRules;
  /** The list of selectable rules in the "Visible Rules" panel. */
  private JList<ConfigRule> visibleRulesList;
  /** The list of selectable rules in the "Active Rules" panel. */
  private JList<ConfigRule> activeRulesList;

  /** The listeners that have been registered with the "Import" button. */
  private Collection<ActionListener> importBtnListeners;
  /** The listeners that have been registered with the "Clear" button. */
  private Collection<ActionListener> clearBtnListeners;
  /** The listeners that have been registered with the "Preview" button. */
  private Collection<ActionListener> previewBtnListeners;
  /** The listeners that have been registered with the "Generate" button. */
  private Collection<ActionListener> generateBtnListeners;
  /** The listeners that have been registered with "Category" list */
  private Collection<MouseListener> categorySelectListeners;
  /** The listeners that have been registered with the "Visible Rules" list */
  private Collection<MouseListener> visibleRulesSelectListeners;
  /** The listeners that have been registered with the "Active Rules" list */
  private Collection<MouseListener> activeRulesSelectListeners;

  /**
   * This method builds out the basic structure of the editor window with split
   * panes. It utilizes <code>createCategoryPanel()</code>,
   * <code>createVisibleRulesPanel()</code>,
   * <code>createActiveRulesPanel()</code>, <code>createTopRow()</code>, and
   * <code>createBottomRow()</code> to populate the content of each layout
   * portion.
   */
  protected void createWindowContent() {
    setTitle("Configuration Editor");
    setMinimumSize(EDITOR_MIN_SIZE);

    //////// Field Initialization ///////////////
    this.configNameField = new JTextField(20);
    this.categoryLabel = new JLabel();

    this.categoryList = new JList<>();
    this.visibleRulesList = new JList<>();
    this.activeRulesList = new JList<>();

    this.importBtnListeners = new ArrayList<>();
    this.clearBtnListeners = new ArrayList<>();
    this.previewBtnListeners = new ArrayList<>();
    this.generateBtnListeners = new ArrayList<>();
    this.categorySelectListeners = new ArrayList<>();
    this.visibleRulesSelectListeners = new ArrayList<>();
    this.activeRulesSelectListeners = new ArrayList<>();
    /////////////////////////////////////////////

    // Horizontal split pane
    JBSplitter window = new JBSplitter(false);
    // Vertical split pane
    JBSplitter rightPanel = new JBSplitter(true);

    rightPanel.setFirstComponent(createVisibleRulesPanel());
    rightPanel.setSecondComponent(createActiveRulesPanel());

    window.setFirstComponent(createSelectCategoryPanel());
    window.setSecondComponent(rightPanel);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(createTopRow(), BorderLayout.NORTH);
    getContentPane().add(window, BorderLayout.CENTER);
    getContentPane().add(createBottomRow(), BorderLayout.SOUTH);
  }

  /**
   * Creates the content for the top row of the window. This includes the "Import"
   * button.
   * 
   * @return The top row of the editor window.
   */
  protected JPanel createTopRow() {
    JPanel topRow = new JPanel(new BorderLayout());
    topRow.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));

    JButton importBtn = new JButton("Import");
    importBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        importBtnListeners.forEach(ibl -> ibl.actionPerformed(e));
      }
    });
    topRow.add(importBtn, BorderLayout.WEST);

    JTextField search = new JTextField();
    search.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void removeUpdate(DocumentEvent e) {
        handlechange();
      }

      @Override
      public void insertUpdate(DocumentEvent e) {
        handlechange();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        handlechange();
      }

      private void handlechange() {
        List<ConfigRule> lst = visibleRules.stream()
            .filter(cr -> cr.getRuleName().toLowerCase().contains(search.getText().toLowerCase()))
            .collect(Collectors.toList());
        visibleRulesList.setListData(lst.toArray(new ConfigRule[lst.size()]));
      }
    });
    topRow.add(search);

    JButton clearBtn = new JButton("Clear");
    clearBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        clearBtnListeners.forEach(cbl -> cbl.actionPerformed(e));
      }
    });
    topRow.add(clearBtn, BorderLayout.EAST);

    topRow.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
    return topRow;
  }

  /**
   * Creates the content for the bottom row of the window. This includes the
   * "Configuration Name" text field and the "Preview" and "Generate" buttons.
   * 
   * @return The bottom row of the editor window.
   */
  protected JPanel createBottomRow() {
    JPanel bottomRow = new JPanel(new BorderLayout());
    bottomRow.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
    JPanel bottomRight = new JPanel(new FlowLayout(FlowLayout.LEADING));

    bottomRow.add(new JLabel("Configuration Name: "), BorderLayout.WEST);
    bottomRow.add(this.configNameField);
    bottomRight.add(Box.createHorizontalStrut(4));

    JButton previewBtn = new JButton("Preview");
    previewBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        previewBtnListeners.forEach(pbl -> pbl.actionPerformed(e));
      }
    });
    bottomRight.add(previewBtn);
    bottomRight.add(Box.createHorizontalStrut(4));

    JButton generateBtn = new JButton("Generate");
    generateBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        generateBtnListeners.forEach(gbl -> gbl.actionPerformed(e));
      }
    });
    bottomRight.add(generateBtn);
    bottomRow.add(bottomRight, BorderLayout.EAST);

    bottomRow.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
    return bottomRow;
  }

  /**
   * Creates the content for the "Category" Panel of the window.
   * 
   * @return The category panel of the editor window.
   */
  protected JPanel createSelectCategoryPanel() {
    JPanel panel = new JPanel(new BorderLayout());

    this.categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.categoryList.setLayoutOrientation(JList.VERTICAL);
    this.categoryList.addMouseListener(new SelectListener() {
      @Override
      public void mouseClicked(MouseEvent e) {
        categorySelectListeners.forEach(csl -> csl.mouseClicked(e));
      }
    });

    JScrollPane scrollPane = new JScrollPane(this.categoryList);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setMinimumSize(new Dimension(SCROLLPANE_WIDTH/2, 2*SCROLLPANE_HEIGHT));
    scrollPane.setPreferredSize(new Dimension(SCROLLPANE_WIDTH/2, 2*SCROLLPANE_HEIGHT));

    panel.add(scrollPane, BorderLayout.CENTER);
    panel.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
    return panel;
  }

  /**
   * Creates the "Visible Rules" Panel of the window.
   * 
   * @return The visible rules panel of the editor window.
   */
  protected JPanel createVisibleRulesPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEADING));

    Font font = this.categoryLabel.getFont();
    this.categoryLabel.setFont(new Font(font.getName(), Font.BOLD, HEADER_FONT_SIZE));

    topRow.add(Box.createHorizontalStrut(4));
    topRow.add(this.categoryLabel);
    topRow.add(Box.createHorizontalStrut(4));

    this.visibleRulesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.visibleRulesList.setLayoutOrientation(JList.VERTICAL);
    this.visibleRulesList.addMouseMotionListener(new MouseMotionListener() {
      @Override
      public void mouseMoved(MouseEvent e) {
        int index = visibleRulesList.locationToIndex(e.getPoint());
        if (index > -1) {
          visibleRulesList.setToolTipText(visibleRulesList.getModel().getElementAt(index).getRuleDescription());
        } else {
          visibleRulesList.setToolTipText("");
        }
      }

      @Override
      public void mouseDragged(MouseEvent e) {
      }
    });
    this.visibleRulesList.addMouseListener(new SelectListener() {
      @Override
      public void mouseClicked(MouseEvent e) {
        visibleRulesSelectListeners.forEach(vrsl -> vrsl.mouseClicked(e));
      }
    });

    JScrollPane scrollPane = new JScrollPane(this.visibleRulesList);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setMinimumSize(new Dimension(SCROLLPANE_WIDTH, SCROLLPANE_HEIGHT));
    scrollPane.setPreferredSize(new Dimension(SCROLLPANE_WIDTH, SCROLLPANE_HEIGHT));

    panel.add(topRow, BorderLayout.NORTH);
    panel.add(scrollPane, BorderLayout.CENTER);
    panel.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
    return panel;
  }

  /**
   * Creates the "Active Rules" panel of the window.
   * 
   * @return The active rules panel of the editor window.
   */
  protected JPanel createActiveRulesPanel() {
    JPanel bottomPanel = new JPanel(new BorderLayout());
    JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEADING));

    JLabel activeLabel = new JLabel("Active Rules");
    Font font = activeLabel.getFont();
    activeLabel.setFont(new Font(font.getName(), Font.BOLD, HEADER_FONT_SIZE));

    topRow.add(Box.createHorizontalStrut(4));
    topRow.add(activeLabel);
    topRow.add(Box.createHorizontalStrut(4));

    this.activeRulesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.activeRulesList.setLayoutOrientation(JList.VERTICAL);
    this.activeRulesList.addMouseMotionListener(new MouseMotionListener() {
      @Override
      public void mouseMoved(MouseEvent e) {
        int index = activeRulesList.locationToIndex(e.getPoint());
        if (index > -1) {
          activeRulesList.setToolTipText(activeRulesList.getModel().getElementAt(index).getRuleDescription());
        } else {
          activeRulesList.setToolTipText("");
        }
      }

      @Override
      public void mouseDragged(MouseEvent e) {
      }
    });
    this.activeRulesList.addMouseListener(new SelectListener() {
      @Override
      public void mouseClicked(MouseEvent e) {
        activeRulesSelectListeners.forEach(arsl -> arsl.mouseClicked(e));
      }
    });

    JScrollPane scrollPane = new JScrollPane(this.activeRulesList);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setMinimumSize(new Dimension(SCROLLPANE_WIDTH, SCROLLPANE_HEIGHT));
    scrollPane.setPreferredSize(new Dimension(SCROLLPANE_WIDTH, SCROLLPANE_HEIGHT));

    bottomPanel.add(topRow, BorderLayout.NORTH);
    bottomPanel.add(scrollPane, BorderLayout.CENTER);
    bottomPanel.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
    return bottomPanel;
  }

  /**
   * Sets the categories to display in the Category panel.
   * 
   * @param categories The categories to set
   */
  public void setCategories(Collection<String> categories) {
    this.categoryList.setListData(categories.toArray(new String[categories.size()]));
  }

  /**
   * Gets the currently-selected category in the Category panel.
   * 
   * @return The currently-selected category
   */
  public String getSelectedCategory() {
    return this.categoryList.getSelectedValue();
  }

  /**
   * Sets the rules to display in the Visible Rules panel.
   * 
   * @param category The category with which <code>rules</code> are associated
   * @param rules    The rules to display
   */
  public void setVisibleRules(String category, Collection<ConfigRule> rules) {
    this.categoryLabel.setText(category);
    this.visibleRules = new ArrayList<>(rules);
    Collections.sort(this.visibleRules);
    this.visibleRulesList.setListData(this.visibleRules.toArray(new ConfigRule[rules.size()]));
  }

  /**
   * Gets the currently-selected rule in the Visible Rules panel.
   * 
   * @return The currently-selected visible rule
   */
  public ConfigRule getSelectedVisibleRule() {
    return this.visibleRulesList.getSelectedValue();
  }

  /**
   * Sets the rules to display in the Active Rules panel.
   * 
   * @param rules The rules to display
   */
  public void setActiveRules(Collection<ConfigRule> rules) {
    this.activeRulesList.setListData(rules.toArray(new ConfigRule[rules.size()]));
  }

  /**
   * Gets the currently-selected rules in the Active Rules panel.
   * 
   * @return The currently-selected active rule
   */
  public ConfigRule getSelectedActiveRule() {
    return this.activeRulesList.getSelectedValue();
  }

  public int getSelectedActiveIndex() {
    return this.activeRulesList.getSelectedIndex();
  }

  /**
   * Sets the text in the Configuration Name text field.
   * 
   * @param name The text to set the field to
   */
  public void setConfigurationName(String name) {
    this.configNameField.setText(name);
  }

  /**
   * Gets the value typed into the Configuration Name text field.
   * 
   * @return The name of this configuration
   */
  public String getConfigurationName() {
    return this.configNameField.getText();
  }

  /**
   * Registers a listener for one of the buttons in the editor window (Import,
   * Clear, Preview, Generate).
   * 
   * @param al     The listener to register
   * @param button The button with which to register <code>al</code>
   */
  public void addButtonListener(ActionListener al, ConfigurationListeners button) {
    switch (button) {
    case IMPORT_BUTTON_LISTENER:
      this.importBtnListeners.add(al);
      break;
    case CLEAR_BUTTON_LISTENER:
      this.clearBtnListeners.add(al);
      break;
    case PREVIEW_BUTTON_LISTENER:
      this.previewBtnListeners.add(al);
      break;
    case GENERATE_BUTTON_LISTENER:
      this.generateBtnListeners.add(al);
      break;
    default:
      break;
    }
  }

  /**
   * Registers a listener for one of the selection lists in the editor window
   * (Categories, Visible Rules, Active Rules).
   * 
   * @param ml            The listener to register
   * @param selectionList The selection list with which to register
   *                      <code>lsl</code>
   */
  public void addSelectionListener(MouseListener ml, ConfigurationListeners selectionList) {
    switch (selectionList) {
    case CATEGORY_SELECT_LISTENER:
      this.categorySelectListeners.add(ml);
      break;
    case VISIBLE_RULES_SELECT_LISTENER:
      this.visibleRulesSelectListeners.add(ml);
      break;
    case ACTIVE_RULES_SELECT_LISTENER:
      this.activeRulesSelectListeners.add(ml);
      break;
    default:
      break;
    }
  }
}
