package uploader.mechanisms;

import java.io.File;

/**
 * A skeleton UploadMechanism implementation.  Override tryToStartUpload() and
 * tryToUploadNextChunk() to complete the implementation.
 *
 * @author David Underhill
 */
public abstract class AbstractUploadMechanism implements UploadMechanism {
    private File currentUploadFile = null;
    private long sz = 0;
    private long offset = 0;
    private String err = null;

    public void cancelUpload() {
        haltWithError("canceled");
    }

    public String getErrorText() {
        return err;
    }

    protected void haltWithError(String err) {
        this.err = err;
        this.currentUploadFile = null;
    }

    public final boolean isUploadComplete() {
        return sz == offset;
    }

    /**
     * Verifies that an upload is not already in progress and then gets a File
     * object representing fn and makes sure it is a valid file.  Resets the
     * error message to null, the offset to 0, and getFile() will now return
     * the file representing fn.  Calls tryToStartUpload() and returns its result.
     *
     * @return -1 on error; otherwise it returns the size of the file
     */
    public final long startUpload(String fn) {
        if(currentUploadFile != null) {
            haltWithError("an upload is already in progress");
            return -1;
        }

        File f = new File(fn);
        if(!f.exists()) {
            haltWithError("does not exist: " + fn);
            return -1;
        }
        else if(!f.isFile()) {
            haltWithError("not a file: " + fn);
            return -1;
        }

        currentUploadFile = f;
        offset = 0;
        sz = f.length(); // save the value so we don't repeatedly query the FS for it
        err = null;

        long ret = tryToStartUpload();
        if(ret<0 && err==null)
            haltWithError("upload failed to start"); // provide a generic error if tryToStartUpload() did not
        return ret;
    }

    public final long uploadNextChunk(long numBytesToUpload) {
        if(currentUploadFile == null) {
            err = "no upload is in progress";
            return -1;
        }

        long max = sz - offset;
        long maxBytesWeCanUpload = Math.min(max, numBytesToUpload);

        long actualBytesUploaded = 0;
        if(maxBytesWeCanUpload > 0) {
            actualBytesUploaded = tryToUploadNextChunk(maxBytesWeCanUpload);
            if(actualBytesUploaded < 0) {
                // set a generic error message if tryToUploadNextChunk() didn't
                if(err == null)
                    haltWithError("upload failed");
                return -1;
            }
            offset += actualBytesUploaded;
        }

        if(this.isUploadComplete()) {
            this.currentUploadFile = null;
            err = null;
        }
        return actualBytesUploaded;
    }

    /** returns the offset of the next byte to send */
    protected long getOffset() {
        return offset;
    }

    /** returns the file being uploaded (null if none) */
    protected File getFile() {
        return currentUploadFile;
    }

    /** returns the size of the file being uploaded (0 if none) */
    protected long getFileSize() {
        return sz;
    }

    /**
     * Called by startUpload().  Will not be called if an upload is not in
     * progress.  The callee should call haltWithError() if returning an error
     * code (otherwise a generic error message will be set).
     *
     * @return -1 on error; otherwise it returns the size of the file
     */
    protected abstract long tryToStartUpload();

    /**
     * Called by uploadNextChunk().  Will not be called if an upload is not in
     * progress.  The callee should call haltWithError() if returning an error
     * code (otherwise a generic error message will be set).
     *
     * @param num_bytes_to_upload  the max number of bytes to upload with this
     *                             chunk; guaranteed to not be larger than the
     *                             number of bytes left to upload.  It will
     *                             always be greater than 0.
     *
     * @return the number of bytes successfully uploaded, or -1 if the upload failed
     */
    protected abstract long tryToUploadNextChunk(long numBytesToUpload);
}
