package uploader;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * A thread which manages the upload queue.  It refreshes the UI as uploads
 * progress and handles requests to upload new items or cancel uploads.
 *
 * @author David Underhill
 */
public class UploadManager extends Thread {
    private static final DecimalFormat SZ_FMT = new DecimalFormat("0.00");
    private static final long CHUNK_SIZE = 4096;

    /**
     * Maximum size file which will be accepted.  Note: This can be bypassed if
     * the file is changed between when we are asked to upload it and the time
     * the upload actually starts.
     */
    private static final long MAX_FILE_SIZE_ALLOWED_MB = 16;


    /** UI for this upload manager */
    private final UploaderPanel uploaderUI;

    /** the object which will actually uploads each file */
    final UploadMechanism uploadMech;

    /** list of items to upload */
    private final LinkedList<UploadItem> uploadQueue = new LinkedList<UploadItem>();
    private final LinkedList<UploadItem> failedList = new LinkedList<UploadItem>();
    private final LinkedList<UploadItem> completedList = new LinkedList<UploadItem>();

    /** whether uploading is enabled */
    private volatile boolean uploadingEnabled = true;

    /** the item currently being uploaded, if any */
    private volatile UploadItem itemBeingUploaded = null;

    /**
     * Constructs a new UploadManager which will manage uploads using the
     * specified upload mechanism.  It will provide callbacks on the Swing event
     * dispatch thread when UI elements need updating.  It also
     * provides methods which can modify p to show the list of items being
     * uploaded, but these methods are not referenced by this thread - they may
     * only be called from the event dispatch thread.  This constructor may only
     * be called from the event dispatch thread too.
     */
    public UploadManager(final UploaderPanel uploaderUI, final UploadMechanism uploadMech) {
        assert SwingUtilities.isEventDispatchThread();
        this.uploaderUI = uploaderUI;
        this.uploadMech = uploadMech;
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
        if(!uploadMech.startUpload(item.getFilename())) {
            cancelCurrentUpload(uploadMech.getErrorText());
            return;
        }
        item.setItemSize(uploadMech.getSizeOfCurrentUpload()); // just in case the file size changed

        // loop until the upload is canceled or done
        while(item!=null) {
            // upload the next chunk of this item
            long bytes_uploaded = uploadMech.uploadNextChunk(CHUNK_SIZE);
            if(bytes_uploaded == -1L) {
                cancelCurrentUpload(uploadMech.getErrorText());
                return;
            }
            else
                item.incrNumBytesUploaded(bytes_uploaded);

            // check to see if the upload is done
            if(uploadMech.isUploadComplete()) {
                synchronized(this) {
                    item.setNumBytesUploaded(item.length()); // 100% complete
                    completedList.add(item);
                    SwingUtilities.invokeLater(new ShowComponent(uploaderUI.getUIClear()));
                    itemBeingUploaded = null;
                }
                return;
            }

            // Pause the upload if uploading is disabled.  Also, get the latest
            // value of itemBeingUploaded (it will change to null if the upload
            // of the current item has been canceled).
            synchronized(this) {
                while(!uploadingEnabled) {
                    try { wait(); } catch(InterruptedException e) {}
                }
                item = itemBeingUploaded;
            }
        }

        // the item's upload has been canceled, but we've partially uploaded it
        uploadMech.cancelUpload();
    }

    /**
     * Cancels the current upload, if any.  If why is not null, then the item
     * is added to the failed list.
     */
    private synchronized void cancelCurrentUpload(String why) {
        if(itemBeingUploaded != null) {
            if(why != null) {
                failedList.add(itemBeingUploaded);
                SwingUtilities.invokeLater(new SetNumFailures(failedList.size()));
                itemBeingUploaded.setProgressText(why, true);
            }
            itemBeingUploaded.setFailed(true);
            itemBeingUploaded = null;
        }
    }

