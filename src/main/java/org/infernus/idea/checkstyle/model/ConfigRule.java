package org.infernus.idea.checkstyle.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * An ADT to represent a CheckStyle rule
 */
public class ConfigRule {
    /** The name of the rule */
    private String ruleName;

    /** The description of the rule */
    private String ruleDescription;

    /** The set of parameters */
    private Map<String, String> parameters;

    /**
     * Creates a new ConfigRule, with the rule's name as the name of it's java file,
     * its description as the first multiline comment, and parameters as
     * the name of the parameters needed to initialize the rule
     *
     * @param path the path to the rule's corresponding java file
     * @throws FileNotFoundException if the given filepath cannot be found
     */
    public ConfigRule(String path) throws IOException {
        String filename = path.substring(path.lastIndexOf('/') + 1);
        this.ruleName = filename.substring(0, filename.indexOf('.'));
        this.parameters = parseParameters(path);
        this.ruleDescription = parseDescription(path);
    }

    /**
     * Parses the java file for the given rule and the description of the rule
     *
     * @param path the path to the rule's corresponding java file
     * @return the description of the rule, null if the file does not contain a description
     * @throws FileNotFoundException if the given filepath cannot be found
     */
    private String parseDescription(String path) throws IOException {
        String description = "";
        File ruleFile = new File(path);
        Scanner readFile = new Scanner(ruleFile);
        boolean foundDescription = false;
        while (readFile.hasNextLine()) {
            String line = readFile.nextLine();
            if (!foundDescription) {
                if (line.contains("/**")) {
                    foundDescription = true;
                }
            } else {
                if (line.contains("*/")) {
                    return description;
                }
                if (!line.contains("<")) {
                    description += line + "\n";
                }
            }
        }
        return null;
    }

    /**
     * Parses the java file for the given rule and returns a set of names
     * of parameters needed to initialize the rule
     *
     * @param path the path to the rule's corresponding java file
     * @return a set of names of parameters needed to initialize
     * the rule
     * @throws FileNotFoundException if the given filepath cannot be found
     */
    private Map<String, String> parseParameters(String path) throws FileNotFoundException {
        File ruleFile = new File(path);
        Scanner readFile = new Scanner(ruleFile);
        Map<String, String> params = new HashMap<>();
        while (readFile.hasNextLine()) {
            String line = readFile.nextLine();
            if (line.contains("public void set")) {
                String name = line.replaceFirst("public void set", "");
                System.out.println(name.substring(0, name.lastIndexOf("(")));
                params.put(name.substring(0, name.lastIndexOf("(")).trim(), "");
            }
        }
        readFile = new Scanner(ruleFile);
        boolean foundDescription = false;
        boolean addToMap = false;
        String description = "";

        while (readFile.hasNextLine()) {
            String line = readFile.nextLine();
            if (addToMap) {
                for (String param : params.keySet()) {
                    if (line.toLowerCase().contains(param.toLowerCase())) {
                        params.put(param, description);
                        description = "";
                        addToMap = false;
                        break;
                    }
                }
            }
            if (!foundDescription) {
                if (line.contains("/**")) {
                    foundDescription = true;
                }
            } else {
                if (line.contains("*/")) {
                    addToMap = true;
                    foundDescription = false;
                }
                if (!line.contains("<")) {
                    description += line + "\n";
                }
            }
        }


        return params;
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
    public Map<String, String> getParameters() {
        return new HashMap<>(parameters);
    }
}
