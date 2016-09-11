package org.infernus.idea.checkstyle;

public enum CheckstyleClasses
{
    TreeWalker("com.puppycrawl.tools.checkstyle.TreeWalker");

    //

    private final String fqcn;



    private CheckstyleClasses(final String pFqcn)
    {
        fqcn = pFqcn;
    }



    public String getFqcn()
    {
        return fqcn;
    }
}
