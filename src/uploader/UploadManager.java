package uploader;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import uploader.mechanisms.UploadMechanism;

/**
 * An which manages the upload queue and the thread(s) which service that queue.
 * It refreshes the UI as uploads progress and handles requests to upload new
 * items or cancel uploads.
 *
 * Note: UploadManager updates UploadItem fields.  This is done in a thread-safe
 * manner.  Updates which modify UI components will be run asynchronously on the
 * Swing EDT.
 *
 * @author David Underhill
 */
public class UploadManager {
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

    /** the name of the items in the queue (a generic name might be 'item') */
    private final String itemType;

    /** UI for this upload manager */
    private final UploaderPanel uploaderUI;

    /** list of items to upload */
    private final LinkedList<UploadItem> uploadQueue = new LinkedList<UploadItem>();
    private final LinkedList<UploadItem> failedList = new LinkedList<UploadItem>();
    private final LinkedList<UploadItem> completedList = new LinkedList<UploadItem>();

    /** whether uploading is enabled */
    private volatile boolean uploadingEnabled = true;

    /** shared statistics about pending/completed uploads */
    private volatile int    numItemsUploaded     = 0;
    private volatile long   numBytesLeftToUpload = 0;

    private final UploaderThread[] uploaderThreads;
    private final Object lock = new Object(); // could use UploadManager.this, but will use this for clarity instead

    /**
     * Thread responsible for uploading files (one at a time).  Shares state
     * in UploadManager with other UploaderThreads (e.g., the queue of pending
     * uploads).
     */
    private class UploaderThread extends Thread {
        /** the object which will actually uploads each file */
        private final UploadMechanism uploadMech;

        /** the item currently being uploaded, if any */
        private volatile UploadItem itemBeingUploaded = null;

        /** per-thread statistic: upload rate */
        private volatile double recentUploadRate_Bps = 0;

        /** constructs a new thread which will upload items with the specified mechanism */
        public UploaderThread(UploadMechanism uploadMech) {
            this.uploadMech = uploadMech;
        }

