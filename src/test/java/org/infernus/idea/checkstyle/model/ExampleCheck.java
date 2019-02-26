package org.infernus.idea.checkstyle.model;

////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2019 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////


import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;



/**
 * Checks correct indentation of Java Code.
 *
 * <p>
 * The basic idea behind this is that while
 * pretty printers are sometimes convenient for reformatting of
 * legacy code, they often either aren't configurable enough or
 * just can't anticipate how format should be done.  Sometimes this is
 * personal preference, other times it is practical experience.  In any
 * case, this check should just ensure that a minimal set of indentation
 * rules are followed.
 * </p>
 *
 * <p>
 * Implementation --
 *  Basically, this check requests visitation for all handled token
 *  types (those tokens registered in the HandlerFactory).  When visitToken
 *  is called, a new ExpressionHandler is created for the AST and pushed
 *  onto the handlers stack.  The new handler then checks the indentation
 *  for the currently visiting AST.  When leaveToken is called, the
 *  ExpressionHandler is popped from the stack.
 * </p>
 *
 * <p>
 *  While on the stack the ExpressionHandler can be queried for the
 *  indentation level it suggests for children as well as for other
 *  values.
 * </p>
 *
 * <p>
 *  While an ExpressionHandler checks the indentation level of its own
 *  AST, it typically also checks surrounding ASTs.  For instance, a
 *  while loop handler checks the while loop as well as the braces
 *  and immediate children.
 * </p>
 * <pre>
 *   - handler class -to-&gt; ID mapping kept in Map
 *   - parent passed in during construction
 *   - suggest child indent level
 *   - allows for some tokens to be on same line (ie inner classes OBJBLOCK)
 *     and not increase indentation level
 *   - looked at using double dispatch for getSuggestedChildIndent(), but it
 *     doesn't seem worthwhile, at least now
 *   - both tabs and spaces are considered whitespace in front of the line...
 *     tabs are converted to spaces
 *   - block parents with parens -- for, while, if, etc... -- are checked that
 *     they match the level of the parent
 * </pre>
 *
 * @noinspection ThisEscapedInObjectConstruction
 */

public class ExampleCheck {

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_ERROR = "indentation.error";

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_ERROR_MULTI = "indentation.error.multi";

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_CHILD_ERROR = "indentation.child.error";

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_CHILD_ERROR_MULTI = "indentation.child.error.multi";

    /** Default indentation amount - based on Sun. */
    private static final int DEFAULT_INDENTATION = 4;



    /** Lines logged as having incorrect indentation. */
    private Set<Integer> incorrectIndentationLines;

    /** How many tabs or spaces to use. */
    private int basicOffset = DEFAULT_INDENTATION;

    /** How much to indent a case label. */
    private int caseIndent = DEFAULT_INDENTATION;

    /** How far brace should be indented when on next line. */
    private int braceAdjustment;

    /** How far throws should be indented when on next line. */
    private int throwsIndent = DEFAULT_INDENTATION;

    /** How much to indent an array initialization when on next line. */
    private int arrayInitIndent = DEFAULT_INDENTATION;

    /** How far continuation line should be indented when line-wrapping is present. */
    private int lineWrappingIndentation = DEFAULT_INDENTATION;

    /**
     * Force strict condition in line wrapping case. If value is true, line wrap indent
     * have to be same as lineWrappingIndentation parameter, if value is false, line wrap indent
     * have to be not less than lineWrappingIndentation parameter.
     */
    private boolean forceStrictCondition;

    /**
     * Get forcing strict condition.
     * @return forceStrictCondition value.
     */
    public boolean isForceStrictCondition() {
        return forceStrictCondition;
    }

    /**
     * Set forcing strict condition.
     * @param value user's value of forceStrictCondition.
     */
    public void setForceStrictCondition(boolean value) {
        forceStrictCondition = value;
    }

    /**
     * Set the basic offset.
     *
     * @param basicOffset   the number of tabs or spaces to indent
     */
    public void setBasicOffset(int basicOffset) {
        this.basicOffset = basicOffset;
    }

    /**
     * Get the basic offset.
     *
     * @return the number of tabs or spaces to indent
     */
    public int getBasicOffset() {
        return basicOffset;
    }

    /**
     * Adjusts brace indentation (positive offset).
     *
     * @param adjustmentAmount   the brace offset
     */
    public void setBraceAdjustment(int adjustmentAmount) {
        braceAdjustment = adjustmentAmount;
    }

    /**
     * Get the brace adjustment amount.
     *
     * @return the positive offset to adjust braces
     */
    public int getBraceAdjustment() {
        return braceAdjustment;
    }

    /**
     * Set the case indentation level.
     *
     * @param amount   the case indentation level
     */
    public void setCaseIndent(int amount) {
        caseIndent = amount;
    }

    /**
     * Get the case indentation level.
     *
     * @return the case indentation level
     */
    public int getCaseIndent() {
        return caseIndent;
    }

    /**
     * Set the throws indentation level.
     *
     * @param throwsIndent the throws indentation level
     */
    public void setThrowsIndent(int throwsIndent) {
        this.throwsIndent = throwsIndent;
    }

    /**
     * Get the throws indentation level.
     *
     * @return the throws indentation level
     */
    public int getThrowsIndent() {
        return throwsIndent;
    }

    /**
     * Set the array initialisation indentation level.
     *
     * @param arrayInitIndent the array initialisation indentation level
     */
    public void setArrayInitIndent(int arrayInitIndent) {
        this.arrayInitIndent = arrayInitIndent;
    }

    /**
     * Get the line-wrapping indentation level.
     *
     * @return the initialisation indentation level
     */
    public int getArrayInitIndent() {
        return arrayInitIndent;
    }

    /**
     * Get the array line-wrapping indentation level.
     *
     * @return the line-wrapping indentation level
     */
    public int getLineWrappingIndentation() {
        return lineWrappingIndentation;
    }

    /**
     * Set the line-wrapping indentation level.
     *
     * @param lineWrappingIndentation the line-wrapping indentation level
     */
    public void setLineWrappingIndentation(int lineWrappingIndentation) {
        this.lineWrappingIndentation = lineWrappingIndentation;
    }



}
