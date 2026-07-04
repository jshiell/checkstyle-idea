package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementEntryMatcher;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementMatchRule;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementCompositeMatchCondition;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementSettingsToken;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementSettings;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.EntryType.*;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Modifier.*;

@SuppressWarnings("unused")
public class DeclarationOrderImporter extends ModuleImporter {

    private boolean ignoreConstructors = false;
    private boolean ignoreModifiers = false;

    @Override
    protected void handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
        if ("ignoreConstructors".equals(attrName)) {
            ignoreConstructors = Boolean.parseBoolean(attrValue);
        } else if ("ignoreModifiers".equals(attrName)) {
            ignoreModifiers = Boolean.parseBoolean(attrValue);
        }
    }

    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        List<StdArrangementMatchRule> rules = new ArrayList<>();

        if (ignoreModifiers) {
            rules.add(rule(FIELD, STATIC));
            rules.add(rule(FIELD));
        } else {
            rules.add(rule(FIELD, STATIC, PUBLIC));
            rules.add(rule(FIELD, STATIC, PROTECTED));
            rules.add(rule(FIELD, STATIC, PACKAGE_PRIVATE));
            rules.add(rule(FIELD, STATIC, PRIVATE));
            rules.add(rule(FIELD, PUBLIC));
            rules.add(rule(FIELD, PROTECTED));
            rules.add(rule(FIELD, PACKAGE_PRIVATE));
            rules.add(rule(FIELD, PRIVATE));
        }

        if (!ignoreConstructors) {
            rules.add(rule(CONSTRUCTOR));
        }

        rules.add(rule(METHOD));

        getCommonSettings(settings).setArrangementSettings(
                StdArrangementSettings.createByMatchRules(Collections.emptyList(), rules));
    }

    private static StdArrangementMatchRule rule(final ArrangementSettingsToken... tokens) {
        if (tokens.length == 1) {
            return new StdArrangementMatchRule(
                    new StdArrangementEntryMatcher(new ArrangementAtomMatchCondition(tokens[0])));
        }
        ArrangementCompositeMatchCondition composite = new ArrangementCompositeMatchCondition();
        for (ArrangementSettingsToken token : tokens) {
            composite.addOperand(new ArrangementAtomMatchCondition(token));
        }
        return new StdArrangementMatchRule(new StdArrangementEntryMatcher(composite));
    }
}
