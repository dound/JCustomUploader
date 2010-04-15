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

    /** weight of the previous upload rate when computing a new upload rate */
    private static final double ALPHA = 0.5;

    /**
     * Maximum size file which will be accepted.  Note: This can be bypassed if
     * the file is changed between when we are asked to upload it and the time
     * the upload actually starts.
     */
    private static final long MAX_FILE_SIZE_ALLOWED_MB = 16;


    /** UI for this upload manager */
    private final UploaderPanel uploaderUI;

    /** the object which will actually uploads each file */
    private final UploadMechanism uploadMech;

    /** list of items to upload */
    private final LinkedList<UploadItem> uploadQueue = new LinkedList<UploadItem>();
    private final LinkedList<UploadItem> failedList = new LinkedList<UploadItem>();
    private final LinkedList<UploadItem> completedList = new LinkedList<UploadItem>();

    /** whether uploading is enabled */
    private volatile boolean uploadingEnabled = true;

    /** the item currently being uploaded, if any */
    private volatile UploadItem itemBeingUploaded = null;

    /** statistics about pending/completed uploads */
    private int numPhotosUploaded = 0;
    private volatile long numBytesLeftToUpload = 0;
    private double recentUploadRate_Bps = 0;

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

        // check the file size just in case it changed since the user added it
        long actualSize = uploadMech.getSizeOfCurrentUpload();
        if(item.length() != actualSize) {
            long diff = actualSize - item.length();
            item.setItemSize(actualSize);
            incrNumBytesLeftToUpload(diff);
            updateProgressTexts();
        }

        // loop until the upload is canceled or done
        long prevTime = System.currentTimeMillis(), now;
        double curRate_Bps = 0;
        long bytes_uploaded = 0;
        while(item!=null) {
            // upload the next chunk of this item
            bytes_uploaded = uploadMech.uploadNextChunk(CHUNK_SIZE);
            if(bytes_uploaded == -1L) {
                cancelCurrentUpload(uploadMech.getErrorText());
                return;
            }
            else {
                item.incrNumBytesUploaded(bytes_uploaded);
                incrNumBytesLeftToUpload(-bytes_uploaded);
                now = System.currentTimeMillis();
                curRate_Bps = (1000.0*bytes_uploaded) / (now - prevTime);
                prevTime = now;
                recentUploadRate_Bps = (recentUploadRate_Bps*ALPHA) + (1.0-ALPHA)*curRate_Bps;
                updateProgressTexts();
            }

            // check to see if the upload is done
            if(uploadMech.isUploadComplete()) {
                synchronized(this) {
                    completedList.add(item);
                    itemBeingUploaded = null;
                }
                item.setNumBytesUploaded(item.length()); // 100% complete
                numPhotosUploaded += 1;
                updateProgressTexts();
                showComponent(uploaderUI.getUIClear());
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
        // When the upload was canceled, the remaining bytes were removed from
        // counter.  We also counted the last chunk sent before we realized the
        // item had been cancelled, so go ahead and add those bytes back so we
        // don't double-count them.
        if(bytes_uploaded > 0)
            this.incrNumBytesLeftToUpload(bytes_uploaded);

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
                setNumFailures(failedList.size());
                itemBeingUploaded.setProgressText(why, true);
            }
            long bytesLeft = itemBeingUploaded.length() - itemBeingUploaded.getNumBytesUploaded();
            incrNumBytesLeftToUpload(-bytesLeft);
            itemBeingUploaded.setFailed(true);
            itemBeingUploaded = null;
            updateProgressTexts();
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
            incrNumBytesLeftToUpload(f.length());
            updateProgressTexts();
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
                    incrNumBytesLeftToUpload(-item.length());
                    updateProgressTexts();
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
        updateProgressTexts();
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
                incrNumBytesLeftToUpload(item.length());
                itr.remove();
                uploadQueue.add(item);
            }
            uploaderUI.getUIRetry().setVisible(false);
            updateProgressTexts();
            notifyAll();
        }
        Container pnlUploadItems = uploaderUI.getUploadItemsContainer();
        pnlUploadItems.validate();
        pnlUploadItems.repaint();
    }

    /** gets the number of uploads in progress */
    private synchronized int getNumUploadsInProgress() {
        if(itemBeingUploaded == null)
            return 0;
        else
            return 1;
    }

    /** gets the items which are waiting to be uploaded or in the process of being uploaded */
    private synchronized int getNumItemsLeftToUpload() {
        return this.uploadQueue.size() + getNumUploadsInProgress();
    }

    /** updates the number of bytes left to upload */
    private synchronized void incrNumBytesLeftToUpload(long n) {
        numBytesLeftToUpload += n;
    }

    /** Simple pluralizer.  Returns s if n is 1 and s+"s" if n!=1. */
    private static final String pl(String s, int n) {
        if(n==1)
            return s;
        else
            return s + "s";
    }

    /** updates the progress texts with the latest stats */
    private synchronized void updateProgressTexts() {
        String pending;
        int itemsLeft = getNumItemsLeftToUpload();
        if(itemsLeft==0)
            pending = "Nothing to upload yet.";
        else {
            String megabytesLeft = SZ_FMT.format(numBytesLeftToUpload / 1024.0 / 1024.0 + 0.01); // never show 0.00
            pending = itemsLeft + pl(" photo",itemsLeft) + " left to upload (" + megabytesLeft + " MB).  ";

            // append the estimated time remaining (round up to the nearest minute if displaying minutes)
            if(!uploadingEnabled)
                pending += "  Uploading is currently disabled.";
            else if(itemsLeft>0 && recentUploadRate_Bps>0) {
                int secondsLeft = (int)(numBytesLeftToUpload / recentUploadRate_Bps);
                if(secondsLeft < 60)
                    pending += "Less than 1 minute remaining.";
                else if(secondsLeft < 3600) {
                    int mins = (secondsLeft+30) / 60;
                    pending += "About " + mins + pl(" minute",mins) + " remaining.";
                }
                else {
                    int hours = secondsLeft / 3600;
                    int mins  = ((secondsLeft - (hours*3600))+30) / 60;
                    pending += "About " + hours + pl(" hour",hours) + " and " + mins + pl(" minute",mins) + " remaining.";
                }
                //System.out.println("rate=" + recentUploadRate_Bps);
            }
        }

        String completed;
        if(numPhotosUploaded == 0)
            completed = "No photos uploaded yet.";
        else if(numPhotosUploaded == 1)
            completed = "1 photo has been uploaded.";
        else
            completed = numPhotosUploaded + " photos have been uploaded.";

        int itemsFailed = failedList.size();
        if(itemsFailed > 0)
            completed += "  " + itemsFailed + pl(" photo",itemsFailed) + " failed to upload.";

        setProgressTexts(pending, completed);
    }

    /** Make the requested component visible.  Executes on the Swing EDT. */
    private void showComponent(final Component c) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                c.setVisible(true);
            }
        });
    }

    /** sets the number of failures on the uploader UI from the Swing EDT */
    private void setNumFailures(final int n) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                uploaderUI.setNumberFailures(n);
            }
        });
    }

    /** sets the progress/completed texts on the uploader UI from the Swing EDT */
    private void setProgressTexts(final String pending, final String completed) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                uploaderUI.setProgressTexts(pending, completed);
            }
        });
    }
}
