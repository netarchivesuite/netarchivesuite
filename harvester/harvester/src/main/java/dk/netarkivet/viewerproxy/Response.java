package dk.netarkivet.viewerproxy;

import java.io.OutputStream;

/**
 * The Response interface is a very minimal version of a HTTP response.
 * We use this to decouple the main parts of the proxy server from
 * a given implementation.
 */
public interface Response {

    /** Return outputstream response should be written to.
     * @return the outputstream response should be written to
     */
    OutputStream getOutputStream();

    /** Set status code.
     * @param statusCode should be valid http status ie. 200, 404,
    */
    void setStatus(int statusCode);

    /** Set status code. and explanatory text string describing the status.
     * @param statusCode should be valid http status ie. 200, 404,
     * @param reason text string explaining status ie. OK, not found,
     */
    void setStatus(int statusCode, String reason);

    /**Add an HTTP header to the response.
     * @param name Name of the header, e.g. Last-Modified-Date
     * @param value The value of the header
    */
    void addHeaderField(String name, String value);

    /** Get the status code from this response.
     *
     * @return The statuscode.
     */
    int getStatus();

}
