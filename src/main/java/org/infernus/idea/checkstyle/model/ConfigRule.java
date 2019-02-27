package org.infernus.idea.checkstyle.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * An ADT to represent a CheckStyle rule
 */
public class ConfigRule {
    private String ruleName;
    /** The description of the rule */
    private String ruleDescription;
    private String catagory;

    /**
     * Stores the available properties of the rule. The key is the
     * name of the property, and the value will be a map containing
     * the property information. Including the type and default in
     * key.
     */
    private Map<String, Map<String, String>> parameters;

    /**
     * Creates a new ConfigRule, with the rule's name
     * @param name - The name of this rule
     */
    public ConfigRule(String name) {
        this.ruleName = name;
        this.parameters = new HashMap<String, Map<String, String>>();
    }

    /**
     * Returns the name of the rule
     *
     * @return the name of the rule
     */
    public String getRuleName() {
        return this.ruleName;
    }

    /***
     * Sets the rule name to name.
     * @param name - The name to set this ConfigRule to.
     */
    public void setRuleName(String name) {
        this.ruleName = name;
    }

    /***
     * Gets the catagory name of this rule
     * @return The catagory name of this rule
     */
    public String getCatagory() {
        return this.catagory;
    }

    /***
     * Sets the catagory to catagory.
     * @param catagory - The catagory name of this rule
     */
    public void setCatagory(String catagory) {
         this.catagory = catagory;
    }

    /**
     * Returns the description of what the rule checks for
     *
     * @return the description for the rule
     */
    public String getRuleDescription() {
        return this.ruleDescription;
    }

    /***
     * Sets the rule description
     * @param description - The description of this rule
     */
    public void setRuleDescription(String description) {
        this.ruleDescription = description;
    }

    /**
     * Returns the set of names of parameters needed to initialize
     * the rule
     *
     * @return a set of names of parameters needed to initialize
     * the rule
     */
    public Map<String, Map<String, String>> getParameters() {
        return new HashMap<>(this.parameters);
    }

    /***
     * Adds the parameter for this rule
     * @param name - The name of the properties
     * @param info - The information of the type. For example, key
     *               would be the "type" and key would be the actual type name.
     */
    public void addParameter(String name, Map<String, String> info) {
        this.parameters.put(name, info);
    }
}
