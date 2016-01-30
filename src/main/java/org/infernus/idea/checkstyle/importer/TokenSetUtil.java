package org.infernus.idea.checkstyle.importer;

import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import java.lang.reflect.Field;
import java.util.Arrays;

class TokenSetUtil {
    private TokenSetUtil() {
    }
    
    static int[] getTokens(String tokenString) {
        String[] tokenStrings = tokenString.split("\\s*,\\s*");
        int[] tokenIds = new int[tokenStrings.length];
        int i = 0;
        for (String tokenStr : tokenStrings) {
            try {
                Field f = TokenTypes.class.getDeclaredField(tokenStr);
                tokenIds[i] = f.getInt(null);
                i ++;
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // Ignore
            }
        }
        return Arrays.copyOf(tokenIds, i);
    }
}
