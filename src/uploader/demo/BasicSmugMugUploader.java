package uploader.demo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import uploader.UploaderPanel;
import uploader.mechanisms.SmugMugUploadMechanism;
import uploader.mechanisms.event.SmugMugUploadListener;
import uploader.util.ImageFileFilter;

/**
 * This basic tool allows you to login to SmugMug, choose an album, and then use
 * the JCustomUploader interface to upload files to SmugMug.  This is primarily
 * intended as a demo.  Most of the code here is just for creating a decent UI
 * for the demo.  initUploaderPanel() is the tiny (<20 lines) piece of code
 * which actually creates the uploader.
 *
 * Note: SmugMug user authentication is done over HTTPS.
 *
 * @author David Underhill
 */
public class BasicSmugMugUploader extends JFrame {
    // what size to make the uploader itself
    private static final int UPLOADER_WIDTH = 700;
    private static final int UPLOADER_HEIGHT = 600;

    /** SmugMug session ID which authenticates the user as logged in */
    private String sessionID;

    final JPanel pnlLogin = new JPanel();
    final JPanel pnlAlbum = new JPanel();
    final JComboBox cboAlbums = new JComboBox();

    public BasicSmugMugUploader() {
        setTitle("JCustomUploader Demo: Uploading to SmugMug");
        setBackground(Color.WHITE);

        initLoginPanel();
        initAlbumPanel();
        add(pnlLogin);
        pack();
    }

