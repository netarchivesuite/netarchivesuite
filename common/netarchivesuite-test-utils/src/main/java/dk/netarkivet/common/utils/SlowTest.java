package dk.netarkivet.common.utils;

/** <p>
 * Marker interface to identify tests as taking a long time to run. This is used to disable slow tests as part of the
 * default <code>mvn test</code> phase. Slow test are included by using the <code>fullTest</code> profile.
 * </p>
 * See general netarchivesuite pom for details.
 */
public interface SlowTest {
}
