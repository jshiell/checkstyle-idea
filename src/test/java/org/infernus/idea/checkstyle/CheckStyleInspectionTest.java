package org.infernus.idea.checkstyle;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.infernus.idea.checkstyle.checker.CheckStyleChecker;
import org.infernus.idea.checkstyle.checker.CheckerFactory;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.config.ConfigurationLocationSource;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ScanScope;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CheckStyleInspection}.
 *
 * <p>Uses the real headless IDEA application (via {@link BasePlatformTestCase}) so that
 * platform services such as {@code InjectedLanguageManager}, {@code ReadAction}, and
 * {@code PsiDocumentManager} work correctly. Project-level services
 * ({@link PluginConfigurationManager}, {@link ConfigurationLocationSource}, {@link CheckerFactory})
 * are replaced with Mockito mocks so that each test exercises a specific code path without
 * requiring a live Checkstyle classloader.</p>
 */
public class CheckStyleInspectionTest extends BasePlatformTestCase {

    private static final String MINIMAL_CHECKSTYLE_XML =
            """
                    <?xml version="1.0"?>
                    <!DOCTYPE module PUBLIC "-//Puppy Crawl//DTD Check Configuration 1.3//EN"
                        "http://www.puppycrawl.com/dtds/configuration_1_3.dtd">
                    <module name="Checker"/>
                    """;

