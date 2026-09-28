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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * verifyPlugin resolves classes but does not check that parentId matches a registered id - a typo here
 * would silently misplace a page in the Settings tree rather than fail the build. This test guards the
 * id/parentId wiring that nests both Checkstyle settings pages under the new group parent.
 */
class CheckStyleSettingsHierarchyTest {

    private static final String GROUP_ID = "org.infernus.idea.checkstyle.group";

    @Test
    void checkstyleGroupIsNestedUnderTools() throws Exception {
        final Element group = configurableWithInstance("org.infernus.idea.checkstyle.CheckStyleGroupConfigurable");

        assertEquals(GROUP_ID, group.getAttribute("id"));
        assertEquals("tools", group.getAttribute("parentId"));
    }

    @Test
    void projectPageIsNestedUnderTheCheckstyleGroup() throws Exception {
        final Element projectPage = configurableWithInstance("org.infernus.idea.checkstyle.CheckStyleConfigurable");

        assertEquals(GROUP_ID, projectPage.getAttribute("parentId"));
    }

    @Test
    void globalPageIsNestedUnderTheCheckstyleGroup() throws Exception {
        final Element globalPage = configurableWithInstance("org.infernus.idea.checkstyle.CheckStyleApplicationConfigurable");

        assertEquals(GROUP_ID, globalPage.getAttribute("parentId"));
    }

    private static Element configurableWithInstance(final String instanceClass) throws Exception {
        final Element root = parse("/META-INF/plugin.xml").getDocumentElement();

        final Optional<Element> match = toElementList(root.getElementsByTagName("*")).stream()
                .filter(element -> instanceClass.equals(element.getAttribute("instance")))
                .findFirst();

        assertTrue(match.isPresent(), "No configurable registered with instance=" + instanceClass);
        return match.get();
    }

    private static Document parse(final String resourcePath) throws Exception {
        try (InputStream stream = CheckStyleSettingsHierarchyTest.class.getResourceAsStream(resourcePath)) {
            assertNotNull(stream, "Resource not found on classpath: " + resourcePath);

            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            return factory.newDocumentBuilder().parse(stream);
        }
    }

    private static List<Element> toElementList(final NodeList nodes) {
        final List<Element> elements = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            elements.add((Element) nodes.item(i));
        }
        return elements;
    }
}
