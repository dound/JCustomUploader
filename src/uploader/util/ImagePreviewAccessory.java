package uploader.util;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;

/**
 * Component which shows a preview of an image selected by JFileChooser.
 *
 * @author David Underhill
 */
public class ImagePreviewAccessory extends JComponent implements PropertyChangeListener {
    /** maximum width of the preview */
    private static final int WIDTH = 400;

    /** maximum height of the preview */
    private static final int HEIGHT = 500;

    /** the file chooser which this object is creating previews for */
    private final JFileChooser fc;

    /** the preview rendered to a fit inside the preview window */
    private ImageIcon preview = null;

    /** construct an ImagePreview which listens for changes to fc */
    public ImagePreviewAccessory(JFileChooser fc) {
        this.fc = fc;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        fc.addPropertyChangeListener(this);
    }

    /** refresh the preview of the selected file when the selection changes */
    public void propertyChange(PropertyChangeEvent e) {
        File selectedFile = fc.getSelectedFile();
        if(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(e.getPropertyName()) && selectedFile!=null) {
            preview = new ImageIcon(selectedFile.getPath());
            if(preview == null)
                return; // couldn't read in the image

            // scale it if needed
            int iw = preview.getIconWidth();
            int ih = preview.getIconHeight();
            float sw = iw / (float)WIDTH;
            float sh = ih / (float)HEIGHT;
            if(sh>1 || sw>1) {
                // need to scale: at least one dimension doesn't fit
                if(sw > sh)
                    preview = new ImageIcon(preview.getImage().getScaledInstance(WIDTH, -1, Image.SCALE_DEFAULT));
                else
                    preview = new ImageIcon(preview.getImage().getScaledInstance(-1, HEIGHT, Image.SCALE_DEFAULT));
            }
            repaint();
        }
        else if(selectedFile == null) {
            // nothing selected => no preview image
            preview = null;
            repaint();
        }
    }

    /** draws a preview of the selected image, if any */
    protected void paintComponent(Graphics g) {
        if(preview != null) {
            int x = Math.max(0, WIDTH/2 - preview.getIconWidth()/2);
            int y = Math.max(0, HEIGHT/2 - preview.getIconHeight()/2);
            preview.paintIcon(this, g, x, y);
        }
    }
}
