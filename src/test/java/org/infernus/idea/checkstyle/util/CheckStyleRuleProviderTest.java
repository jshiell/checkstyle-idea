package org.infernus.idea.checkstyle.util;

import org.junit.Test;

import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;

public class CheckStyleRuleProviderTest {
  @Test(expected = FileNotFoundException.class)
  public void checkStyleRuleProviderInitNotExistingFileTest() throws FileNotFoundException {
    new CheckStyleRuleProvider("definitely not here");
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkStyleRuleProviderInitNotXMLFileTest() throws FileNotFoundException {
    new CheckStyleRuleProvider("src/test/java/org/infernus/idea/checkstyle/util/CheckStyleRuleProviderTest.java");
  }

  @Test
  public void checkStyleRuleProviderTest() {
    CheckStyleRuleProvider provider = new CheckStyleRuleProvider();

    assertEquals(6, provider.getTypeOptions("Scope").size());
  }
}
