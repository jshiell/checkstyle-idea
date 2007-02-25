package org.infernus.idea.checkstyle;

import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;

import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * A manager for CheckStyle plug-in configuration.
 *
 * @author James Shiell
 * @version 1.0
 */
public final class CheckStyleConfiguration extends Properties
        implements JDOMExternalizable {

    /**
     * The CP location of the default CheckStyle configuration.
     */
    public static final String DEFAULT_CONFIG = "/sun_checks.xml";

    /**
     * The CheckStyle file path.
     */
    public static final String CONFIG_FILE = "config-file";

    /**
     * {@inheritDoc}
     */
    public void readExternal(final Element element)
            throws InvalidDataException {
        clear();

        if (element == null) {
            return;
        }

        for (Element propertyElement : (List<Element>)
                element.getChildren("property")) {
            final String propertyName = propertyElement.getAttributeValue("name");
            final String propertyValue = propertyElement.getText();

            if (propertyName != null) {
                put(propertyName, propertyValue);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(final Element element)
            throws WriteExternalException {
        if (element == null) {
            throw new IllegalArgumentException("Element may not be null.");
        }

        for (final Enumeration e = propertyNames(); e.hasMoreElements();) {
            final String propertyName = (String) e.nextElement();
            final String propertyValue = getProperty(propertyName);

            final Element propertyElement = new Element("property");
            propertyElement.setAttribute("name", propertyName);
            propertyElement.setText(propertyValue);

            element.addContent(propertyElement);
        }
    }
}
