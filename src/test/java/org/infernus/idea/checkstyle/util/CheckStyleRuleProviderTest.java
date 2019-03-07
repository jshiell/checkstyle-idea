package org.infernus.idea.checkstyle.util;

import org.junit.Test;

import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;

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

  @Test
  public void CheckStyleRuleProviderTest() {
    CheckStyleRuleProvider provider = new CheckStyleRuleProvider();

    assertEquals(6, provider.getTypeOptions("Scope").size());
  }
}