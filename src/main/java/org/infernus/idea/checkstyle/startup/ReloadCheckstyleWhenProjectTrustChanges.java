package org.infernus.idea.checkstyle.startup;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.ide.trustedProjects.TrustedProjects;
import com.intellij.ide.trustedProjects.TrustedProjectsListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.util.messages.MessageBusConnection;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.ConfigurationInvalidator;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.util.Notifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.infernus.idea.checkstyle.CheckStyleBundle.message;

/**
 * Third-party check JARs are only loaded for trusted projects, so when a project's trust state changes the
 * cached checkers and the Checkstyle classloader built under the old state have to be thrown away.
 */
public class ReloadCheckstyleWhenProjectTrustChanges implements ProjectActivity {

    @FunctionalInterface
    interface Warner {
        void showWarning(Project project, String text);
    }

    @FunctionalInterface
    interface ConnectionFactory {
        MessageBusConnection connect(@NotNull Disposable parentDisposable);
    }

    private final ConnectionFactory connectionFactory;
    private final Warner warner;
    private final Consumer<Runnable> backgroundDispatcher;
    private final Predicate<Project> projectIsTrusted;

    public ReloadCheckstyleWhenProjectTrustChanges() {
        this(parentDisposable -> ApplicationManager.getApplication().getMessageBus().connect(parentDisposable),
                Notifications::showWarning,
                ApplicationManager.getApplication()::executeOnPooledThread,
                TrustedProjects::isProjectTrusted);
    }

    ReloadCheckstyleWhenProjectTrustChanges(@NotNull final ConnectionFactory connectionFactory,
                                            @NotNull final Warner warner,
                                            @NotNull final Consumer<Runnable> backgroundDispatcher,
                                            @NotNull final Predicate<Project> projectIsTrusted) {
        this.connectionFactory = connectionFactory;
        this.warner = warner;
        this.backgroundDispatcher = backgroundDispatcher;
        this.projectIsTrusted = projectIsTrusted;
    }

    @Nullable
    @Override
    public Object execute(@NotNull final Project project, @NotNull final Continuation<? super Unit> continuation) {
        // TrustedProjects offers no public way to tell an explicit "no" from "not decided yet" (that is the
        // @Internal getProjectTrustedState), so a project with no recorded decision is warned too. The trust
        // dialog normally resolves before the project opens, so that should only be an unusual open path.
        if (!project.getService(PluginConfigurationManager.class).getCurrent().getThirdPartyClasspath().isEmpty()
                && !projectIsTrusted.test(project)) {
            warner.showWarning(project, message("startup.untrusted-project.third-party-classpath-skipped"));
        }

        MessageBusConnection connection = connectionFactory.connect(project.getService(CheckstyleProjectService.class));
        connection.subscribe(TrustedProjectsListener.TOPIC, new TrustedProjectsListener() {
            @Override
            public void onProjectTrusted(@NotNull final Project trusted) {
                reload(trusted);
            }

            @Override
            public void onProjectUntrusted(@NotNull final Project untrusted) {
                reload(untrusted);
            }

            /**
             * {@code TrustedProjectsListener.TOPIC} is published on the application bus, so this fires for
             * every open project, not just ours.
             */
            private void reload(@NotNull final Project eventProject) {
                if (!project.equals(eventProject) || project.isDisposed()) {
                    return;
                }
                backgroundDispatcher.accept(() -> {
                    // Re-checked: the project can close between the event and this running on a pooled thread.
                    if (project.isDisposed()) {
                        return;
                    }
                    project.getService(ConfigurationInvalidator.class).invalidateCachedResources();
                    ApplicationManager.getApplication().invokeLater(
                            () -> DaemonCodeAnalyzer.getInstance(project).restart(),
                            project.getDisposed());
                });
            }
        });
        return null;
    }
}
