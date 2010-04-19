package uploader.demo;

import java.awt.Color;

import javax.swing.JFrame;

/**
 * Wrapper for SmugMugUploaderPanel which turns it into a standalone application.
 *
 * @author David Underhill
 */
public class BasicSmugMugUploader extends JFrame {
    public BasicSmugMugUploader() {
        setTitle("JCustomUploader Demo: Uploading to SmugMug");
        setBackground(Color.WHITE);
        setContentPane(new SmugMugUploaderPanel());
        setSize(700, 600);
    }

    public static void main(String[] args) {
        BasicSmugMugUploader demo = new BasicSmugMugUploader();
        demo.setDefaultCloseOperation(EXIT_ON_CLOSE);
        demo.setVisible(true);
    }
}
