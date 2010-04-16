package uploader.mechanisms;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * A skeleton UploadMechanism implementation.  Override tryToStartUpload() and
 * tryToUploadNextChunk() to complete the implementation.  uploadCanceled()
 * should also be overridden if anything needs to happen when an upload is
 * halted before it completes.
 *
 * @author David Underhill
 */
public abstract class AbstractUploadMechanism implements UploadMechanism {
    private BufferedInputStream currentUploadFile = null;
    private long sz = 0;
    private long offset = 0;
    private String err = null;

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

        // open the file
        try {
            currentUploadFile = new BufferedInputStream(new FileInputStream(f));
        } catch(FileNotFoundException e) {
            haltWithError(e.getMessage());
            return -1;
        }

        offset = 0;
        sz = f.length();
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
            this.currentUploadFile = null;
            err = null;
        }
        return actualBytes;
    }

    /** returns the offset of the next byte to send */
    protected long getOffset() {
        return offset;
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
