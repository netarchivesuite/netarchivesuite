package dk.netarkivet.common.distribute.arcrepository;

/**
 * Interface that just narrows the close() methos in AutoCloseable to not throw any checked exceptions.
 */
public interface ExceptionlessAutoCloseable extends AutoCloseable {
    @Override
    void close();
}
