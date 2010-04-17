package uploader.mechanisms.event;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import uploader.util.Pair;

/**
 * Handles constructing an InputStream for the file to upload.
 *
 * @author David Underhill
 */
public class UploadFileGetter {
    /** returns an InputStream for file f and the size of the file */
    public Pair<InputStream, Long> getInputStream(File f) throws IOException {
        return new Pair<InputStream, Long>(new FileInputStream(f), f.length());
    }
}
