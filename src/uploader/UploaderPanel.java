package uploader;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class UploaderPanel extends JPanel {
    private static final int MARGIN_SIZE = 5;
    public static final Color BG_COLOR = Color.WHITE;
    private static final ImageIcon ICON_ADD = Util.createImageIcon("/resources/add.png");
    private static final ImageIcon ICON_ADD_PRESSED = Util.createImageIcon("/resources/add-press.png");
    private static final ImageIcon ICON_ADD_HOVER = Util.createImageIcon("/resources/add-hover.png");
    private static final ImageIcon ICON_UPLOAD_PAUSE = Util.createImageIcon("/resources/upload-pause.png");
    private static final ImageIcon ICON_UPLOAD_PAUSE_PRESSED = Util.createImageIcon("/resources/upload-pause-press.png");
    private static final ImageIcon ICON_UPLOAD_PAUSE_HOVER = Util.createImageIcon("/resources/upload-pause-hover.png");
    private static final ImageIcon ICON_UPLOAD_RESUME = Util.createImageIcon("/resources/upload-resume.png");
    private static final ImageIcon ICON_UPLOAD_RESUME_PRESSED = Util.createImageIcon("/resources/upload-resume-press.png");
    private static final ImageIcon ICON_UPLOAD_RESUME_HOVER = Util.createImageIcon("/resources/upload-resume-hover.png");

    private static final JFileChooser FC;
    private static final ImageFileFilter FILTER_IMAGES = new ImageFileFilter();
    private static final ImagePreview IMAGE_PREVIEW_ACCESSORY;
    static {
        System.setProperty("swing.disableFileChooserSpeedFix", "true");
        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
        FC = new JFileChooser();
        FC.setAcceptAllFileFilterUsed(false);
        FC.setMultiSelectionEnabled(true);
        FC.setFileFilter(FILTER_IMAGES);
        IMAGE_PREVIEW_ACCESSORY = new ImagePreview(FC);
    }

    private final JPanel pnlUploadList = new JPanel();
    private final JLabel txtPending = new JLabel("No photos added yet.");
    private final JLabel txtUploaded = new JLabel("No photos uploaded yet.");
    private final UploadManager uploader;
    private boolean uploadingEnabled = true;

    public UploaderPanel() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(new EmptyBorder(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE));
        this.setBackground(BG_COLOR);

        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
        uploader = new UploadManager(pnlUploadList, new SmugMugUploadMechanism());

        add(create_commands_panel());
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(create_upload_list());
        add(create_footer_panel());

        uploader.start();
    }

    private JPanel create_commands_panel() {
        JPanel pnlCmds = new JPanel();
        pnlCmds.setLayout(new BoxLayout(pnlCmds, BoxLayout.X_AXIS));
        pnlCmds.setOpaque(false);
        pnlCmds.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JButton btnAddImages = new JButton("Add images", ICON_ADD);
        Util.prepButtonUI(btnAddImages, ICON_ADD_PRESSED, ICON_ADD_HOVER);
        pnlCmds.add(btnAddImages);
        btnAddImages.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FC.setFileSelectionMode(JFileChooser.FILES_ONLY);
                FC.setDialogTitle("Choose image files to upload");
                FC.setAccessory(IMAGE_PREVIEW_ACCESSORY);
                int ret = FC.showDialog(btnAddImages, "Upload");
                if(ret == JFileChooser.APPROVE_OPTION) {
                    // TODO: remove these example entries
                    for(File f : FC.getSelectedFiles()) {
                        uploader.addFileToUpload(f);
                    }
                }
            }
        });

        pnlCmds.add(Box.createRigidArea(new Dimension(15, 0)));

        JButton btnAddFolder = new JButton("Add all images in a folder", ICON_ADD);
        Util.prepButtonUI(btnAddFolder, ICON_ADD_PRESSED, ICON_ADD_HOVER);
        pnlCmds.add(btnAddFolder);
        btnAddFolder.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FC.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                FC.setDialogTitle("Choose folders to upload images from");
                FC.setAccessory(null);
                int ret = FC.showDialog(btnAddImages, "Upload");
                if(ret == JFileChooser.APPROVE_OPTION) {
                    for(File dir : FC.getSelectedFiles()) {
                        for(File f : dir.listFiles()) {
                            if(!f.isDirectory() && FILTER_IMAGES.accept(f)) {
                                uploader.addFileToUpload(f);
                            }
                        }
                    }
                }
            }
        });

        pnlCmds.add(Box.createHorizontalGlue());

        final JButton btnToggleUploading = new JButton("Pause uploading", ICON_UPLOAD_PAUSE);
        Util.prepButtonUI(btnToggleUploading, ICON_UPLOAD_PAUSE_PRESSED, ICON_UPLOAD_PAUSE_HOVER);
        btnToggleUploading.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                uploadingEnabled = !uploadingEnabled;
                uploader.setUploadingEnabled(uploadingEnabled);
                if(uploadingEnabled) {
                    btnToggleUploading.setText("Pause uploading");
                    btnToggleUploading.setIcon(ICON_UPLOAD_PAUSE);
                    btnToggleUploading.setPressedIcon(ICON_UPLOAD_PAUSE_PRESSED);
                    btnToggleUploading.setRolloverIcon(ICON_UPLOAD_PAUSE_HOVER);
                }
                else {
                    btnToggleUploading.setText("Resume uploading");
                    btnToggleUploading.setIcon(ICON_UPLOAD_RESUME);
                    btnToggleUploading.setPressedIcon(ICON_UPLOAD_RESUME_PRESSED);
                    btnToggleUploading.setRolloverIcon(ICON_UPLOAD_RESUME_HOVER);
                }
            }
        });
        pnlCmds.add(btnToggleUploading);

        return pnlCmds;
    }

    private JScrollPane create_upload_list() {
        pnlUploadList.setLayout(new BoxLayout(pnlUploadList, BoxLayout.Y_AXIS));
        pnlUploadList.setBackground(BG_COLOR);
        pnlUploadList.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane spUploadList = new JScrollPane(pnlUploadList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        spUploadList.setAlignmentX(Component.LEFT_ALIGNMENT);
        return spUploadList;
   }

   private JPanel create_footer_panel() {
        JPanel pnlFooter = new JPanel();
        pnlFooter.setOpaque(false);
        pnlFooter.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnlFooter.setLayout(new BoxLayout(pnlFooter, BoxLayout.Y_AXIS));
        pnlFooter.add(Box.createRigidArea(new Dimension(0, 5)));
        pnlFooter.add(txtPending);
        pnlFooter.add(Box.createRigidArea(new Dimension(0, 5)));
        pnlFooter.add(txtUploaded);

        return pnlFooter;
    }
}
