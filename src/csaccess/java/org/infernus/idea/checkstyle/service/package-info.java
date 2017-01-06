/**
 * The Checkstyle Service Layer, or just the "service layer". The Checkstyle tool itself, which means the classes in
 * {@code com.puppycrawl}, are available in this package and its subpackages only. All classes of the service layer
 * are loaded by a custom classloader which is discarded and rebuilt whenever the user of the Checkstyle-IDEA plugin
 * selects a new Checkstyle version. This is required so that the plugin can support multiple Checkstyle versions.
 * <p>This layer must be kept as thin as absolutely possible. Test coverage must be very high in order to make sure
 * that breaking changes in the Checkstyle API are detected and handled.</p>
 */
package org.infernus.idea.checkstyle.service;
