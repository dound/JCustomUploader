package uploader;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import uploader.util.Util;

/**
 * The UI for an item being uploaded.  This includes information about the
 * current state of the upload (e.g., number of bytes uploaded).  This object
 * is thread-safe except for methods which explicitly indicate threading
 * restrictions.
 *
 * @author David Underhill
 */
public class UploadItem extends JPanel {
    private static final Color COLOR_UPLOAD_FAILED = new Color(255, 228, 228);
    private static final Color COLOR_UPLOAD_PROGRESS = new Color(212, 255, 255);
    private static final Color COLOR_UPLOAD_COMPLETE = new Color(212, 255, 212);
    private static final Color DARK_RED = new Color(64, 0, 0);
    private static final DecimalFormat SZ_FMT = new DecimalFormat("0.00");
    private static final ImageIcon ICON_CLOSE = Util.createImageIcon("/resources/close.png");
    private static final ImageIcon ICON_CLOSE_PRESSED = Util.createImageIcon("/resources/close-press.png");
    private static final ImageIcon ICON_CLOSE_HOVER = Util.createImageIcon("/resources/close-hover.png");
    private static final ImageIcon ICON_CHECKMARK = Util.createImageIcon("/resources/checkmark.png");
    private static final ImageIcon ICON_ALERT = Util.createImageIcon("/resources/alert.png");

    private final UploadManager uploader;
    private final String fn;
    private volatile long szBytes;
    private volatile long numBytesUploaded = 0;
    private volatile boolean failed = false;

    private final JLabel lblProgress = new JLabel("not yet uploaded", JLabel.RIGHT);
    private final JButton btnRemove;

