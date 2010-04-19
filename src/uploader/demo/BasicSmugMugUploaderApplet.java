package uploader.demo;

import javax.swing.JApplet;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Wrapper for SmugMugUploaderPanel which turns it into an applet.
 *
 * @author David Underhill
 */
public class BasicSmugMugUploaderApplet extends JApplet {
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
        JPanel newContentPane = new SmugMugUploaderPanel();
        setContentPane(newContentPane);
    }
}
