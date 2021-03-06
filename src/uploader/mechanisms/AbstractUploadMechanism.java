package uploader.mechanisms;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;

import uploader.mechanisms.event.UploadFileGetter;
import uploader.mechanisms.event.UploadFileGetter.InputStreamAndSizePair;

/**
 * A skeleton UploadMechanism implementation.  Override tryToStartUpload() and
 * tryToUploadNextChunk() to complete the implementation.  uploadCanceled()
 * should also be overridden if anything needs to happen when an upload is
 * halted before it completes.  finalizeUpload() should also be overridden if
 * your mechanism needs to do anything after you've uploaded the last of the
 * file (e.g., upload a trailer or check the server's response).
 *
 * @author David Underhill
 */
public abstract class AbstractUploadMechanism implements UploadMechanism {
    private BufferedInputStream currentUploadFile = null;
    private File currentUploadFileObj = null;
    private long sz = 0;
    private long offset = 0;
    private String err = null;

    /** object which handles getting the file */
    private UploadFileGetter fileGetter = new UploadFileGetter();

    /** buffer to store file data */
    private final byte[] buffer;

    /** constructs an AbstractUploadMechanism with an internal buffer of 4096B */
    public AbstractUploadMechanism() {
        this(4096);
    }

    /** constructs an AbstractUploadMechanism with an internal buffer of buf_sz bytes */
    public AbstractUploadMechanism(int buf_sz) {
        this.buffer = new byte[buf_sz];
    }

    public final void cancelUpload() {
        haltWithError("canceled");
    }

    /** closes the current file (currentUploadFile MUST NOT be null) */
    private void closeFile() {
        try {
            this.currentUploadFile.close();
        }
        catch(IOException e) {
            // ignore it
        }
    }

    public String getErrorText() {
        return err;
    }

    protected void haltWithError(String err) {
        this.err = err;
        offset = -1;
        closeFile();
        uploadCanceled();
        currentUploadFile = null;
        currentUploadFileObj = null;
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
        currentUploadFileObj = f;

        // open the file
        try {
            InputStreamAndSizePair ret = fileGetter.getInputStream(f);
            currentUploadFile = new BufferedInputStream(ret.input);
            sz = ret.length;
        } catch(IOException e) {
            haltWithError(e.getMessage());
            return -1;
        }

        offset = 0;
        err = null;

        if(!tryToStartUpload(f)) {
            if(err==null)
                haltWithError("upload failed to start"); // provide a generic error if tryToStartUpload() did not
            return -1;
        }
        return sz;
    }

    public final long uploadNextChunk(long numBytesToUpload) {
        if(currentUploadFile == null) {
            err = "no upload is in progress";
            return -1;
        }

        long max = sz - offset;
        long maxBytesWeCanUpload = Math.min(max, numBytesToUpload);

        int actualBytes = 0;
        if(maxBytesWeCanUpload > 0) {
            // read in the max number of bytes we can
            try {
                actualBytes = currentUploadFile.read(buffer, 0, (int)maxBytesWeCanUpload);
            } catch (IOException e) {
                haltWithError(e.getMessage());
                return -1;
            }
            if(actualBytes < 0) {
                haltWithError("unexpected end of file");
                return -1;
            }

            // send the bytes
            if(!tryToUploadNextChunk(buffer, actualBytes)) {
                // set a generic error message if tryToUploadNextChunk() didn't
                if(err == null)
                    haltWithError("upload failed");
                return -1;
            }
            offset += actualBytes;
        }

        if(this.isUploadComplete()) {
            if(!finalizeUpload()) {
                if(err==null)
                    haltWithError("couldn't finalize"); // generic error if the subclass didn't set one
                return -1;
            }

            closeFile();
            currentUploadFile = null;
            currentUploadFileObj = null;
            err = null;
        }
        return actualBytes;
    }

    /** returns the offset of the next byte to send */
    protected long getOffset() {
        return offset;
    }

    /**
     * Returns the File object associated with the file currently being uploaded
     * or null if no upload is in progress.
     */
    public final File getFile() {
        return currentUploadFileObj;
    }

    /** returns the size of the file being uploaded (0 if none) */
    protected long getFileSize() {
        return sz;
    }

    /**
     * Returns the object used to get the file for upload.
     */
    protected UploadFileGetter getUploadFileGetter() {
        return fileGetter;
    }

    /**
     * Sets the object which will be used to get the file for upload.  The
     * default file getter simply returns the file itself (buffered).  It may
     * be overridden if some pre-processing should be done to the file.
     */
    public void setUploadFileGetter(UploadFileGetter fileGetter) {
        this.fileGetter = fileGetter;
    }

    /**
     * Called by startUpload().  Will not be called if an upload is not in
     * progress.  The callee should call haltWithError() if returning an error
     * code (otherwise a generic error message will be set).
     *
     * @return true if the upload has been started successfully
     */
    protected abstract boolean tryToStartUpload(File f);

    /**
     * Called by uploadNextChunk().  Will not be called if an upload is not in
     * progress.  The callee should call haltWithError() if returning an error
     * code (otherwise a generic error message will be set).
     *
     * @param buf  contains the bytes to upload
     * @param len  the number of bytes to upload; always greater than 0
     *
     * @return true if the bytes were successfully uploaded
     */
    protected abstract boolean tryToUploadNextChunk(byte[] buf, int len);

    /**
     * Called when all bytes have been successfully sent.
     * @return true if the upload has succeeded
     */
    protected boolean finalizeUpload() { return true; }

    /** Called when an upload is halted or canceled. */
    protected void uploadCanceled() {}
}
