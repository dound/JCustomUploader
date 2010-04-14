package uploader;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
        btnAddImages.setPressedIcon(ICON_ADD_PRESSED);
        btnAddImages.setRolloverIcon(ICON_ADD_HOVER);
        btnAddImages.setMargin(new Insets(2,2,2,10));
        btnAddImages.setFocusPainted(false);
        btnAddImages.setContentAreaFilled(false);
        btnAddImages.setBorderPainted(false);
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
        btnAddFolder.setPressedIcon(ICON_ADD_PRESSED);
        btnAddFolder.setRolloverIcon(ICON_ADD_HOVER);
        btnAddFolder.setMargin(new Insets(2,2,2,10));
        btnAddFolder.setFocusPainted(false);
        btnAddFolder.setContentAreaFilled(false);
        btnAddFolder.setBorderPainted(false);
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
        pnlCmds.add(create_upload_panel());
        return pnlCmds;
    }

    private JPanel create_upload_panel() {
        final JPanel pnlUpload = new JPanel();
        pnlUpload.setLayout(new BoxLayout(pnlUpload, BoxLayout.X_AXIS));
        pnlUpload.setOpaque(false);
        pnlUpload.setAlignmentX(Component.RIGHT_ALIGNMENT);

        final JCheckBox chkUploading = new JCheckBox("Upload photos as I add them", true);
        final JButton btnUploadNow = new JButton("Start Uploading Now");
        pnlUpload.add(Box.createHorizontalGlue());
        pnlUpload.add(chkUploading);
        pnlUpload.add(btnUploadNow);

        chkUploading.setOpaque(false);
        chkUploading.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chkUploading.setVisible(false);
                btnUploadNow.setVisible(true);
                uploader.setUploadingEnabled(false);
            }
        });

        btnUploadNow.setVisible(false);
        btnUploadNow.setOpaque(false);
        btnUploadNow.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                btnUploadNow.setVisible(false);
                chkUploading.setVisible(true);
                chkUploading.setSelected(true);
                uploader.setUploadingEnabled(true);
            }
        });

        // reserve the space needed for the larger of the two components so that
        // when we switch between them the layout doesn't shift any
        int maxWidth = Math.max(chkUploading.getMaximumSize().width, btnUploadNow.getMaximumSize().width);
        int maxHeight = Math.max(chkUploading.getMaximumSize().height, btnUploadNow.getMaximumSize().height) + 1;
        Dimension sz = new Dimension(maxWidth, maxHeight);
        pnlUpload.setMinimumSize(sz);
        pnlUpload.setPreferredSize(sz);
        pnlUpload.setMaximumSize(sz);
        return pnlUpload;
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
