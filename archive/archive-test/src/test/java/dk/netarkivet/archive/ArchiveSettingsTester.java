package dk.netarkivet.archive;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import junit.framework.TestCase;

/** Unittestersuite for the ArchiveSettings class. */
public class ArchiveSettingsTester extends TestCase {

    @SuppressWarnings("rawtypes")
	public void testNoFinalSettingsConstants() {
        Class c = ArchiveSettings.class;
        Field[] fields = c.getDeclaredFields();
        for (Field f: fields) {
            // Check that all static public fields are not final
            int modifiers = f.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
                assertFalse("public static fields must not be final, "
                		+ "but this was violated by field " + f.getName(),            
                         Modifier.isFinal(modifiers));
            }
        }
    }
}
