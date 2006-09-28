package org.infernus.idea.checkstyle;

import com.intellij.codeInspection.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import org.jdom.Element;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Inspection for CheckStyle integration for IntelliJ IDEA.
 *
 * @author James Shiell
 * @version 1.0
 */
public class CheckStyleInspection extends LocalInspectionTool {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(
            CheckStyleInspection.class);

    /**
     * IDEA filetype for Java files.
     */
    private static final FileType FILETYPE_JAVA
            = FileTypeManager.getInstance().getFileTypeByExtension("java");

    /**
     * The prefix of the temporary files.
     */
    private static final String TEMPFILE_NAME = "checkstyle-idea";

    /**
     * The extension of temporary files.
     */
    private static final String TEMPFILE_EXTENSION = ".java";

    /**
     * The CP location of the default CheckStyle configuration.
     */
    private static final String DEFAULT_CONFIG = "/sun_checks.xml";

    /**
     * The configuration panel for the inspection.
     */
    private final CheckStyleFilePanel configPanel = new CheckStyleFilePanel();
    private static final String CONFIG_FILEPATH_ELEMENT = "config-file";

    /**
     * Produce a CheckStyle checker.
     *
     * @return a checker.
     */
    public Checker getChecker() {
        try {
            final Checker checker;
            final File configFile = configPanel.getConfigFile();
            if (configFile == null) {
                final InputStream in = CheckStyleInspection.class.getResourceAsStream(
                        DEFAULT_CONFIG);
                checker = CheckerFactory.getInstance().getChecker(in);
                in.close();
            } else {
                checker = CheckerFactory.getInstance().getChecker(configFile);
            }

            return checker;

        } catch (Exception e) {
            LOG.error("Error", e);
            throw new RuntimeException("Couldn't create Checker", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    public JComponent createOptionsPanel() {
        return configPanel;
    }

    /**
     * {@inheritDoc}
     */
    public void readSettings(final Element element)
            throws InvalidDataException {
        final Element configPathElement = element.getChild(
                CONFIG_FILEPATH_ELEMENT);
        if (configPathElement != null) {
            final String filePath = configPathElement.getText();
            if (filePath != null && filePath.trim().length() > 0) {
                configPanel.setConfigFile(new File(filePath));
            } else {
                configPanel.setConfigFile(null);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeSettings(final Element element)
            throws WriteExternalException {
        final File configFile = configPanel.getConfigFile();
        if (configFile != null) {
            final Element configPathElement = new Element(
                    CONFIG_FILEPATH_ELEMENT);
            configPathElement.setText(configFile.getAbsolutePath());
            element.addContent(configPathElement);
        }
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    public String getGroupDisplayName() {
        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);
        return resources.getString("plugin.group");
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    public String getDisplayName() {
        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);
        return resources.getString("plugin.display-name");
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @NonNls
    public String getShortName() {
        return "CheckStyle-IDEA";
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    public ProblemDescriptor[] checkFile(@NotNull final PsiFile psiFile,
                                         @NotNull final InspectionManager manager,
                                         final boolean isOnTheFly) {
        if (!psiFile.isValid() || !psiFile.isPhysical()
                || !FILETYPE_JAVA.equals(psiFile.getFileType())) {
            return null;
        }

        File tempFile = null;
        try {
            final Checker checker = getChecker();

            // we need to copy to a file as IntelliJ may not have saved the file recently...
            tempFile = File.createTempFile(TEMPFILE_NAME, TEMPFILE_EXTENSION);
            final BufferedWriter tempFileOut = new BufferedWriter(
                    new FileWriter(tempFile));
            tempFileOut.write(psiFile.getText());
            tempFileOut.flush();
            tempFileOut.close();

            final CheckStyleAuditListener listener = new CheckStyleAuditListener(psiFile, manager);
            checker.addListener(listener);
            checker.process(new File[] {tempFile});
            checker.destroy();

            final List<ProblemDescriptor> problems = listener.getProblems();
            return problems.toArray(new ProblemDescriptor[problems.size()]);

        } catch (IOException e) {
            LOG.error("Failure when creating temp file", e);
            throw new RuntimeException("Couldn't create temp file", e);

        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Listener for the CheckStyle process.
     */
    protected class CheckStyleAuditListener implements AuditListener {

        private final PsiFile psiFile;
        private final InspectionManager manager;

        private List<AuditEvent> errors = new ArrayList<AuditEvent>();
        private List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();

        /**
         * Create a new listener.
         *
         * @param psiFile the file being checked.
         * @param manager the current inspection manager.
         */
        public CheckStyleAuditListener(final PsiFile psiFile,
                                       final InspectionManager manager) {
            this.psiFile = psiFile;
            this.manager = manager;
        }

        /**
         * {@inheritDoc}
         */
        public void auditStarted(final AuditEvent auditEvent) {
            errors.clear();
            problems.clear();
        }

        /**
         * {@inheritDoc}
         */
        public void auditFinished(final AuditEvent auditEvent) {
            final char[] text = psiFile.textToCharArray();

            // we cache the offset of each line as it is created, so as to
            // avoid retreating ground we've already covered.
            final List<Integer> lineLengthCache = new ArrayList<Integer>();
            lineLengthCache.add(0); // line 1 is offset 0

            for (final AuditEvent event : errors) {
                int offset;
                boolean endOfLine = false;

                // start of file
                if (event.getLine() == 0) { // start of file errors
                    offset = event.getColumn();

                // line offset is cached...
                } else if (event.getLine() <= lineLengthCache.size()) {
                    offset = lineLengthCache.get(event.getLine() - 1) + event.getColumn();

                // further search required
                } else {
                    // start from end of cached data
                    offset = lineLengthCache.get(lineLengthCache.size() - 1);
                    int line = lineLengthCache.size();

                    int column = 0;
                    for (int i = offset; i < text.length; ++i) {
                        final char character = text[i];

                        // for linefeeds we need to handle CR, LF and CRLF,
                        // hence we accept either and only trigger a new
                        // line on the LF of CRLF.
                        final char nextChar = (i + 1) < text.length ? text[i + 1] : '\0';
                        if (character == '\n' || character == '\r' && nextChar != '\n') {
                            ++line;
                            ++offset;
                            lineLengthCache.add(offset);
                            column = 0;
                        } else {
                            ++column;
                            ++offset;
                        }

                        // need to go to end of line though
                        if (event.getLine() == line && event.getColumn() == column) {
                            if (column == 0 && Character.isWhitespace(nextChar)) {
                                // move line errors to after EOL
                                endOfLine = true;
                            }
                            break;
                        }
                    }
                }

                final PsiElement victim = psiFile.findElementAt(offset);

                if (victim == null) {
                    LOG.error("Couldn't find victim for error: " + event.getFileName() + "("
                            + event.getLine() + ":" + event.getColumn() + ") " + event.getMessage());
                } else {
                    final String message = event.getLocalizedMessage() != null
                            ? event.getLocalizedMessage().getMessage()
                            : event.getMessage();
                    final ProblemHighlightType problemType
                            = ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
                    final ProblemDescriptor problem = manager.createProblemDescriptor(
                            victim, message, null, problemType, endOfLine);
                    problems.add(problem);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void fileStarted(final AuditEvent auditEvent) {

        }

        /**
         * {@inheritDoc}
         */
        public void fileFinished(final AuditEvent auditEvent) {

        }

        /**
         * {@inheritDoc}
         */
        public void addError(final AuditEvent auditEvent) {
            errors.add(auditEvent);
        }

        /**
         * {@inheritDoc}
         */
        public void addException(final AuditEvent auditEvent,
                                 final Throwable throwable) {
            LOG.error("Exception during CheckStyle execution", throwable);
            errors.add(auditEvent);
        }

        /**
         * Get the problems found by this scan.
         *
         * @return the problems found by this scan.
         */
        public List<ProblemDescriptor> getProblems() {
            return problems;
        }

    }
}