    public UploadItem(UploadManager uploader, final String filename, final long sizeInBytes) {
        this.uploader = uploader;
        this.fn = filename;
        this.szBytes = sizeInBytes;

        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSz = new JLabel(SZ_FMT.format(sizeInBytes / 1024.0 / 1024.0) + " MB", JLabel.RIGHT);
        Dimension lblSzDim = new Dimension(65, lblSz.getMaximumSize().height);
        Util.setSize(lblSz, lblSzDim);
        add(lblSz);
        add(Box.createRigidArea(new Dimension(15, 0)));

        // use a text field for filename so that long names can be scrolled to view the full name
        JTextField txtFn = new JTextField();
        txtFn.setText(fn);
        txtFn.setEditable(false);
        txtFn.setBorder(null);
        txtFn.setHighlighter(null); // don't highlight selected text
        txtFn.setOpaque(false);
        Dimension lblFnDim = new Dimension(400, txtFn.getPreferredSize().height);
        Util.setSize(txtFn, lblFnDim);
        add(txtFn);

        add(Box.createHorizontalGlue());
        Dimension lblProgressDim = new Dimension(175, lblSzDim.height);
        Util.setSize(lblProgress, lblProgressDim);
        lblProgress.setHorizontalTextPosition(SwingConstants.LEFT);
        add(lblProgress);
        add(Box.createRigidArea(new Dimension(5, 0)));

        btnRemove = new JButton(ICON_CLOSE);
        btnRemove.setContentAreaFilled(false);
        btnRemove.setBorderPainted(false);
        btnRemove.setOpaque(false);
        Util.prepButtonUI(btnRemove, ICON_CLOSE_PRESSED, ICON_CLOSE_HOVER);
        btnRemove.setToolTipText("Don't upload this item");
        Dimension btnRmSz = new Dimension(17, 22);
        Util.setSize(btnRemove, btnRmSz);
        btnRemove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(!isUploaded())
                    removeItemFromUploader();
            }
        });
        add(btnRemove);
        add(Box.createRigidArea(new Dimension(5, 0)));
    }

    /**
     * Returns the size of the file.
     *
     * Thread-safe (volatile variable).
     */
    public long length() {
        return szBytes;
    }

    /**
     * Sets the size of the file (use this if the size has changed).
     *
     * Thread-safe (volatile variable and Swing thread-safe method).
     */
    public void setItemSize(long sizeOfCurrentUpload) {
        szBytes = sizeOfCurrentUpload;
        this.repaint(); // thread-safe
    }

    /**
     * Returns the filename of this item.
     *
     * Thread-safe (final variable).
     */
    public String getFilename() {
        return fn;
    }

    /**
     * Returns the percentage of this item which has been uploaded [0.0,1.0].
     *
     * Thread-safe (volatile variables).
     */
    public double getPercentUploaded() {
        return numBytesUploaded / (double)szBytes;
    }

    /**
     * Sets the text associated with the progress label.
     *
     * Thread-safe (asynchronously updates UI from Swing EDT).
     */
    public void setProgressText(final String s, final boolean showAlert) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setProgressTextDirectly(s, showAlert);
            }
        });
    }

    /** Updates the progress text.  MUST be called from the Swing EDT. */
    private void setProgressTextDirectly(String s, boolean showAlert) {
        assert SwingUtilities.isEventDispatchThread();
        lblProgress.setText(s);
        if(showAlert) {
            lblProgress.setIcon(ICON_ALERT);
            lblProgress.setForeground(DARK_RED);
            lblProgress.setToolTipText(s);
        }
        else {
            lblProgress.setIcon(null);
            lblProgress.setForeground(Color.BLACK);
        }
    }

    /**
     * Returns true if the item has been completely uploaded.
     *
     * Thread-safe (volatile variables).
     */
    public boolean isUploaded() {
        return numBytesUploaded == szBytes;
    }

    /**
     * Paints the background color to reflect how far completed this upload is.
     *
     * MUST be called from the Swing EDT.
     */
    public void paintComponent(Graphics g) {
        assert SwingUtilities.isEventDispatchThread();
        Graphics2D g2d = (Graphics2D)g;
        Color c = g2d.getColor();

        int perDoneX = (int)(getWidth() * getPercentUploaded());
        if(isUploaded())
            g2d.setColor(COLOR_UPLOAD_COMPLETE);
        else if(isFailed()) {
            g2d.setColor(COLOR_UPLOAD_FAILED);
            perDoneX = getWidth();
        }
        else
            g2d.setColor(COLOR_UPLOAD_PROGRESS);
        g2d.fillRect(0, 0, perDoneX, getHeight());

        g2d.setColor(UploaderPanel.BG_COLOR);
        g2d.fillRect(perDoneX, 0, getWidth(), getHeight());

        g2d.setColor(c);
    }

    /** asks the uploader to remove it from its upload queue */
    private void removeItemFromUploader() {
        uploader.removeItemToUpload(this);
    }

    /**
     * Returns the number of bytes uploaded.
     *
     * Thread-safe (volatile variable).
     */
    public long getNumBytesUploaded() {
        return numBytesUploaded;
    }

    /**
     * Sets the number of bytes of this item which have been uploaded.  When all
     * bytes have been uploaded, the remove button is replaced with a static
     * checkmark.  The progress text for this item is appropriately updated.
     *
     * Thread-safe (volatile variable and asynchronously updates UI from Swing EDT).
     */
    public void setNumBytesUploaded(final long n) {
        numBytesUploaded = n;
        final UploadItem me = this;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if(isUploaded()) {
                    btnRemove.setIcon(ICON_CHECKMARK);
                    btnRemove.setPressedIcon(ICON_CHECKMARK);
                    btnRemove.setRolloverIcon(ICON_CHECKMARK);
                    setProgressTextDirectly("uploaded!", false);
                }
                else {
                    if(n > 0)
                        setProgressTextDirectly(SZ_FMT.format(100*getPercentUploaded()) + "% uploaded", false);
                    else
                        setProgressTextDirectly("will retry", false);
                }
                me.repaint();
            }
        });
    }

    /**
     * Returns whether this item has failed to upload.
     *
     * Thread-safe (volatile variable).
     */
    public boolean isFailed() {
        return failed;
    }

    /**
     * Sets whether this upload item is in a failed state or not.
     *
     * Thread-safe (volatile variable and thread-safe Swing method).
     */
    public void setFailed(boolean b) {
        this.failed = b;
        this.repaint(); // thread-safe
    }
}
