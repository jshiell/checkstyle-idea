package org.infernus.idea.checkstyle.util;

import org.junit.Test;

import java.io.FileNotFoundException;

public class CheckStyleRuleProviderTest {
  @Test(expected = FileNotFoundException.class)
  public void CheckStyleRuleProviderInitNotExistingFileTest() throws FileNotFoundException {
    CheckStyleRuleProvider provider = new CheckStyleRuleProvider("definitely not here");
  }

  @Test(expected = IllegalArgumentException.class)
  public void CheckStyleRuleProviderInitNotXMLFileTest() throws FileNotFoundException {
    CheckStyleRuleProvider provider =
            new CheckStyleRuleProvider("src/test/java/org/infernus/idea/checkstyle/util/CheckStyleRuleProviderTest.java");
  }
}