package uploader.mechanisms.event;

import uploader.mechanisms.HTTPUploadMechanism;

/**
 * Callbacks which fire for certain events during the uploading process.
 *
 * @author David Underhill
 */
public interface HTTPUploadListener {
    /**
     * Called when a response has been received.  This is only called if code
     * indicates a successful response (i.e., 2xx series code).
     *
     * @return null if the upload succeeded, a string describing why it failed otherwise
     */
    public String responseReceived(HTTPUploadMechanism u, int code, String resp);
}