    /** build the UI components for the login panel */
    private void initLoginPanel() {
        pnlLogin.setBackground(Color.WHITE);
        pnlLogin.setLayout(new BoxLayout(pnlLogin, BoxLayout.Y_AXIS));
        pnlLogin.setBorder(new EmptyBorder(10, 10, 10, 10));

        final JPanel pnlForm = new JPanel();
        pnlForm.setOpaque(false);
        pnlForm.setLayout(new BoxLayout(pnlForm, BoxLayout.X_AXIS));
        pnlLogin.add(pnlForm);

        final JPanel pnlLabels = new JPanel();
        pnlLabels.setOpaque(false);
        pnlLabels.setLayout(new BoxLayout(pnlLabels, BoxLayout.Y_AXIS));
        pnlForm.add(pnlLabels);
        final JLabel lblLogin = new JLabel("Login (E-mail):");
        final JLabel lblPW = new JLabel("Password:");
        pnlLabels.add(lblLogin);
        pnlLabels.add(Box.createRigidArea(new Dimension(0,5)));
        pnlLabels.add(lblPW);

        pnlForm.add(Box.createRigidArea(new Dimension(10,0)));

        final JPanel pnlInputs = new JPanel();
        pnlInputs.setOpaque(false);
        pnlInputs.setLayout(new BoxLayout(pnlInputs, BoxLayout.Y_AXIS));
        pnlForm.add(pnlInputs);
        final JTextField txtLogin = new JTextField();
        final JPasswordField txtPW = new JPasswordField();
        pnlInputs.add(txtLogin);
        pnlInputs.add(Box.createRigidArea(new Dimension(0,5)));
        pnlInputs.add(txtPW);

        pnlLogin.add(Box.createRigidArea(new Dimension(0,15)));

        final JButton btnLogin = new JButton("Login to your SmugMug account");
        btnLogin.setAlignmentX(CENTER_ALIGNMENT);
        pnlLogin.add(btnLogin);
        btnLogin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // try to log the user in
                btnLogin.setEnabled(false);
                String errMsg = doLogin(txtLogin.getText(), new String(txtPW.getPassword()));
                if(errMsg == null) {
                    for(Album a : getAlbums())
                        cboAlbums.addItem(a);

                    BasicSmugMugUploader.this.remove(pnlLogin);
                    BasicSmugMugUploader.this.add(pnlAlbum);
                    BasicSmugMugUploader.this.pack();
                }
                else {
                    JOptionPane.showMessageDialog(btnLogin, "Login failed: " + errMsg);
                    btnLogin.setEnabled(true);
                }
            }
        });
        txtLogin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                txtPW.requestFocus();
            }
        });
        txtPW.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                btnLogin.doClick();
            }
        });
    }

    /** build the UI components for the album picker panel */
    private void initAlbumPanel() {
        pnlAlbum.setBackground(Color.WHITE);
        pnlAlbum.setLayout(new BoxLayout(pnlAlbum, BoxLayout.Y_AXIS));
        pnlAlbum.setBorder(new EmptyBorder(10, 10, 10, 10));

        final JPanel pnlForm = new JPanel();
        pnlForm.setOpaque(false);
        pnlForm.setLayout(new BoxLayout(pnlForm, BoxLayout.X_AXIS));
        pnlAlbum.add(pnlForm);

        final JLabel lblAlbum = new JLabel("Album:");
        pnlForm.add(lblAlbum);

        pnlForm.add(Box.createRigidArea(new Dimension(10,0)));

        pnlForm.add(cboAlbums);

        pnlAlbum.add(Box.createRigidArea(new Dimension(0,15)));

        final JButton btnPickAlbum = new JButton("Pick photos to upload to this album");
        btnPickAlbum.setAlignmentX(CENTER_ALIGNMENT);
        pnlAlbum.add(btnPickAlbum);
        btnPickAlbum.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // bring up the uploader and tell it to upload to the selected album
                btnPickAlbum.setEnabled(false);
                Album a = (Album)cboAlbums.getSelectedItem();
                if(a != null) {
                    BasicSmugMugUploader.this.remove(pnlAlbum);
                    BasicSmugMugUploader.this.add(initUploaderPanel(a.id));
                }
            }
        });
    }

    /** fetch a URL */
    private String fetchURL(String url) {
        try {
            URL loginURL = new URL(url);
            BufferedReader in = new BufferedReader(new InputStreamReader(loginURL.openStream()));
            String resp = "";
            String line;
            while( (line=in.readLine()) != null )
                resp += line;
            in.close();
            return resp;
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(this, "Error: could not fetch " + url + ": " + e.getMessage());
            System.exit(-1);
            return null;
        }
    }

    /** try to login to SmugMug with the specified email/pw */
    private String doLogin(String email, String pw) {
        String resp = fetchURL("https://api.smugmug.com/services/api/rest/1.2.2/?method=smugmug.login.withPassword&APIKey=Pst4h28pdJJZA4PgPEVi9AStVwHoTBxv&EmailAddress=" + email + "&Password=" + pw);
        Matcher sess_matcher = RE_SESSION.matcher(resp);
        if(sess_matcher.find()) {
            sessionID = sess_matcher.group(1);
            return null;
        }
        else {
            Matcher err_matcher = RE_ERR.matcher(resp);
            if(err_matcher.find())
                return err_matcher.group(1);
            else
                return "login failed";
        }
    }

    // regular expressions for various SmugMug messages and fields
    private static final Pattern RE_SESSION  = Pattern.compile("<Session id=\"([^\"]+)\"/>");
    private static final Pattern RE_ERR   = Pattern.compile("msg=\"([^\"]+)\"");
    private static final Pattern RE_ALBUMS  = Pattern.compile("<Album id=\"([^\"]+)\" Key=\"[^\"]+\" Title=\"([^\"]+)\">");

    /** try to get a list of the albums */
    private LinkedList<Album> getAlbums() {
        String resp = fetchURL("https://api.smugmug.com/services/api/rest/1.2.2/?method=smugmug.albums.get&SessionID=" + sessionID);
        LinkedList<Album> albums = new LinkedList<Album>();
        Matcher album_matcher = RE_ALBUMS.matcher(resp);
        while(album_matcher.find())
            albums.add(new Album(album_matcher.group(1), album_matcher.group(2)));
        return albums;
    }

    /** stores an album's ID and title */
    private class Album {
        public String id;
        public String title;
        public Album(String id, String title) {
            this.id = id;
            this.title = title;
        }
        public String toString() {
            return title;
        }
    }

    /**
     * Create a JCustomUploader which will upload up to three photos
     * concurrently.  This uploader is configured with SmugMugUploadMechanisms
     * which will upload to the album selected by the user.  The session header
     * is used to authenticate this user with SmugMug.
     */
    private UploaderPanel initUploaderPanel(String albumID) {
        final int NUM_THREADS = 3;
        final SmugMugUploadMechanism[] uploadMechs = new SmugMugUploadMechanism[NUM_THREADS];
        for(int i=0; i<NUM_THREADS; i++) {
            uploadMechs[i] = new SmugMugUploadMechanism(albumID, sessionID);

            // print the image ID/key/URL to show how to get these as photos are uploaded to SmugMug
            uploadMechs[i].setSmugMugEventListener(new SmugMugUploadListener() {
                public void responseReceived(SmugMugUploadMechanism u, String imageID, String imageKey, String imageURL) {
                    System.out.println("info for newly uploaded image: filename="+u.getFile().getName() + " id="+imageID + " key="+imageKey + " url="+imageURL);
                }
            });
        }

        this.setBounds(getX(), getY(), UPLOADER_WIDTH, UPLOADER_HEIGHT);
        return new UploaderPanel(UPLOADER_WIDTH, uploadMechs, "photo", new ImageFileFilter(), true);
    }

    public static void main(String[] args) {
        BasicSmugMugUploader demo = new BasicSmugMugUploader();
        demo.setDefaultCloseOperation(EXIT_ON_CLOSE);
        demo.setVisible(true);
    }
}
