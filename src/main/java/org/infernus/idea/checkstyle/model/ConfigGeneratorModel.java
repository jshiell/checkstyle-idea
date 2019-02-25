package org.infernus.idea.checkstyle.model;

import jdk.internal.org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ConfigGeneratorModel {
    /** The current state of the configuration */
    XMLConfig config;

    /** The path the the config will be saved to when it is generated */
    private String path;

    /** The name of the configuration file */
    String configName;

    /** The set of rules that are currently in the configuration */
    private Set<XMLConfig> activeRules;

    /** The set of possible rules a user can add to the configuration */
    private Set<XMLConfig> rules;

    /**
     * Creates a new ConfigGeneratorModel with a blank XML configuration, file name,
     * path to the file, and set of active rules
     */
    public ConfigGeneratorModel() {
        this.path = "";
        this.configName = "";
        this.config = new XMLConfig("Checker");
        this.activeRules = new HashSet<>();
        this.rules = new HashSet<>();
    }

    /**
     * Adds a new rule to the current configuration state
     *
     */
    public void addActiveRule() {

    }

    /**
     * Generates and saves the user-defined config to the given path
     *
     * @param path the filepath to save the generated configuration file to
     * @throws IllegalArgumentException - When the root module is not name "Checker"
     * @throws IllegalArgumentException - When the path is not saving to XML
     * @throws IllegalArgumentException - When the parent directory doesn't exist
     * @throws IOException - When file could not be created with the path
     */
    public void generateConfig(String path) {
        String filepath = path + configName;
        //Commented out until ConfigWriter is merged
        //ConfigWriter.saveConfig(config, filepath);
    }

    /**
     * Imports the state of an existing configuration file
     *
     * @param filePath the path to the XML configuration file to import
     * @throws FileNotFoundException - When the passed in file doesn’t exist.
     * @throws IllegalArgumentException - When the passed in file is not XML,
     *         or doesn’t have “Checker” as root module.
     * @throws ParserConfigurationException - DocumentFactory config error, please
     *         report when this error is thrown
     * @throws SAXException - When parsing error occur
     */
    public void importConfig(String filePath) {
        path = filePath.substring(0, filePath.lastIndexOf('/') + 1);
        configName = filePath.substring(filePath.lastIndexOf('/') + 1);
        //config = ConfigReader.readConfig(filePath);
    }

    /**
     * Sets the configuration file's name to 'name'
     *
     * @param name the new name for the configuration file
     */
    public void setConfigName(String name) {
        configName = name;
    }

    /**
     *
     * @return
     */
    public Set<XMLConfig> getActiveRules() {
        return new HashSet<>(activeRules);
    }

    /*
    /**
     *
     * @param ruleQuery
     * @return
     *
    public Set<String> searchRuleSet(String ruleQuery) {
        Set<String> queryResults = new HashSet<>();

        return queryResults;
    }
    */
}
