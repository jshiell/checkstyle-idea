package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInsight.daemon.impl.actions.SuppressFix;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.jetbrains.annotations.NotNull;

public class SuppressForCheckstyleFix extends SuppressFix {
    SuppressForCheckstyleFix(@NotNull String sourceCheckName) {
        super("checkstyle:" + sourceCheckName);
    }

    @NotNull
    @Override
    public String getText() {
        return CheckStyleBundle.message("inspection.fix.suppress-for-checkstyle");
    }

}
