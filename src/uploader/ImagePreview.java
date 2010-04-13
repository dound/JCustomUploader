package uploader;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;

/* component which shows a preview of an image selected by JFileChooser */
public class ImagePreview extends JComponent implements PropertyChangeListener {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 500;

    ImageIcon thumbnail = null;
    File file = null;

    public ImagePreview(JFileChooser fc) {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        fc.addPropertyChangeListener(this);
    }

    public void loadImage() {
        if (file == null) {
            thumbnail = null;
            return;
        }

        ImageIcon tmpIcon = new ImageIcon(file.getPath());
        int iw = tmpIcon.getIconWidth();
        int ih = tmpIcon.getIconHeight();
        float sw = iw / (float)WIDTH;
        float sh = ih / (float)HEIGHT;
        if (tmpIcon != null) {
            if(sh>1 || sw>1) {
                // need to scale: at least one dimension doesn't fit
                if(sw > sh)
                    thumbnail = new ImageIcon(tmpIcon.getImage().getScaledInstance(WIDTH, -1, Image.SCALE_DEFAULT));
                else
                    thumbnail = new ImageIcon(tmpIcon.getImage().getScaledInstance(-1, HEIGHT, Image.SCALE_DEFAULT));
            } else { //no need to miniaturize
                thumbnail = tmpIcon;
            }
        }
    }

    public void propertyChange(PropertyChangeEvent e) {
        boolean update = false;
        String prop = e.getPropertyName();

        //If the directory changed, don't show an image.
        if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)) {
            file = null;
            update = true;

        //If a file became selected, find out which one.
        } else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
            file = (File) e.getNewValue();
            update = true;
        }

        //Update the preview accordingly.
        if (update) {
            thumbnail = null;
            if (isShowing()) {
                loadImage();
                repaint();
            }
        }
    }

    protected void paintComponent(Graphics g) {
        if (thumbnail == null) {
            loadImage();
        }
        if (thumbnail != null) {
            int x = getWidth()/2 - thumbnail.getIconWidth()/2;
            int y = getHeight()/2 - thumbnail.getIconHeight()/2;

            if (y < 0) {
                y = 0;
            }

            if (x < 5) {
                x = 5;
            }
            thumbnail.paintIcon(this, g, x, y);
        }
    }
}
