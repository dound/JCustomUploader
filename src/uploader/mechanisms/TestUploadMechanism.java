package uploader.mechanisms;

import java.util.Random;


/**
 * Pretends to upload files.  Intended to help test code which uses
 * UploadMechanism.
 *
 * @author David Underhill
 */
public class TestUploadMechanism extends AbstractUploadMechanism {
    private final Random rand;
    private long chunkXferTime_ms;
    private double chanceStartFails;
    private double chanceNextChunkFails;

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

    public long tryToStartUpload() {
        if(rand.nextFloat() < chanceStartFails) {
            haltWithError("random start failure");
            return -1;
        }
        return getFileSize();
    }

    public long tryToUploadNextChunk(long numBytesToUpload) {
        if(rand.nextFloat() < chanceNextChunkFails) {
            haltWithError("random upload failure");
            return -1;
        }
        else {
            try { Thread.sleep(this.chunkXferTime_ms); } catch(InterruptedException e) {}
            return numBytesToUpload;
        }
    }
}
