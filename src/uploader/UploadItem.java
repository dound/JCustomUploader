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
    private static final Color COLOR_UPLOAD_PROGRESS = new Color(196, 255, 196);
    private static final DecimalFormat SZ_FMT = new DecimalFormat("0.00");
    private static final ImageIcon ICON_CLOSE = Util.createImageIcon("/resources/close.png");
    private static final ImageIcon ICON_CLOSE_PRESSED = Util.createImageIcon("/resources/close-press.png");
    private static final ImageIcon ICON_CLOSE_HOVER = Util.createImageIcon("/resources/close-hover.png");
    private static final ImageIcon ICON_CHECKMARK = Util.createImageIcon("/resources/checkmark.png");

    private final UploadManager uploader;
    private final String fn;
    private final long szBytes;
    private long numBytesUploaded = 0;

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
        add(lblProgress);
        add(Box.createRigidArea(new Dimension(5, 0)));

        btnRemove = new JButton(ICON_CLOSE);
        btnRemove.setPressedIcon(ICON_CLOSE_PRESSED);
        btnRemove.setRolloverIcon(ICON_CLOSE_HOVER);
        btnRemove.setFocusPainted(false);
        btnRemove.setContentAreaFilled(false);
        btnRemove.setBorderPainted(false);
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

    /** returns the percentage of this item which has been uploaded [0.0,1.0] */
    public double getPercentUploaded() {
        return numBytesUploaded / (double)szBytes;
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
            lblProgress.setText("uploaded!");
        }
        else {
            lblProgress.setText(SZ_FMT.format(100*getPercentUploaded()) + "% uploaded");
        }
    }
}
