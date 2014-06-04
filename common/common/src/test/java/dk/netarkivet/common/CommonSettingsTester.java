package dk.netarkivet.common;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.Field;

import org.junit.Test;

/**
 * Test that all the public static fields for the CommonSettings class are
 * <i>not</i> final.
 * 
 */
public class CommonSettingsTester {

    @Test
    public void testNoFinalSettingsConstants() {
        Class<CommonSettings> c = CommonSettings.class;
        Field[] fields = c.getDeclaredFields();
        for (Field f : fields) {
            int modifiers = f.getModifiers();
            if (isPublic(modifiers) && isStatic(modifiers)) {
                assertFalse("CommonSettings: field final: " + f.getName(),
                        isFinal(modifiers));
            }
        }
    }
}
