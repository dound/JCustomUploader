package uploader.mechanisms;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import uploader.mechanisms.event.HTTPUploadListener;

/**
 * Uploads files using HTTP/1.0.  Files can be uploaded as raw binary data or
 * encoded as multipart/form-data.  An optional custom header
 * X-JCustomUploader-Filename can be included (its value is the file's name).
 *
 * @author David Underhill
 */
public class HTTPUploadMechanism extends AbstractUploadMechanism {
    /** the server:port to connect to */
    private final String host;
    private final int port;

    /** the request line and all headers EXCEPT content-length */
    private final String request_line_and_headers;

    /** whether to include the X-JCustomUploader-Filename header */
    private final boolean addCustomHeaderWithFilename;

    /** whether to encode the file as multipart/form-data */
    private final String multipartFormDataField;

    /** an object (if any) to notify whenever a response completes */
    private volatile HTTPUploadListener eventListener = null;

    /**
     * The boundary to use: if it overlaps with any data in the file, then it
     * is broken for that file, but the likelihood of an overlap is nil so I
     * went with the statically determined boundary instead of dynamically
     * computing one which definitely does not overlap.
     */
    private static final String BOUNDARY = "--------------------0ffc0f7fc30ad95021fc3b543ff06fe7dc8b79bd";

    /** the socket which we'll send the data over */
    private Socket sock;
    private OutputStream out;

    /**
     * Will upload to the specified path on the specified server on TCP port 80
     * via HTTP/1.0 POST with no extra headers.
     */
    public HTTPUploadMechanism(String host, String path) {
        this(host, 80, "POST", path, "");
    }

    /**
     * Calls the full constructor with these parameters and
     * addCustomHeaderWithFilename set to true and multipartFormDataField set
     * to null.
     */
    public HTTPUploadMechanism(String host, int port, String method, String path, String headers) {
        this(host, port, method, path, headers, true, null);
    }

    /**
     * Will upload to the specified path on the specified host:port via the
     * HTTP/1.0 method and including the specified headers.
     *
     * @param host     may be either an IP address or a hostname
     * @param port     the port to connect on (typically port 80)
     * @param method   the HTTP method to use (e.g., POST)
     * @param path     the path to send this to ("Request-URI")
     * @param headers  any headers to include; be sure to put one per line with
     *                 each line followed by "\r\n".
     * @param addCustomHeaderWithFilename  if true, then a header named
     *                 X-JCustomUploader-Filename will be set to the file's name.
     * @param multipartFormDataField  if specified, then the data will be
     *                 encoded as multipart/form-data and the field name will be
     *                 the value of this parameter.  Set to null to just send
     *                 raw bytes as the body.
     */
    public HTTPUploadMechanism(String host, int port, String method, String path,
                               String headers, boolean addCustomHeaderWithFilename,
                               String multipartFormDataField) {
        this.host = host;
        this.port = port;
        this.request_line_and_headers = method + " " + path + " HTTP/1.0\r\n" + headers;

        this.addCustomHeaderWithFilename = addCustomHeaderWithFilename;
        this.multipartFormDataField = multipartFormDataField;
    }

    public boolean tryToStartUpload(File f) {
        try {
            // open a TCP connection to the server
            sock = new Socket(host, port);
            out = sock.getOutputStream();

            // build any additional headers for this request ...
            String moreHeaders = "";

            // prepare the multipart/form-data stuff if requested
            String multiPartHeader = null;
            long sz = getFileSize();
            if(multipartFormDataField != null) {
                multiPartHeader = "--" + BOUNDARY + "\r\n" +
                                  "Content-Disposition: form-data; name=\"" + multipartFormDataField + "\"; filename=\"" + f.getName() + "\"\r\n" +
                                  "Content-Type: application/octet-stream\r\n\r\n";

                // update size to include the encoding overheard
                sz += multiPartHeader.getBytes().length + BOUNDARY.getBytes().length + 8;

                // provide a Content-Type header indicating that the body in multipart form data
                moreHeaders += "Content-Type: multipart/form-data; boundary=" + BOUNDARY + "\r\n";
            }

            // add any headers specific to this file
            String fileSpecificHeaders = getAdditionalHeaders(f);
            if(fileSpecificHeaders == null)
                return false;
            moreHeaders += fileSpecificHeaders;

            // send the request line and headers
            String data = request_line_and_headers + moreHeaders + "Content-Length: " + sz + "\r\n\r\n";
            out.write(data.getBytes());

            // send the multipart/form-data header (if we're doing this encoding)
            if(multipartFormDataField != null) {
                out.write(multiPartHeader.getBytes());
            }

            return true;
        } catch (UnknownHostException e) {
            haltWithError(e.getMessage());
            return false;
        } catch (IOException e) {
            haltWithError(e.getMessage());
            return false;
        }
    }

