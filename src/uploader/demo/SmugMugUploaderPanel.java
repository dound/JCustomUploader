package uploader.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import uploader.UploaderPanel;
import uploader.mechanisms.SmugMugUploadMechanism;
import uploader.mechanisms.event.ScaledImageGetter;
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
public class SmugMugUploaderPanel extends JPanel {
    // what size to make the uploader itself
    private static final int UPLOADER_WIDTH = 700;

    /** SmugMug session ID which authenticates the user as logged in */
    private String sessionID;

    /** maximum image size (<=0 if no limit) */
    private int maxPhotoSideLength = 0;

    /** stores a list of Albums in the user's account */
    private final JComboBox cboAlbums = new JComboBox();

    public SmugMugUploaderPanel() {
        setOpaque(true);
        setBackground(Color.WHITE);
        add(initLoginPanel());
    }

    /** build the UI components for the login panel */
    private JPanel initLoginPanel() {
        final JPanel pnlLogin = new JPanel();
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
                    ListIterator itr = getAlbums().listIterator();
                    while(itr.hasNext()) {
                        Album a = (Album)itr.next();
                        cboAlbums.addItem(a);
                    }

                    remove(pnlLogin);
                    add(initAlbumPanel());
                    validate();
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
        return pnlLogin;
    }

    /** build the UI components for the album picker panel */
    private JPanel initAlbumPanel() {
        final JPanel pnlAlbum = new JPanel();
        pnlAlbum.setBackground(Color.WHITE);
        pnlAlbum.setLayout(new BoxLayout(pnlAlbum, BoxLayout.Y_AXIS));
        pnlAlbum.setBorder(new EmptyBorder(10, 10, 10, 10));

        final JPanel pnlForm = new JPanel();
        pnlForm.setOpaque(false);
        pnlForm.setLayout(new BoxLayout(pnlForm, BoxLayout.X_AXIS));
        pnlForm.setAlignmentX(LEFT_ALIGNMENT);
        final JLabel lblAlbum = new JLabel("Album:");
        pnlForm.add(lblAlbum);
        pnlForm.add(Box.createRigidArea(new Dimension(10,0)));
        pnlForm.add(cboAlbums);
        pnlAlbum.add(pnlForm);

        pnlAlbum.add(Box.createRigidArea(new Dimension(0,15)));

        // UI for letting the user choose to resize images before uploading if desired
        final JPanel pnlSizer = new JPanel();
        pnlSizer.setOpaque(false);
        pnlSizer.setLayout(new BoxLayout(pnlSizer, BoxLayout.Y_AXIS));
        pnlSizer.setAlignmentX(LEFT_ALIGNMENT);
        pnlAlbum.add(pnlSizer);

        final JLabel lblSizer = new JLabel("Image Resizing:");
        lblSizer.setAlignmentX(LEFT_ALIGNMENT);
        pnlSizer.add(lblSizer);

        final JRadioButton optNoLimit = new JRadioButton("Don't resize my images");
        optNoLimit.setOpaque(false);
        optNoLimit.setAlignmentX(LEFT_ALIGNMENT);
        optNoLimit.setSelected(true);
        pnlSizer.add(optNoLimit);

        final JPanel pnlMaxLength = new JPanel();
        pnlMaxLength.setOpaque(false);
        pnlMaxLength.setLayout(new BoxLayout(pnlMaxLength, BoxLayout.X_AXIS));
        pnlMaxLength.setAlignmentX(LEFT_ALIGNMENT);
        pnlSizer.add(pnlMaxLength);
        final JRadioButton optResize = new JRadioButton("Maximum side length: ");
        optResize.setOpaque(false);
        optResize.setAlignmentX(LEFT_ALIGNMENT);
        pnlMaxLength.add(optResize);
        final JTextField txtMaxLength = new JTextField();
        txtMaxLength.setMaximumSize(new Dimension(50, txtMaxLength.getMaximumSize().height));
        txtMaxLength.setEnabled(false);
        pnlMaxLength.add(txtMaxLength);

        final ButtonGroup optionGroup = new ButtonGroup();
        optionGroup.add(optNoLimit);
        optionGroup.add(optResize);

        pnlAlbum.add(Box.createRigidArea(new Dimension(0,15)));

        // the button to show the uploader
        final JButton btnPickAlbum = new JButton("Pick photos to upload to this album");
        pnlAlbum.add(btnPickAlbum);
        btnPickAlbum.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // bring up the uploader and tell it to upload to the selected album
                btnPickAlbum.setEnabled(false);
                Album a = (Album)cboAlbums.getSelectedItem();
                if(a != null) {
                    // determine the max side length of photos, if any
                    try {
                        if(optNoLimit.isSelected())
                            maxPhotoSideLength = -1; // no limit
                        else {
                            int v = Integer.valueOf(txtMaxLength.getText());
                            if(v <= 0)
                                throw new Exception("bad side length (must be greater than 0)");
                            maxPhotoSideLength = v;
                        }
                    }
                    catch(Exception ex) {
                        JOptionPane.showMessageDialog(btnPickAlbum, ex.getMessage());
                        btnPickAlbum.setEnabled(true);
                        return;
                    }

                    // go to the uploader panel
                    remove(pnlAlbum);
                    setMinimumSize(new Dimension(700, 600));
                    setLayout(new BorderLayout());
                    add(initUploaderPanel(a.id));
                    validate();
                    repaint();
                }
            }
        });
        optNoLimit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                txtMaxLength.setText("");
                txtMaxLength.setEnabled(false);
            }
        });
        optResize.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                txtMaxLength.setEnabled(true);
                txtMaxLength.requestFocus();
            }
        });
        return pnlAlbum;
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
    private LinkedList getAlbums() {
        String resp = fetchURL("https://api.smugmug.com/services/api/rest/1.2.2/?method=smugmug.albums.get&SessionID=" + sessionID);
        LinkedList albums = new LinkedList();
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
        final ScaledImageGetter fileGetter = (maxPhotoSideLength>0) ? new ScaledImageGetter(100) : null;
        for(int i=0; i<NUM_THREADS; i++) {
            uploadMechs[i] = new SmugMugUploadMechanism(albumID, sessionID);
            if(fileGetter != null)
                uploadMechs[i].setUploadFileGetter(fileGetter);

            // print the image ID/key/URL to show how to get these as photos are uploaded to SmugMug
            uploadMechs[i].setSmugMugEventListener(new SmugMugUploadListener() {
                public void responseReceived(SmugMugUploadMechanism u, String imageID, String imageKey, String imageURL) {
                    System.out.println("info for newly uploaded image: filename="+u.getFile().getName() + " id="+imageID + " key="+imageKey + " url="+imageURL);
                }
            });
        }

        return new UploaderPanel(UPLOADER_WIDTH, uploadMechs, "photo", new ImageFileFilter(), true);
    }
}
