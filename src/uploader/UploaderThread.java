package uploader;

import java.io.File;
import java.util.Vector;

import javax.swing.JPanel;

public class UploaderThread extends Thread {
    /** UI container which holds UploadItems */
    private final JPanel uploadList;

    /** list of items to upload */
    private Vector<UploadItem> itemsToUpload = new Vector<UploadItem>();

    /** whether uploading is enabled */
    private boolean uploadingEnabled = true;

    public UploaderThread(final JPanel p) {
        uploadList = p;
    }

    public void run() {
        // TODO
    }

    /** adds an item to the upload queue */
    public void addFileToUpload(File f) {
        UploadItem item = new UploadItem(this, f.getPath(), f.length());
        itemsToUpload.add(item);
        uploadList.add(item);
        uploadList.validate();
    }

    /** removes an item from the upload queue */
    public void removeItemToUpload(UploadItem item) {
        itemsToUpload.remove(item);
        uploadList.remove(item);
        uploadList.validate();
        uploadList.repaint();
    }

    /** sets whether uploads may be done */
    public void setUploadingEnabled(boolean b) {
        this.uploadingEnabled = b;
    }
}
