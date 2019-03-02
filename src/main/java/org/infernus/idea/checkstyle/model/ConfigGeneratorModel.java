package org.infernus.idea.checkstyle.model;

import org.infernus.idea.checkstyle.util.ConfigReader;
import org.infernus.idea.checkstyle.util.ConfigWriter;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class ConfigGeneratorModel {
    /** The current state of the configuration */
    XMLConfig config;

    /** The path the the config will be saved to when it is generated */
    private String path;

    /** The Map<String, ConfigRule> with the String being the name of a rule
     * mapping to the corresponding ConfigRule
     */
    private Map<String, ConfigRule> activeRules;

    /**
     * Creates a new ConfigGeneratorModel with a blank XML configuration, file name,
     * path to the file, and set of active rules
     */
    public ConfigGeneratorModel() {
        this.path = "";
        this.config = new XMLConfig("Checker");
        this.activeRules = new HashMap<>();
    }

    /**
     * Adds a new rule to the current configuration state
     *
     * @param rule the XMLConfig representation of the rule to be added
     *             to the configuration
     */
    public void addActiveRule(XMLConfig rule) {

    }

    /**
     * Generates and saves the user-defined config to the given path
     *
     * @param fileName the name to save the generated configuration file as
     * @throws IllegalArgumentException - When the root module is not name "Checker"
     * @throws IllegalArgumentException - When the path is not saving to XML
     * @throws IllegalArgumentException - When the parent directory doesn't exist
     * @throws IOException - When file could not be created with the path
     */
    public void generateConfig(String fileName) throws IOException {
        String filepath = path + fileName + ".xml";
        ConfigWriter.saveConfig(filepath, config);
    }

    /**
     * Imports the state of an existing configuration file
     *
     * @param fileName the name of the XML configuration file to import
     * @throws FileNotFoundException - When the passed in file doesn’t exist.
     * @throws IllegalArgumentException - When the passed in file is not XML,
     *         or doesn’t have “Checker” as root module.
     * @throws ParserConfigurationException - DocumentFactory config error, please
     *         report when this error is thrown
     */
    public void importConfig(String fileName) throws ParserConfigurationException, SAXException, IOException {
        config = ConfigReader.readConfig(path + fileName + ".xml");
    }

    /**
     * Returns a set of the active rules in the current configuration
     *
     * @return a Collection<XMLConfig> of all the active rules in the current
     *         configuration
     */
    public Collection<XMLConfig> getActiveRules() {
        return null;
    }

    /**
     * Returns the XML configuration representation for the given rule
     *
     * @param rule the rule to get the XML representation for
     * @return the XML format for the given rule
     */
    public ConfigRule getConfigRuleforXML(XMLConfig rule) {
        String ruleName = rule.getName();
        return null;
    }

    /**
     * Removes an active rule from the current configuration
     *
     * @param rule the rule to remove from the XML config
     */
    public void removeActiveRule(XMLConfig rule) {

    }

    /**
     * Returns information for all available rules that a user can
     * add to their configuration
     *
     * @return a Map<String, List<ConfigRule>> where the keys are Strings
     *         representing the rule categories, which map to Lists of
     *         ConfigRules, which contain all details for a given rule.
     */
    public TreeMap<String, List<ConfigRule>> getAvailableRules() {
        return null;
    }

    /**
     * Returns a string representation of what the current XML configuration
     * will look like.
     *
     * @return a string representation of what the current XML configuration
     *         will look like.
     */
    public String getPreview() {
        return "";
    }
}
