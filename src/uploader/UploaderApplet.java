package uploader;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;

/**
 * Boilerplate applet shell.
 *
 * @author David Underhill
 */
public class UploaderApplet extends JApplet {
    //Called when this applet is loaded into the browser.
    public void init() {
        //Execute a job on the event-dispatching thread; creating this applet's GUI.
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
        //Create and set up the content pane.
        UploaderPanel newContentPane = new UploaderPanel(getWidth());
        newContentPane.setOpaque(true);
        setContentPane(newContentPane);
    }
}
