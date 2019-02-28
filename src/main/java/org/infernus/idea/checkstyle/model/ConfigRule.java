package org.infernus.idea.checkstyle.model;

import java.util.*;

/**
 * An ADT to represent a CheckStyle rule
 */
public class ConfigRule {
    /** The name of the rule */
    private String ruleName;
    /** The description of the rule */
    private String ruleDescription;
    private String category;

    /**
     * Stores the available properties of the rule. The key is the
     * name of the property, and the value will be a ADT that stores
     * the metadata of the property.
     */
    private Map<String, PropertyMetadata> parameters;

    /**
     * Creates a new ConfigRule, with the rule's name
     * @param name - The name of this rule
     */
    public ConfigRule(String name) {
        this.ruleName = name;
        this.parameters = new HashMap<String, PropertyMetadata>();
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
     * Gets the category name of this rule
     * @return The category name of this rule
     */
    public String getCategory() {
        return this.category;
    }

    /***
     * Sets the category to category.
     * @param category - The category name of this rule
     */
    public void setCategory(String category) {
         this.category = category;
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
    public Map<String, PropertyMetadata> getParameters() {
        return new HashMap<String, PropertyMetadata>(this.parameters);
    }

    /***
     * Adds the parameter for this rule
     * @param name - The name of the properties
     * @param metadata - The information of the property.
     */
    public void addParameter(String name, PropertyMetadata metadata) {
        this.parameters.put(name, metadata);
    }
}
