package dk.netarkivet.archive;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.Assert;
import org.junit.Test;

public class ArchiveSettingsTest {

    @Test
	public void testNoFinalSettingsConstants() {
        for (Field f: ArchiveSettings.class.getDeclaredFields()) {
            // Check that all static public fields are not final
            int modifiers = f.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
                Assert.assertFalse(
                        "public static fields must not be final, "
                		+ "but this was violated by field " + f.getName(),            
                         Modifier.isFinal(modifiers));
            }
        }
    }
}
