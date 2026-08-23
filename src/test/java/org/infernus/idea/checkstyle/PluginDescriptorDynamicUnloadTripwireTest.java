package org.infernus.idea.checkstyle;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the conditions {@code DynamicPlugins.checkCanUnloadWithoutRestart} checks against our own
 * descriptors, so a future extension addition that would silently reintroduce a restart requirement
 * is caught here rather than discovered by a user. This cannot assert extension-point {@code dynamic}
 * status, which lives in the platform's own descriptors rather than ours.
 */
class PluginDescriptorDynamicUnloadTripwireTest {

    @Test
    void pluginXmlHasNoKnownDynamicUnloadBlockers() throws Exception {
        assertDescriptorHasNoKnownDynamicUnloadBlockers("/META-INF/plugin.xml");
    }

    @Test
    void mavenExtensionDescriptorHasNoKnownDynamicUnloadBlockers() throws Exception {
        assertDescriptorHasNoKnownDynamicUnloadBlockers("/META-INF/checkstyle-idea-maven.xml");
    }

    private static void assertDescriptorHasNoKnownDynamicUnloadBlockers(final String resourcePath) throws Exception {
        final Element root = parse(resourcePath).getDocumentElement();

        assertTrue(elementsNamed(root, "component").isEmpty(),
                resourcePath + ": <component> registrations are not dynamic-unload safe");

        assertTrue(elementsWithAttribute(root, "overrides").isEmpty(),
                resourcePath + ": service overrides= are not dynamic-unload safe");

        assertTrue(elementsWithAttribute(root, "use-idea-classloader").isEmpty(),
                resourcePath + ": use-idea-classloader is not dynamic-unload safe");

        assertTrue(elementsNamed(root, "reference").isEmpty(),
                resourcePath + ": <reference> action elements are not dynamic-unload safe");

        for (final Element group : elementsNamed(root, "group")) {
            assertTrue(group.hasAttribute("id"),
                    resourcePath + ": <group> without an id is not dynamic-unload safe");
            assertTrue(elementsNamed(group, "group").isEmpty(),
                    resourcePath + ": nested <group> is not dynamic-unload safe");
        }
    }

    private static Document parse(final String resourcePath) throws Exception {
        try (InputStream stream = PluginDescriptorDynamicUnloadTripwireTest.class.getResourceAsStream(resourcePath)) {
            assertNotNull(stream, "Resource not found on classpath: " + resourcePath);

            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            return factory.newDocumentBuilder().parse(stream);
        }
    }

    private static List<Element> elementsNamed(final Element root, final String tagName) {
        return toElementList(root.getElementsByTagName(tagName));
    }

    private static List<Element> elementsWithAttribute(final Element root, final String attributeName) {
        final List<Element> matches = new ArrayList<>();
        for (final Element element : toElementList(root.getElementsByTagName("*"))) {
            if (element.hasAttribute(attributeName)) {
                matches.add(element);
            }
        }
        return matches;
    }

    private static List<Element> toElementList(final NodeList nodes) {
        final List<Element> elements = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            elements.add((Element) nodes.item(i));
        }
        return elements;
    }
}
