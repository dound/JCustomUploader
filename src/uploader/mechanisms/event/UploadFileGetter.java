package uploader.mechanisms.event;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Handles constructing an InputStream for the file to upload.
 *
 * @author David Underhill
 */
public class UploadFileGetter {
    /** returns an InputStream for file f and the size of the file */
    public InputStreamAndSizePair getInputStream(File f) throws IOException {
        return new InputStreamAndSizePair(new FileInputStream(f), f.length());
    }

    /** contains an input stream and the length of the data on that stream */
    public class InputStreamAndSizePair {
        public final InputStream input;
        public final long length;
        public InputStreamAndSizePair(final InputStream input, final long length ) {
            this.input = input;
            this.length = length;
        }
    }
}
