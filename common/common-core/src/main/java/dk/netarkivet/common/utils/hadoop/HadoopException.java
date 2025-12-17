package dk.netarkivet.common.utils.hadoop;

public class HadoopException extends Exception {
    public HadoopException(String message) {
        super(message);
    }

    public HadoopException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public HadoopException(final Throwable cause) {
        super(cause);
    }

}
