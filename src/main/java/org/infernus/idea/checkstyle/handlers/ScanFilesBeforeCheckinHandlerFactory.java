package org.infernus.idea.checkstyle.handlers;

import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import org.jetbrains.annotations.NotNull;

public class ScanFilesBeforeCheckinHandlerFactory extends CheckinHandlerFactory {

    @NotNull
    @Override
    public CheckinHandler createHandler(final CheckinProjectPanel checkinProjectPanel,
                                        final CommitContext commitContext) {
        return new ScanFilesBeforeCheckinHandler(checkinProjectPanel);
    }

}
