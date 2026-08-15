package org.infernus.idea.checkstyle.checker;

import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ScanResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The results of a completed scan, sorted into those worth displaying and those the user needs
 * telling about.
 *
 * @param validResults         the results that were produced against an available rules file.
 * @param rulesFilesNotPresent the rules files that could not be found.
 * @param blockedRulesFiles    the rules files that are blocked following an earlier error.
 */
record ScanOutcome(@NotNull List<ScanResult> validResults,
                   @NotNull List<ConfigurationLocation> rulesFilesNotPresent,
                   @NotNull List<ConfigurationLocation> blockedRulesFiles) {

    @NotNull
    static ScanOutcome of(@NotNull final List<ScanResult> scanResults) {
        final var validResults = new ArrayList<ScanResult>();
        final var notPresent = new ArrayList<ConfigurationLocation>();
        final var blocked = new ArrayList<ConfigurationLocation>();

        for (final ScanResult scanResult : scanResults) {
            final ConfigurationLocationResult locationResult = scanResult.configurationLocationResult();
            if (locationResult == null) {
                continue;
            }
            switch (locationResult.status()) {
                case NOT_PRESENT -> notPresent.add(locationResult.location());
                case BLOCKED -> blocked.add(locationResult.location());
                default -> validResults.add(scanResult);
            }
        }

        return new ScanOutcome(validResults, notPresent, blocked);
    }

    boolean hasBlockedRulesFiles() {
        return !blockedRulesFiles.isEmpty();
    }

    /**
     * A message describing anything the user needs to know about the rules files used.
     *
     * @return the message, which is empty when there is nothing to report.
     */
    @NotNull
    String warningMessage() {
        final var warningMessages = new ArrayList<String>();
        if (!rulesFilesNotPresent.isEmpty()) {
            warningMessages.add(CheckStyleBundle.message("plugin.results.no-rules-file"));
        }
        if (hasBlockedRulesFiles()) {
            final var maxTimeBlocked = blockedRulesFiles.stream()
                    .map(ConfigurationLocation::blockedForSeconds)
                    .reduce(Long::max)
                    .orElse(0L);
            final var blockedLocations = String.join(", ",
                    blockedRulesFiles.stream().map(ConfigurationLocation::getDescription).toList());
            warningMessages.add(CheckStyleBundle.message("plugin.results.rules-blocked", maxTimeBlocked, blockedLocations));
        }
        return String.join("; ", warningMessages);
    }
}
