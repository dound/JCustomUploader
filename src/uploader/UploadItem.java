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

public class UploadItem extends JPanel {
    private static final Color COLOR_UPLOAD_FAILED = new Color(255, 196, 196);
    private static final Color COLOR_UPLOAD_PROGRESS = new Color(196, 255, 255);
    private static final Color COLOR_UPLOAD_COMPLETE = new Color(196, 255, 196);
    private static final Color DARK_RED = new Color(64, 0, 0);
    private static final DecimalFormat SZ_FMT = new DecimalFormat("0.00");
    private static final ImageIcon ICON_CLOSE = Util.createImageIcon("/resources/close.png");
    private static final ImageIcon ICON_CLOSE_PRESSED = Util.createImageIcon("/resources/close-press.png");
    private static final ImageIcon ICON_CLOSE_HOVER = Util.createImageIcon("/resources/close-hover.png");
    private static final ImageIcon ICON_CHECKMARK = Util.createImageIcon("/resources/checkmark.png");
    private static final ImageIcon ICON_ALERT = Util.createImageIcon("/resources/alert.png");

    private final UploadManager uploader;
    private final String fn;
    private long szBytes;
    private long numBytesUploaded = 0;
    private boolean failed = false;

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
        lblSz.setMinimumSize(lblSzDim);
        lblSz.setPreferredSize(lblSzDim);
        lblSz.setMaximumSize(lblSzDim);
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
        txtFn.setPreferredSize(lblFnDim);
        txtFn.setMaximumSize(lblFnDim);
        add(txtFn);

        add(Box.createHorizontalGlue());
        Dimension lblProgressDim = new Dimension(175, lblSzDim.height);
        lblProgress.setMinimumSize(lblProgressDim);
        lblProgress.setPreferredSize(lblProgressDim);
        lblProgress.setMaximumSize(lblProgressDim);
        add(lblProgress);
        add(Box.createRigidArea(new Dimension(5, 0)));

        btnRemove = new JButton(ICON_CLOSE);
        btnRemove.setContentAreaFilled(false);
        btnRemove.setBorderPainted(false);
        btnRemove.setOpaque(false);
        Util.prepButtonUI(btnRemove, ICON_CLOSE_PRESSED, ICON_CLOSE_HOVER);
        btnRemove.setToolTipText("Don't upload this photo");
        Dimension btnRmSz = new Dimension(17, 22);
        btnRemove.setMinimumSize(btnRmSz);
        btnRemove.setPreferredSize(btnRmSz);
        btnRemove.setMaximumSize(btnRmSz);
        btnRemove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(!isUploaded())
                    removeItemFromUploader();
            }
        });
        add(btnRemove);
        add(Box.createRigidArea(new Dimension(5, 0)));
    }

    /** returns the size of the file */
    public long length() {
        return szBytes;
    }

    /** sets the size of the file (use this if the size has changed) */
    public void setItemSize(long sizeOfCurrentUpload) {
        szBytes = sizeOfCurrentUpload;
        this.repaint();
    }

    /** returns the filename of this item */
    public String getFilename() {
        return fn;
    }

    /** returns the percentage of this item which has been uploaded [0.0,1.0] */
    public double getPercentUploaded() {
        return numBytesUploaded / (double)szBytes;
    }

    /** returns the text associated with the progress label */
    public String getProgressText() {
        return lblProgress.getText();
    }

    /** sets the text associated with the progress label */
    public void setProgressText(String s, boolean showAlert) {
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

    /** returns true if the item has been completely uploaded */
    public boolean isUploaded() {
        return numBytesUploaded == szBytes;
    }

    /** paints the background color to reflect how far completed this upload is */
    public void paintComponent(Graphics g) {
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

    /** returns the number of bytes uploaded */
    public long getNumBytesUploaded() {
        return numBytesUploaded;
    }

    /**
     * Sets the number of bytes of this item which have been uploaded.  When all
     * bytes have been uploaded, the remove button is replaced with a static
     * checkmark.  The progress text for this item is appropriately updated.
     */
    public void setNumBytesUploaded(long n) {
        numBytesUploaded = n;
        if(isUploaded()) {
            btnRemove.setIcon(ICON_CHECKMARK);
            btnRemove.setPressedIcon(ICON_CHECKMARK);
            btnRemove.setRolloverIcon(ICON_CHECKMARK);
            setProgressText("uploaded!", false);
        }
        else {
            setProgressText(SZ_FMT.format(100*getPercentUploaded()) + "% uploaded", false);
        }
        this.repaint();
    }

    /** convenience wrapper for setNumBytesUploaded() */
    public void incrNumBytesUploaded(long bytesUploaded) {
        setNumBytesUploaded(numBytesUploaded + bytesUploaded);
    }

    /** returns whether this item has failed to upload */
    public boolean isFailed() {
        return failed;
    }

    /** sets whether this upload item is in a failed state or not */
    public void setFailed(boolean b) {
        this.failed = b;
        this.repaint();
    }
}