        /**
         * Uploader thread main body.  Whenever uploading is enabled and there
         * is an item to upload, uploading will take place.  If uploading is
         * disabled, then the thread will stop uploading ASAP.
         */
        public void run() {
            while(true) {
                try {
                    synchronized(lock) {
                        while(!uploadingEnabled || uploadQueue.size()==0) {
                            lock.wait(); // wait until we're allowed to upload AND we have something to upload
                        }
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
            synchronized(lock) {
                if(itemBeingUploaded==null && uploadQueue.size()>0) {
                    UploadItem item = uploadQueue.removeFirst();
                    itemBeingUploaded = item;
                }
            }
        }

        private void uploadOneItem() {
            UploadItem item;
            synchronized(lock) {
                item = itemBeingUploaded;
            }
            if(item == null) {
                // item's upload has been canceled: we never started, so bail now
                return;
            }

            // initialize the upload process
            long actualSize = uploadMech.startUpload(item.getFilename());
            if(actualSize < 0) {
                cancelCurrentUpload(uploadMech.getErrorText());
                return;
            }

            // check the file size just in case it changed since the user added it
            if(item.length() != actualSize) {
                long diff = actualSize - item.length();
                item.setItemSize(actualSize);
                incrNumBytesLeftToUpload(diff);
                updateProgressTexts();
            }

            // loop until the upload is canceled or done
            long prevTime = System.currentTimeMillis(), now;
            double curRate_Bps = 0;
            long bytesUploaded = 0;
            long totalBytesUploaded = 0;
            while(item!=null) {
                // If this should be our last chunk, optimistically update the
                // GUI.  Otherwise, it may look like the upload stalled at X%
                // (though it hasn't) while we wait for the server's response.
                if(actualSize-totalBytesUploaded < CHUNK_SIZE) {
                    final UploadItem tmp = item;
                    SwingUtilities.invokeLater(new Runnable() {
                       public void run() {
                           tmp.setProgressText("finalizing ...", false);
                           tmp.repaint();
                       }
                    });
                }

                // upload the next chunk of this item
                bytesUploaded = uploadMech.uploadNextChunk(CHUNK_SIZE);
                if(bytesUploaded == -1L) {
                    cancelCurrentUpload(uploadMech.getErrorText());
                    return;
                }
                else {
                    totalBytesUploaded += bytesUploaded;
                    item.setNumBytesUploaded(totalBytesUploaded);
                    incrNumBytesLeftToUpload(-bytesUploaded);
                    now = System.currentTimeMillis();
                    curRate_Bps = (1000.0*bytesUploaded) / (now - prevTime);
                    prevTime = now;
                    recentUploadRate_Bps = (recentUploadRate_Bps*ALPHA) + (1.0-ALPHA)*curRate_Bps;
                    updateProgressTexts();
                }

                // check to see if the upload is done
                if(uploadMech.isUploadComplete()) {
                    synchronized(lock) {
                        completedList.add(item);
                        itemBeingUploaded = null;
                        numItemsUploaded += 1;
                    }
                    item.setNumBytesUploaded(item.length()); // 100% complete
                    updateProgressTexts();
                    showComponent(uploaderUI.getUIClear());
                    return;
                }

                // Pause the upload if uploading is disabled.  Also, get the latest
                // value of itemBeingUploaded (it will change to null if the upload
                // of the current item has been canceled).
                synchronized(lock) {
                    while(!uploadingEnabled) {
                        try { lock.wait(); } catch(InterruptedException e) {}
                    }
                    item = itemBeingUploaded;
                }
            }

            // When the upload was canceled, the remaining bytes were removed from
            // counter.  We also counted the last chunk sent before we realized the
            // item had been cancelled, so go ahead and add those bytes back so we
            // don't double-count them.
            if(bytesUploaded > 0)
                incrNumBytesLeftToUpload(bytesUploaded);

            // the item's upload has been canceled, but we've partially uploaded it
            uploadMech.cancelUpload();
        }

        /**
         * Cancels the current upload, if any.  If why is not null, then the item
         * is added to the failed list.
         */
        private void cancelCurrentUpload(String why) {
            synchronized(lock) {
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
        }
    }

    /**
     * Constructs a new UploadManager which will manage uploads using the
     * specified upload mechanisms.
     *
     * @param uploaderUI    the UI this manager works for
     * @param itemType      text describing what kind of items are being uploaded
     * @param uploadsMechs  how to upload files (one per thread we should use)
     */
    public UploadManager(final UploaderPanel uploaderUI, String itemType, final UploadMechanism[] uploadMechs) {
        assert SwingUtilities.isEventDispatchThread();
        this.uploaderUI = uploaderUI;
        this.itemType = itemType;

        uploaderThreads = new UploaderThread[uploadMechs.length];
        for(int i=0; i<uploadMechs.length; i++)
            uploaderThreads[i] = new UploaderThread(uploadMechs[i]);
    }

    /** Starts all of the uploader thread(s). */
    public void start() {
        for(UploaderThread ut : uploaderThreads)
            ut.start();
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

        UploadItem item = new UploadItem(this, f.getPath(), f.getName(), f.length());
        this.uploaderUI.makeDropTarget(item);
        Container pnlUploadItems = uploaderUI.getUploadItemsContainer();
        pnlUploadItems.add(item);
        pnlUploadItems.validate();

        synchronized(lock) {
            uploadQueue.add(item);
            incrNumBytesLeftToUpload(f.length());
            updateProgressTexts();
            lock.notifyAll();
        }
    }

    /**
     * Removes an item from the upload queue.
     *
     * May ONLY be called from the Swing event dispatch thread (may modify pnlUploadItems).
     */
    public void removeItemToUpload(UploadItem item) {
        assert SwingUtilities.isEventDispatchThread();
        synchronized(lock) {
            boolean wasInProgress = false;
            for(UploaderThread ut : uploaderThreads) {
                if(item == ut.itemBeingUploaded) {
                    ut.cancelCurrentUpload(null); // try to halt the upload in progress
                    wasInProgress = true;
                    break;
                }
            }
            if(!wasInProgress) {
                if(uploadQueue.remove(item)) { // is it in the upload queue? (e..g, hasn't started yet)
                    incrNumBytesLeftToUpload(-item.length());
                }
                else if(failedList.remove(item)) { // is it in the failed queue?
                    setNumFailures(failedList.size());
                }
                else {
                    // too late: it has already been uploaded
                    return;
                }
                updateProgressTexts();
            }
        }

        item.setProgressText("canceled by user", true);
        Container pnlUploadItems = uploaderUI.getUploadItemsContainer();
        pnlUploadItems.remove(item);
        pnlUploadItems.validate();
        pnlUploadItems.repaint();
    }

    /** sets whether uploads may be done */
    public void setUploadingEnabled(boolean b) {
        synchronized(lock) {
            this.uploadingEnabled = b;
            updateProgressTexts();
            lock.notifyAll();
        }
    }

    /**
     * Clears completed items.
     *
     * May ONLY be called from the Swing event dispatch thread (may modify pnlUploadItems).
     */
    public void clearCompletedItems() {
        assert SwingUtilities.isEventDispatchThread();
        Container pnlUploadItems = uploaderUI.getUploadItemsContainer();
        synchronized(lock) {
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
        synchronized(lock) {
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
            lock.notifyAll();
        }
        Container pnlUploadItems = uploaderUI.getUploadItemsContainer();
        pnlUploadItems.validate();
        pnlUploadItems.repaint();
    }

    /** gets the number of uploads in progress */
    private int getNumUploadsInProgress() {
        synchronized(lock) {
            int count = 0;
            for(UploaderThread ut : uploaderThreads)
                if(ut.itemBeingUploaded != null)
                    count += 1;
            return count;
        }
    }

    /** gets the items which are waiting to be uploaded or in the process of being uploaded */
    private int getNumItemsLeftToUpload() {
        synchronized(lock) {
            return this.uploadQueue.size() + getNumUploadsInProgress();
        }
    }

    /** updates the number of bytes left to upload */
    private void incrNumBytesLeftToUpload(long n) {
        synchronized(lock) {
            numBytesLeftToUpload += n;
        }
    }

    /** Simple pluralizer.  Returns s if n is 1 and s+"s" if n!=1. */
    private static final String pl(String s, int n) {
        if(n==1)
            return s;
        else
            return s + "s";
    }

    /** updates the progress texts with the latest stats */
    private void updateProgressTexts() {
        double totRecentUploadRate_Bps;
        int itemsLeft;
        int itemsFailed;
        long numBytesLeftToUploadCopy;
        int numItemsUploadedCopy;

        // get a copy of all the info we need up front => minimize the critical section size
        synchronized(lock) {
            // compute the average upload rate
            totRecentUploadRate_Bps = 0;
            for(UploaderThread ut : uploaderThreads)
                totRecentUploadRate_Bps += ut.recentUploadRate_Bps;

            itemsLeft = getNumItemsLeftToUpload();
            itemsFailed = failedList.size();
            numBytesLeftToUploadCopy = numBytesLeftToUpload;
            numItemsUploadedCopy = numItemsUploaded;
        }

        String pending;
        if(itemsLeft==0)
            pending = "Nothing to upload.";
        else {
            String megabytesLeft = SZ_FMT.format(numBytesLeftToUploadCopy / 1024.0 / 1024.0 + 0.01); // never show 0.00
            pending = itemsLeft + pl(" "+itemType,itemsLeft) + " left (" + megabytesLeft + " MB).  ";

            // append the estimated time remaining (round up to the nearest minute if displaying minutes)
            if(!uploadingEnabled)
                pending += "  Uploading is currently disabled.";
            else if(itemsLeft>0 && totRecentUploadRate_Bps>0) {
                int secondsLeft = (int)(numBytesLeftToUploadCopy / totRecentUploadRate_Bps);
                if(secondsLeft < 60)
                    pending += "Less than 1 minute";
                else if(secondsLeft < 3600) {
                    int mins = (secondsLeft+30) / 60;
                    pending += "About " + mins + pl(" minute",mins);
                }
                else {
                    int hours = secondsLeft / 3600;
                    int mins  = ((secondsLeft - (hours*3600))+30) / 60;
                    pending += "About " + hours + pl(" hour",hours) + " and " + mins + pl(" minute",mins);
                }
                int kbps = (int)(8 * totRecentUploadRate_Bps / 1000);
                pending += " remaining at " + kbps + " kb/s";
            }
        }

        String completed;
        if(numItemsUploadedCopy == 0)
            completed = "Nothing uploaded yet.";
        else if(numItemsUploadedCopy == 1)
            completed = "1 " + itemType + " has been uploaded.";
        else
            completed = numItemsUploadedCopy + " " + itemType + "s have been uploaded.";

        if(itemsFailed > 0)
            completed += "  " + itemsFailed + pl(" "+itemType,itemsFailed) + " failed to upload.";

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
