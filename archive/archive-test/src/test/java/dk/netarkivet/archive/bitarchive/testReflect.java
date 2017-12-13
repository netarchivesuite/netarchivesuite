package dk.netarkivet.archive.bitarchive;

import java.lang.reflect.Field;

import dk.netarkivet.common.distribute.Channels;

public class testReflect {
	public static void resetChannels() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
    	Field field = Channels.class.getDeclaredField("instance");
    	field.set(null, (Channels) null);
    }
	public static void main(String[] args) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Channels.reset();
	}
}
