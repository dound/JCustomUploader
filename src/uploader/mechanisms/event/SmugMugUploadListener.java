package uploader.mechanisms.event;

import uploader.mechanisms.SmugMugUploadMechanism;

/**
 * Callbacks which fire for certain events during the uploading process.
 *
 * @author David Underhill
 */
public interface SmugMugUploadListener {
    /**
     * Called when a response has been received which confirms that an upload
     * was successful.
     *
     * @param imageID   as returned by the SmugMug API
     * @param imageKey  as returned by the SmugMug API
     * @param imageURL  as returned by the SmugMug API
     */
    public void responseReceived(SmugMugUploadMechanism u, String imageID, String imageKey, String imageURL);
}
