package uploader;

import java.io.File;
import java.util.LinkedList;

import javax.swing.JPanel;

/**
 * A thread which manages the upload queue.  It refreshes the UI as uploads
 * progress and handles requests to upload new items or cancel uploads.
 *
 * @author David Underhill
 */
public class UploadManager extends Thread {
    /** UI container which holds UploadItems */
    private final JPanel pnlUploadItems;

    /** list of items to upload */
    private final LinkedList<UploadItem> uploadQueue = new LinkedList<UploadItem>();

    /** whether uploading is enabled */
    private volatile boolean uploadingEnabled = true;

    /** the item currently being uploaded, if any */
    private volatile UploadItem itemBeingUploaded = null;

    /** constructs a new UploaderThread which will put new UploadItems in p. */
    public UploadManager(final JPanel p) {
        pnlUploadItems = p;
    }

    /**
     * Starts the uploader thread.  Whenever uploading is enabled and there is
     * an item to upload, uploading will take place.  If uploading is disabled,
     * then the thread will stop uploading ASAP.
     */
    public void run() {
        while(true) {
            try {
                synchronized(this) {
                    while(!uploadingEnabled || uploadQueue.size()==0)
                        wait(); // wait until we're allowed to upload AND we have something to upload
                }

                if(uploadingEnabled) {
                    prepareNextUpload();
                    uploadOneItem();
                }
            } catch(InterruptedException e) {
                // ignore it
            }
        }
    }

    /**
     * If no item is being uploaded, then the next item from the upload queue
     * is moved out of the queue and set to itemBeingUploaded.
     */
    private void prepareNextUpload() {
        synchronized(this) {
            if(itemBeingUploaded==null && uploadQueue.size()>0) {
                UploadItem item = uploadQueue.removeFirst();
                itemBeingUploaded = item;
            }
        }
    }

    private void uploadOneItem() {
        UploadItem item;
        synchronized(this) {
            item = itemBeingUploaded;
        }
        if(item == null) {
            // item's upload has been canceled: we never started, so bail now
            return;
        }

        // initialize the upload process
        // TODO: do whatever is required to start an upload

        // loop until the upload is canceled or done
        while(item!=null) {
            // upload the next chunk of this item
            // TODO: upload a chunk!

            // check to see if the upload is done
            if(item.isUploaded()) {
                synchronized(this) {
                    itemBeingUploaded = null;
                }
                return;
            }

            // get the latest value of itemBeingUploaded (it will change to null
            // if the upload should be stopped)
            synchronized(this) {
                item = itemBeingUploaded;
            }
        }

        // the item's upload has been canceled, but we've partially uploaded it
        // TODO: tell the server that the upload has been canceled
    }

    /** adds an item to the upload queue */
    public void addFileToUpload(File f) {
        if(f.length() <= 0)
            return; // can't upload an empty file

        UploadItem item = new UploadItem(this, f.getPath(), f.length());
        pnlUploadItems.add(item);
        pnlUploadItems.validate();

        synchronized(this) {
            uploadQueue.add(item);
            notifyAll();
        }
    }

    /** removes an item from the upload queue */
    public void removeItemToUpload(UploadItem item) {
        pnlUploadItems.remove(item);
        pnlUploadItems.validate();
        pnlUploadItems.repaint();

        synchronized(this) {
            if(item == itemBeingUploaded)
                itemBeingUploaded = null;   // try to halt the upload in progress
            else
                uploadQueue.remove(item); // remove it from the upload queue (hasn't started yet)
        }
    }

    /** sets whether uploads may be done */
    public synchronized void setUploadingEnabled(boolean b) {
        this.uploadingEnabled = b;
        notifyAll();
    }
}