    /**
     * Returns the additional HTTP headers to send with the request for f.  By
     * default, the X-JCustomUploader-Filename header will be returned if
     * addCustomHeaderWithFilename=true was passed to the constructor.  null
     * should be returned (after calling haltWithError()) if an error occurs.
     */
    protected String getAdditionalHeaders(File f) {
        if(addCustomHeaderWithFilename)
            return "X-JCustomUploader-Filename: " + f.getName() + "\r\n";
        else
            return "";
    }

    public boolean tryToUploadNextChunk(byte[] buf, int len) {
        try {
            // send the bytes over the socket
            out.write(buf, 0, len);
            return true;
        } catch (IOException e) {
            haltWithError(e.getMessage());
            return false;
        }
    }

    protected boolean finalizeUpload() {
        // send the multipart/form-data footer (if we're doing this encoding)
        if(multipartFormDataField != null) {
            String footer = "\r\n--" + BOUNDARY + "--\r\n";
            try {
                out.write(footer.getBytes());
            } catch (IOException e) {
                haltWithError(e.getMessage());
                return false;
            }
        }

        // check the response to make sure we get a 2xx code back
        try {
            byte[] buf = new byte[1024];
            int n = sock.getInputStream().read(buf, 0, 13);
            if(n != 13)
                throw new IOException("bad response");
            else {
                // response starts with: "HTTP/1.0 XYZ " where XYZ is the code
                String strCode = new String(buf, 9, 3);
                int code = Integer.parseInt(strCode);
                if(code>=200 && code<300) {
                    // be nice and read the rest of their response
                    HTTPUploadListener listener = eventListener;
                    String resp = "";
                    while(n > 0) {
                        if(listener != null)
                            resp += new String(buf, 0, n); // only save the response if have to share it
                        n = sock.getInputStream().read(buf, 0, 1024);
                    }

                    // if something is listening, fire an event to let them know about the response
                    if(listener != null) {
                        String errMsg = listener.responseReceived(this, code, resp);
                        if(errMsg != null) {
                            // something in the response indicated a failure ...
                            haltWithError(errMsg);
                            return false;
                        }
                    }
                    return true;
                }
                else {
                    if(code >= 500)
                        haltWithError("server error (" + code + ")");
                    else
                        haltWithError("bad response (" + code + ")");
                    return false;
                }
            }
        }
        catch(IOException e) {
            haltWithError(e.getMessage());
            return false;
        }
        catch(NumberFormatException e) {
            haltWithError("bad response code");
            return false;
        }
    }

    protected void uploadCanceled() {
        try { if( out!=null)  out.close(); } catch (IOException e) {}
        try { if(sock!=null) sock.close(); } catch (IOException e) {}
        out = null;
        sock = null;
    }

    /** gets the object listening for upload events (may be null) */
    public HTTPUploadListener getEventListener() {
        return eventListener;
    }

    /**
     * Sets the object to be notified of upload events (responses).  Note that
     * callbacks will be issued from an upload thread NOT the Swing EDT.  Use
     * SwingUtilities.invokeLater() if you need to run on the EDT.
     *
     * This method is thread-safe (i.e., you can set the event listener from
     * any thread).
     */
    public void setEventListener(HTTPUploadListener l) {
        eventListener = l;
    }
}
