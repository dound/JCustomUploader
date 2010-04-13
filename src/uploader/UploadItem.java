package uploader;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.DecimalFormat;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class UploadItem extends JPanel {
    private static final Color COLOR_UPLOAD_PROGRESS = new Color(196, 255, 196);
    private static final DecimalFormat SZ_FMT = new DecimalFormat("0.00");

    private final String fn;
    private final int szBytes;
    private int numBytesUploaded = 0;

    private final JLabel lblProgress = new JLabel("not yet uploaded");

    public UploadItem(final String filename, final int sizeInBytes) {
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

        JButton btnRemove = new JButton("X");
        add(btnRemove);
    }

    /** returns the percentage of this item which has been uploaded [0.0,1.0] */
    public double getPercentUploaded() {
        return numBytesUploaded / (double)szBytes;
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
}
