package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckerFactoryCacheKeyTest {

    @Mock private ConfigurationLocation locationA;
    @Mock private ConfigurationLocation locationB;
    @Mock private Module moduleA;
    @Mock private Module moduleB;
    @Mock private Project projectA;
    @Mock private Project projectB;

    @Test
    void twoKeysWithSameLocationAndNoModuleAreEqual() {
        CheckerFactoryCacheKey key1 = new CheckerFactoryCacheKey(locationA, null);
        CheckerFactoryCacheKey key2 = new CheckerFactoryCacheKey(locationA, null);

        assertThat(key1, is(equalTo(key2)));
    }

    @Test
    void twoKeysWithSameLocationAndNoModuleHaveSameHashCode() {
        CheckerFactoryCacheKey key1 = new CheckerFactoryCacheKey(locationA, null);
        CheckerFactoryCacheKey key2 = new CheckerFactoryCacheKey(locationA, null);

        assertThat(key1.hashCode(), is(key2.hashCode()));
    }

    @Test
    void twoKeysWithDifferentLocationsAreNotEqual() {
        CheckerFactoryCacheKey key1 = new CheckerFactoryCacheKey(locationA, null);
        CheckerFactoryCacheKey key2 = new CheckerFactoryCacheKey(locationB, null);

        assertThat(key1, is(not(equalTo(key2))));
    }

    @Test
    void twoKeysWithSameLocationAndSameModuleAreEqual() {
        when(moduleA.getName()).thenReturn("myModule");
        when(moduleA.getProject()).thenReturn(projectA);
        when(projectA.getName()).thenReturn("myProject");

        CheckerFactoryCacheKey key1 = new CheckerFactoryCacheKey(locationA, moduleA);
        CheckerFactoryCacheKey key2 = new CheckerFactoryCacheKey(locationA, moduleA);

        assertThat(key1, is(equalTo(key2)));
    }

    @Test
    void twoKeysWithSameLocationAndSameModuleHaveSameHashCode() {
        when(moduleA.getName()).thenReturn("myModule");
        when(moduleA.getProject()).thenReturn(projectA);
        when(projectA.getName()).thenReturn("myProject");

        CheckerFactoryCacheKey key1 = new CheckerFactoryCacheKey(locationA, moduleA);
        CheckerFactoryCacheKey key2 = new CheckerFactoryCacheKey(locationA, moduleA);

        assertThat(key1.hashCode(), is(key2.hashCode()));
    }

    @Test
    void keyWithModuleIsNotEqualToKeyWithNoModule() {
        when(moduleA.getName()).thenReturn("myModule");
        when(moduleA.getProject()).thenReturn(projectA);
        when(projectA.getName()).thenReturn("myProject");

        CheckerFactoryCacheKey withModule = new CheckerFactoryCacheKey(locationA, moduleA);
        CheckerFactoryCacheKey withoutModule = new CheckerFactoryCacheKey(locationA, null);

        assertThat(withModule, is(not(equalTo(withoutModule))));
    }

    @Test
    void keysWithDifferentModuleNamesAreNotEqual() {
        when(moduleA.getName()).thenReturn("moduleA");
        when(moduleA.getProject()).thenReturn(projectA);
        when(projectA.getName()).thenReturn("myProject");

        when(moduleB.getName()).thenReturn("moduleB");
        when(moduleB.getProject()).thenReturn(projectA);

        CheckerFactoryCacheKey key1 = new CheckerFactoryCacheKey(locationA, moduleA);
        CheckerFactoryCacheKey key2 = new CheckerFactoryCacheKey(locationA, moduleB);

        assertThat(key1, is(not(equalTo(key2))));
    }

    @Test
    void keysWithDifferentProjectNamesAreNotEqual() {
        when(moduleA.getName()).thenReturn("myModule");
        when(moduleA.getProject()).thenReturn(projectA);
        when(projectA.getName()).thenReturn("projectA");

        when(moduleB.getName()).thenReturn("myModule");
        when(moduleB.getProject()).thenReturn(projectB);
        when(projectB.getName()).thenReturn("projectB");

        CheckerFactoryCacheKey key1 = new CheckerFactoryCacheKey(locationA, moduleA);
        CheckerFactoryCacheKey key2 = new CheckerFactoryCacheKey(locationA, moduleB);

        assertThat(key1, is(not(equalTo(key2))));
    }

    @Test
    void keyIsNotEqualToNull() {
        CheckerFactoryCacheKey key = new CheckerFactoryCacheKey(locationA, null);

        assertThat(key.equals(null), is(false));
    }

    @Test
    void keyIsEqualToItself() {
        CheckerFactoryCacheKey key = new CheckerFactoryCacheKey(locationA, null);

        assertThat(key.equals(key), is(true));
    }
}