    /**
     * Adds an item to the upload queue.
     *
     * May ONLY be called from the Swing event dispatch thread (may modify pnlUploadItems).
     */
    public void addFileToUpload(File f) {
        assert SwingUtilities.isEventDispatchThread();
        if(f.length() <= 0)
            return; // can't upload an empty file
        else if(f.length() > MAX_FILE_SIZE_ALLOWED_MB*1024*1024) {
            //+.01=>make sure rounded number is still strictly greater than the threshold
            double sz_MB = f.length()/1024.0/1024.0 + 0.01;
            JOptionPane.showMessageDialog(null,
                    "Warning: skipping " + f.getName() + " because it is too big.\n" +
                    "\n" +
                    "Size of " + f.getName() + ": " + SZ_FMT.format(sz_MB) + "MB\n" +
                    "Max Size Allowed: " + SZ_FMT.format(MAX_FILE_SIZE_ALLOWED_MB) + "MB\n");
            return;
        }

        UploadItem item = new UploadItem(this, f.getPath(), f.length());
        Container pnlUploadItems = uploaderUI.getUploadItemsContainer();
        pnlUploadItems.add(item);
        pnlUploadItems.validate();

        synchronized(this) {
            uploadQueue.add(item);
            notifyAll();
        }
    }

    /**
     * Removes an item from the upload queue.
     *
     * May ONLY be called from the Swing event dispatch thread (may modify pnlUploadItems).
     */
    public void removeItemToUpload(UploadItem item) {
        assert SwingUtilities.isEventDispatchThread();
        synchronized(this) {
            if(item == itemBeingUploaded)
                cancelCurrentUpload(null); // try to halt the upload in progress
            else {
                try {
                    uploadQueue.remove(item); // remove it from the upload queue (hasn't started yet)
                }
                catch(NoSuchElementException e) {
                    // too late: it has already been uploaded
                    return;
                }
            }
        }

        item.setProgressText("canceled by user", true);
        Container pnlUploadItems = uploaderUI.getUploadItemsContainer();
        pnlUploadItems.remove(item);
        pnlUploadItems.validate();
        pnlUploadItems.repaint();
    }

    /** sets whether uploads may be done */
    public synchronized void setUploadingEnabled(boolean b) {
        this.uploadingEnabled = b;
        notifyAll();
    }

    /**
     * Clears completed items.
     *
     * May ONLY be called from the Swing event dispatch thread (may modify pnlUploadItems).
     */
    public void clearCompletedItems() {
        assert SwingUtilities.isEventDispatchThread();
        Container pnlUploadItems = uploaderUI.getUploadItemsContainer();
        synchronized(this) {
            ListIterator<UploadItem> itr = completedList.listIterator();
            while(itr.hasNext()) {
                UploadItem item = itr.next();
                pnlUploadItems.remove(item);
                itr.remove();
            }
            uploaderUI.getUIClear().setVisible(false);
        }
        pnlUploadItems.validate();
        pnlUploadItems.repaint();
    }

    /**
     * Put failed items back in the upload queue (at the end of the queue).
     *
     * May ONLY be called from the Swing event dispatch thread (may modify pnlUploadItems).
     */
    public void retryFailedItems() {
        assert SwingUtilities.isEventDispatchThread();
        synchronized(this) {
            ListIterator<UploadItem> itr = failedList.listIterator();
            while(itr.hasNext()) {
                UploadItem item = itr.next();
                item.setProgressText("will retry this upload", false);
                item.setFailed(false);
                item.setNumBytesUploaded(0);
                itr.remove();
                uploadQueue.add(item);
            }
            uploaderUI.getUIRetry().setVisible(false);
            notifyAll();
        }
        Container pnlUploadItems = uploaderUI.getUploadItemsContainer();
        pnlUploadItems.validate();
        pnlUploadItems.repaint();
    }

    /** a runnable which makes the requested component visible when run */
    private class ShowComponent implements Runnable {
        private Component c;
        public ShowComponent(Component c) {
            this.c = c;
        }
        public void run() {
            assert SwingUtilities.isEventDispatchThread();
            c.setVisible(true);
        }
    }

    /** runnable which sets the number of failures on the uploader UI */
    private class SetNumFailures implements Runnable {
        private int n;
        public SetNumFailures(int n) {
            this.n = n;
        }
        public void run() {
            uploaderUI.setNumberFailures(n);
        }
    }
}
