package uploader;

/**
 * Uploads files to SmugMug.
 *
 * @author David Underhill
 */
public class SmugMugUploadMechanism implements UploadMechanism {
    public void cancelUpload() {
        // TODO Auto-generated method stub
    }

    public String getErrorText() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isUploadComplete() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean startUpload(String fn) {
        // TODO Auto-generated method stub
        return false;
    }

    public long uploadNextChunk(long numBytesToUpload) {
        // TODO Auto-generated method stub
        return 0;
    }

    public long getSizeOfCurrentUpload() {
        // TODO
        return 0;
    }
}
