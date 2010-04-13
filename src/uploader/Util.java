package uploader;

import javax.swing.ImageIcon;

public final class Util {
    private Util() {}
    private static final Util singleton = new Util();

    /** Returns an ImageIcon, or null if the path was invalid. */
    public static final ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = singleton.getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
}
