package uploader;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/* ImageFilter.java is used by FileChooserDemo2.java. */
public class ImageFileFilter extends FileFilter {
    /** returns the extension of a file */
    private static String getExtension(File f) {
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1)
            return s.substring(i+1);
        else
            return null;
    }

    /** accepts all directories and all gif, jpg, and png files */
    public boolean accept(File f) {
        if(f.isDirectory())
            return true;

        String ext = getExtension(f);
        if (ext != null) {
            ext = ext.toLowerCase();
            return (ext.equals("gif")  ||
                    ext.equals("jpeg") ||
                    ext.equals("jpg")  ||
                    ext.equals("png"));
        } else {
            return false;
        }
    }

    /** describes this filter */
    public String getDescription() {
        return "Any Images (*.gif, *.jpg, *.png)";
    }
}
