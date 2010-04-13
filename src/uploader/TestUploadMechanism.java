package uploader;

import java.io.File;
import java.util.Random;

/**
 * Pretends to upload files.  Intended to help test code which uses
 * UploadMechanism.
 *
 * @author David Underhill
 */
public class TestUploadMechanism implements UploadMechanism {
    private final Random rand;
    private long chunkXferTime_ms;
    private double chanceStartFails;
    private double chanceNextChunkFails;

    private String currentUpload = null;
    private long sz = 0;
    private long offset = 0;
    private String err = "";

    /**
     * Constructs a test upload mechanism with the specified failure chances
     * with random seeded to the current time.
     */
    public TestUploadMechanism(long chunkXferTime_ms, double chanceStartFails, double chanceNextChunkFails) {
        this(chunkXferTime_ms, chanceStartFails, chanceNextChunkFails, System.currentTimeMillis());
    }

    /**
     * Constructs a test upload mechanism with the specified chunk transfer
     * time (how long uploadNextChunk() will block for), failure chances,
     * and random seed.  Failure chances should be in the range [0.0, 1.0]
     * (e.g., 0.25 => 25% chance of failure).  chanceNextChunkFails will be
     * called a lot for large files, so you may want to consider using a smaller
     * value for this.
     */
    public TestUploadMechanism(long chunkXferTime_ms, double chanceStartFails, double chanceNextChunkFails, long seed) {
        this.chunkXferTime_ms = chunkXferTime_ms;
        this.chanceStartFails = chanceStartFails;
        this.chanceNextChunkFails = chanceNextChunkFails;
        this.rand = new Random(seed);
    }

    public void cancelUpload() {
        haltWithError("canceled");
    }

    public String getErrorText() {
        return err;
    }

    public long getSizeOfCurrentUpload() {
        return sz;
    }

    public boolean isUploadComplete() {
        return sz == offset;
    }

    public boolean startUpload(String fn) {
        if(currentUpload == null) {
            haltWithError("no upload is in progress");
            return false;
        }

        if(rand.nextFloat() < chanceStartFails) {
            haltWithError("random start failure");
            return false;
        }
        else {
            File f = new File(fn);
            if(!f.exists()) {
                haltWithError("does not exist: " + fn);
                return false;
            }
            else if(!f.isFile()) {
                haltWithError("not a file: " + fn);
                return false;
            }

            offset = 0;
            sz = f.length();
            return true;
        }
    }

    public long uploadNextChunk(long numBytesToUpload) {
        if(currentUpload == null) {
            err = "no upload is in progress";
            return -1;
        }

        if(rand.nextFloat() < chanceNextChunkFails) {
            haltWithError("random chunk upload failure");
            return -1;
        }
        else {
            long max = sz - offset;
            offset += max;
            if(max > 0) {
                try { Thread.sleep(this.chunkXferTime_ms); } catch(InterruptedException e) {}
            }
            return Math.min(max, numBytesToUpload);
        }
    }

    private void haltWithError(String err) {
        this.err = err;
        this.currentUpload = null;
    }
}
