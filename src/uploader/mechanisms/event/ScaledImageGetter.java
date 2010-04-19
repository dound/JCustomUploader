package uploader.mechanisms.event;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Ensures that uploaded photos are no larger than a specified size.
 *
 * @author David Underhill
 */
public class ScaledImageGetter extends UploadFileGetter {
    /** maximum edge length allowed */
    private final int maxEdgeLength;

    /**
     * Constructs a file getter which resizes images so that their longest edge
     * is no bigger than the specified length.
     */
    public ScaledImageGetter(int maxEdgeLength) {
        this.maxEdgeLength = maxEdgeLength;
    }

    /**
     * Returns the InputStream containing the photo (resized if needed) and the
     * number of bytes it contains.
     */
    public InputStreamAndSizePair getInputStream(File f) throws IOException {
        BufferedImage img = ImageIO.read(f);
        int w = img.getWidth();
        int h = img.getHeight();

        int longEdge = Math.max(w, h);
        if(longEdge <= maxEdgeLength)
            return super.getInputStream(f);

        double scale = maxEdgeLength / (double)longEdge;
        int w2 = (int)(w * scale);
        int h2 = (int)(h * scale);

        BufferedImage image = new BufferedImage(w2, h2, BufferedImage.TYPE_INT_RGB);
        image.getGraphics().drawImage(img, 0, 0, w2, h2, null);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return new InputStreamAndSizePair(new ByteArrayInputStream(bytes), bytes.length);
    }
}
