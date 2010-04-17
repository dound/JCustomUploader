package uploader.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.swing.ImageIcon;
import javax.swing.JButton;

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

    /** Configures the button's UI to a standard scheme used in this project. */
    public static final void prepButtonUI(JButton b, ImageIcon pressed, ImageIcon hover) {
        b.setPressedIcon(pressed);
        b.setRolloverIcon(hover);
        b.setFocusPainted(false);
        b.setMargin(new Insets(1,2,1,2));
        b.setForeground(Color.BLUE);

        // disable content area fill and border painting to make buttons look more like links
        //b.setContentAreaFilled(false);
        //b.setBorderPainted(false);
    }

    /** Sets c's min, max, and preferred sizes to d. */
    public static final void setSize(Component c, Dimension d) {
        c.setMinimumSize(d);
        c.setPreferredSize(d);
        c.setMaximumSize(d);
    }

    /**
     * Returns the hex string representing the MD5 of the specified file f.  An
     * exception is thrown if the value cannot be computed.
     */
    public static final String md5(File f) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        InputStream is = new FileInputStream(f);
        byte[] buffer = new byte[4096];
        int read = 0;
        while((read = is.read(buffer)) > 0) {
            digest.update(buffer, 0, read);
        }
        is.close();
        byte[] md5sum = digest.digest();
        BigInteger bigInt = new BigInteger(1, md5sum);
        return bigInt.toString(16);
    }

    /**
     * Encode data into base64.
     *
     * Source: http://www.javaworld.com/javaworld/javatips/jw-javatip36.html
     */
    public final static byte[] base64bytes(byte[] byteData) {
        if (byteData == null)  return  null;
        int iSrcIdx;      // index into source (byteData)
        int iDestIdx;     // index into destination (byteDest)
        byte byteDest[] = new byte[((byteData.length+2)/3)*4];
        for (iSrcIdx=0, iDestIdx=0; iSrcIdx < byteData.length-2; iSrcIdx += 3) {
            byteDest[iDestIdx++] = (byte) ((byteData[iSrcIdx] >>> 2) & 077);
            byteDest[iDestIdx++] = (byte) ((byteData[iSrcIdx+1] >>> 4) & 017 |
                                           (byteData[iSrcIdx] << 4) & 077);
            byteDest[iDestIdx++] = (byte) ((byteData[iSrcIdx+2] >>> 6) & 003 |
                                           (byteData[iSrcIdx+1] << 2) & 077);
            byteDest[iDestIdx++] = (byte) (byteData[iSrcIdx+2] & 077);
        }

        if (iSrcIdx < byteData.length) {
            byteDest[iDestIdx++] = (byte) ((byteData[iSrcIdx] >>> 2) & 077);
            if (iSrcIdx < byteData.length-1) {
                byteDest[iDestIdx++] = (byte) ((byteData[iSrcIdx+1] >>> 4) & 017 |
                                               (byteData[iSrcIdx] << 4) & 077);
                byteDest[iDestIdx++] = (byte) ((byteData[iSrcIdx+1] << 2) & 077);
            }
            else
                byteDest[iDestIdx++] = (byte) ((byteData[iSrcIdx] << 4) & 077);
        }

        for (iSrcIdx = 0; iSrcIdx < iDestIdx; iSrcIdx++) {
            if      (byteDest[iSrcIdx] < 26)  byteDest[iSrcIdx] = (byte)(byteDest[iSrcIdx] + 'A');
            else if (byteDest[iSrcIdx] < 52)  byteDest[iSrcIdx] = (byte)(byteDest[iSrcIdx] + 'a'-26);
            else if (byteDest[iSrcIdx] < 62)  byteDest[iSrcIdx] = (byte)(byteDest[iSrcIdx] + '0'-52);
            else if (byteDest[iSrcIdx] < 63)  byteDest[iSrcIdx] = '+';
            else                              byteDest[iSrcIdx] = '/';
        }

        for ( ; iSrcIdx < byteDest.length; iSrcIdx++)
            byteDest[iSrcIdx] = '=';

        return byteDest;
    }

    /** URL encodes a string. */
    public static final String urlencode(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, "UTF-8");
    }
}
