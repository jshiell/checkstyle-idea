package org.infernus.idea.checkstyle.model;

import java.util.HashSet;
import java.util.Set;

public class ConfigRule {

    /** The name of the rule */
    private String ruleName;

    /** The description of the rule */
    private String ruleDescription;

    /** The set of parameters */
    private Set<String> parameters;


    /**
     * Creates a new ConfigRule, with the rule's name as the name of it's java file,
     * its description as the first comment within "<p><p/>" tags, and parameters as
     * the name of the parameters needed to initialize the rule
     *
     * @param path the path to the rule's corresponding java file
     */
    public ConfigRule(String path) {
        String filename = path.substring(path.lastIndexOf('/') + 1);
        this.ruleName = filename.substring(0, filename.indexOf('.'));
        this.parameters = parseParameters(path);
        this.ruleDescription = parseDescription(path);
    }

    /**
     * Parses the java file for the given rule and the description of the rule
     *
     * @param path the path to the rule's corresponding java file
     * @return the description of the rule
     */
    private String parseDescription(String path) {
        return null;
    }

    /**
     * Parses the java file for the given rule and returns a set of names
     * of parameters needed to initialize the rule
     *
     * @param path the path to the rule's corresponding java file
     * @return
     */
    private Set<String> parseParameters(String path) {
        return null;
    }

    /**
     * Returns the name of the rule
     *
     * @return the name of the rule
     */
    public String getRuleName() {
        return ruleName;
    }

    /**
     * Returns the description of what the rule checks for
     *
     * @return the description for the rule
     */
    public String getRuleDescription() {
        return ruleDescription;
    }

    /**
     * Returns the set of names of parameters needed to initialize
     * the rule
     *
     * @return a set of names of parameters needed to initialize
     * the rule
     */
    public Set<String> getParameters() {
        return new HashSet<>(parameters);
    }
}
