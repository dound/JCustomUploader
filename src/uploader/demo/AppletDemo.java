package uploader.demo;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;

import uploader.UploaderPanel;
import uploader.mechanisms.TestUploadMechanism;
import uploader.mechanisms.UploadMechanism;
import uploader.util.ImageFileFilter;

/**
 * Boilerplate applet shell.
 *
 * @author David Underhill
 */
public class AppletDemo extends JApplet {
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
        final int NUM_THREADS = 3;
        final UploadMechanism[] uploadMechs = new UploadMechanism[NUM_THREADS];
        for(int i=0; i<NUM_THREADS; i++)
            uploadMechs[i] = new TestUploadMechanism(50, 0.2, 0.0005, System.currentTimeMillis()+i*100);
        UploaderPanel newContentPane = new UploaderPanel(getWidth(), uploadMechs, "photo", new ImageFileFilter(), true);
        newContentPane.setOpaque(true);
        setContentPane(newContentPane);
    }
}