    private CheckStyleInspection underTest;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        underTest = new CheckStyleInspection();
    }

    public void testDropIgnoredProblemsKeepsProblemsAboveIgnoreLevel() {
        PsiElement element = mock(PsiElement.class);
        List<Problem> input = List.of(
                problem(element, SeverityLevel.Error),
                problem(element, SeverityLevel.Warning),
                problem(element, SeverityLevel.Info)
        );

        List<Problem> result = underTest.dropIgnoredProblems(input);

        assertEquals(3, result.size());
    }

    public void testDropIgnoredProblemsRemovesIgnoreLevelProblems() {
        PsiElement element = mock(PsiElement.class);
        List<Problem> input = List.of(
                problem(element, SeverityLevel.Error),
                problem(element, SeverityLevel.Ignore)
        );

        List<Problem> result = underTest.dropIgnoredProblems(input);

        assertEquals(1, result.size());
        assertEquals(SeverityLevel.Error, result.getFirst().severityLevel());
    }

    public void testDropIgnoredProblemsReturnsEmptyListWhenAllProblemsAreIgnored() {
        PsiElement element = mock(PsiElement.class);
        List<Problem> input = List.of(
                problem(element, SeverityLevel.Ignore),
                problem(element, SeverityLevel.Ignore)
        );

        List<Problem> result = underTest.dropIgnoredProblems(input);

        assertTrue(result.isEmpty());
    }

    public void testDropIgnoredProblemsReturnsEmptyListForEmptyInput() {
        List<Problem> result = underTest.dropIgnoredProblems(List.of());

        assertTrue(result.isEmpty());
    }

    public void testDropIgnoredProblemsDoesNotMutateTheInputList() {
        PsiElement element = mock(PsiElement.class);
        List<Problem> input = List.of(
                problem(element, SeverityLevel.Error),
                problem(element, SeverityLevel.Ignore)
        );

        underTest.dropIgnoredProblems(input);

        assertEquals(2, input.size());
    }

    public void testCheckFileReturnsNoProblemsWhenNoLocationsAreConfigured() {
        ConfigurationLocation location = inMemoryLocation();
        registerPluginConfigurationManager(everythingScopeConfigurationWith(location));

        ConfigurationLocationSource emptyLocationSource = mock(ConfigurationLocationSource.class);
        when(emptyLocationSource.getConfigurationLocations(any(), any())).thenReturn(Collections.emptySortedSet());
        registerConfigurationLocationSource(emptyLocationSource);

        PsiFile psiFile = myFixture.configureByText("Foo.java", "class Foo {}");
        InspectionManager manager = InspectionManager.getInstance(getProject());

        ProblemDescriptor[] result = underTest.checkFile(psiFile, manager, false);

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    public void testCheckFileReturnsNoProblemsWhenAllLocationsAreBlocked() {
        ConfigurationLocation location = inMemoryLocation();
        registerPluginConfigurationManager(everythingScopeConfigurationWith(location));

        ConfigurationLocation blockedLocation = inMemoryLocation();
        blockedLocation.block();

        TreeSet<ConfigurationLocation> locations = new TreeSet<>();
        locations.add(blockedLocation);

        ConfigurationLocationSource locationSource = mock(ConfigurationLocationSource.class);
        when(locationSource.getConfigurationLocations(any(), any())).thenReturn(locations);
        registerConfigurationLocationSource(locationSource);

        PsiFile psiFile = myFixture.configureByText("Foo.java", "class Foo {}");
        InspectionManager manager = InspectionManager.getInstance(getProject());

        ProblemDescriptor[] result = underTest.checkFile(psiFile, manager, false);

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    public void testCheckFileReturnsNoProblemsWhenCheckerIsNotAvailable() {
        ConfigurationLocation location = inMemoryLocation();
        registerPluginConfigurationManager(everythingScopeConfigurationWith(location));

        TreeSet<ConfigurationLocation> locations = new TreeSet<>();
        locations.add(location);

        ConfigurationLocationSource locationSource = mock(ConfigurationLocationSource.class);
        when(locationSource.getConfigurationLocations(any(), any())).thenReturn(locations);
        registerConfigurationLocationSource(locationSource);

        CheckerFactory checkerFactory = mock(CheckerFactory.class);
        when(checkerFactory.checker(any(), any())).thenReturn(Optional.empty());
        registerCheckerFactory(checkerFactory);

        PsiFile psiFile = myFixture.configureByText("Foo.java", "class Foo {}");
        InspectionManager manager = InspectionManager.getInstance(getProject());

        ProblemDescriptor[] result = underTest.checkFile(psiFile, manager, false);

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    public void testCheckFileReturnsProblemDescriptorsWhenCheckerFindsProblems() {
        ConfigurationLocation location = inMemoryLocation();
        registerPluginConfigurationManager(everythingScopeConfigurationWith(location));

        TreeSet<ConfigurationLocation> locations = new TreeSet<>();
        locations.add(location);

        ConfigurationLocationSource locationSource = mock(ConfigurationLocationSource.class);
        when(locationSource.getConfigurationLocations(any(), any())).thenReturn(locations);
        registerConfigurationLocationSource(locationSource);

        PsiFile psiFile = myFixture.addFileToProject("Foo.java", "class Foo {}");

        Problem problem = new Problem(
                psiFile,
                "test.message.key",
                SeverityLevel.Warning,
                1, 1,
                "SourceCheck",
                false,
                false);

        CheckStyleChecker checker = mock(CheckStyleChecker.class);
        when(checker.scan(any(), anyBoolean())).thenReturn(Map.of(psiFile, List.of(problem)));

        CheckerFactory checkerFactory = mock(CheckerFactory.class);
        when(checkerFactory.checker(any(), any())).thenReturn(Optional.of(checker));
        registerCheckerFactory(checkerFactory);

        InspectionManager manager = InspectionManager.getInstance(getProject());

        ProblemDescriptor[] result = underTest.checkFile(psiFile, manager, false);

        assertNotNull(result);
        assertEquals(1, result.length);
    }

    private Problem problem(final PsiElement element, final SeverityLevel severityLevel) {
        return new Problem(element, "message", severityLevel, 1, 1, "SourceCheck", false, false);
    }

    private ConfigurationLocation inMemoryLocation() {
        return new StringConfigurationLocation(MINIMAL_CHECKSTYLE_XML, getProject());
    }

    private PluginConfiguration everythingScopeConfigurationWith(final ConfigurationLocation location) {
        TreeSet<ConfigurationLocation> locationSet = new TreeSet<>();
        locationSet.add(location);
        TreeSet<String> activeIds = new TreeSet<>();
        activeIds.add(location.getId());
        return PluginConfigurationBuilder.testInstance("10.0")
                .withScanScope(ScanScope.Everything)
                .withLocations(locationSet)
                .withActiveLocationIds(activeIds)
                .build();
    }

    private void registerPluginConfigurationManager(final PluginConfiguration config) {
        PluginConfigurationManager mockConfigManager = mock(PluginConfigurationManager.class);
        when(mockConfigManager.getCurrent()).thenReturn(config);
        ServiceContainerUtil.replaceService(
                getProject(), PluginConfigurationManager.class, mockConfigManager, getTestRootDisposable());
    }

    private void registerConfigurationLocationSource(final ConfigurationLocationSource locationSource) {
        ServiceContainerUtil.replaceService(
                getProject(), ConfigurationLocationSource.class, locationSource, getTestRootDisposable());
    }

    private void registerCheckerFactory(final CheckerFactory checkerFactory) {
        ServiceContainerUtil.replaceService(
                getProject(), CheckerFactory.class, checkerFactory, getTestRootDisposable());
    }
}
