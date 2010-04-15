package uploader;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;

import uploader.util.ImageFileFilter;

/**
 * Boilerplate applet shell.
 *
 * @author David Underhill
 */
public class UploaderApplet extends JApplet {
    //Called when this applet is loaded into the browser.
    public void init() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    createGUI();
                }
            });
        } catch (Exception e) {
            System.err.println("unable to create the GUI: " + e.getMessage());
        }
    }

    private void createGUI() {
        UploaderPanel newContentPane = new UploaderPanel(getWidth(), "photo", new ImageFileFilter(), true);
        newContentPane.setOpaque(true);
        setContentPane(newContentPane);
    }
}
