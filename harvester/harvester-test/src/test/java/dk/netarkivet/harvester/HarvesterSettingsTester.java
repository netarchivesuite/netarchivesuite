package dk.netarkivet.harvester;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import junit.framework.TestCase;

/** Unittestersuite for the HarvesterSettings class. */
public class HarvesterSettingsTester extends TestCase {

    public void testNoFinalSettingsConstants() {
        Class<HarvesterSettings> c = HarvesterSettings.class;
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
    
    /** 
     * If this test fails, we need to update the SingleMBeanObjectTester#Setup 
     * and ChannelIDTester.
     */
    public void testHarvestControllerPrioritySettingUnchanged() {
       assertEquals("The 'HarvesterSettings.HARVEST_CONTROLLER_CHANNEL' "
               + "setting has changed. Please update " 
               + "SingleMBeanObjectTester#Setup method",
               HarvesterSettings.HARVEST_CONTROLLER_CHANNEL, 
               "settings.harvester.harvesting.channel"); 
    }
    
}
