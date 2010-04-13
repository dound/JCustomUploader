package uploader;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

public class UploaderPanel extends JPanel {
    private static final int MARGIN_SIZE = 5;
    public static final Color BG_COLOR = Color.WHITE;
    private static final ImageIcon ICON_ADD = Util.createImageIcon("/resources/add.png");
    private static final ImageIcon ICON_ADD_PRESSED = Util.createImageIcon("/resources/add-press.png");

    private final JLabel txtPending = new JLabel("No photos added yet.");
    private final JLabel txtUploaded = new JLabel("No photos uploaded yet.");

    public UploaderPanel() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(new EmptyBorder(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE));
        this.setBackground(BG_COLOR);

        add(create_commands_panel());
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(create_upload_list());
        add(create_footer_panel());
    }

    private JPanel create_commands_panel() {
        JPanel pnlCmds = new JPanel();
        pnlCmds.setLayout(new BoxLayout(pnlCmds, BoxLayout.X_AXIS));
        pnlCmds.setOpaque(false);
        pnlCmds.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnAddImages = new JButton("Add images", ICON_ADD);
        btnAddImages.setPressedIcon(ICON_ADD_PRESSED);
        btnAddImages.setMargin(new Insets(2,2,2,10));
        btnAddImages.setFocusPainted(false);
        pnlCmds.add(btnAddImages);
        btnAddImages.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // TODO: show file selection dialog box
            }
        });

        pnlCmds.add(Box.createRigidArea(new Dimension(15, 0)));

        JButton btnAddFolder = new JButton("Add all images in a folder", ICON_ADD);
        btnAddFolder.setPressedIcon(ICON_ADD_PRESSED);
        btnAddFolder.setMargin(new Insets(2,2,2,10));
        btnAddFolder.setFocusPainted(false);
        pnlCmds.add(btnAddFolder);
        btnAddFolder.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // TODO: show folder selection dialog box
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
            }
        });

        btnUploadNow.setVisible(false);
        btnUploadNow.setOpaque(false);
        btnUploadNow.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                btnUploadNow.setVisible(false);
                chkUploading.setVisible(true);
                chkUploading.setSelected(true);
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
        JPanel pnlUploadList = new JPanel();
        pnlUploadList.setLayout(new BoxLayout(pnlUploadList, BoxLayout.Y_AXIS));
        pnlUploadList.setOpaque(false);
        pnlUploadList.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane spUploadList = new JScrollPane(pnlUploadList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        spUploadList.setOpaque(false);
        spUploadList.setAlignmentX(Component.LEFT_ALIGNMENT);

        // TODO: remove these example entries
        for(int i=0; i<25; i++)
            pnlUploadList.add(new UploadItem("/home/dgu/magic1", i*100+100));
        pnlUploadList.add(new UploadItem("/home/dgu/more/magic2/fgjsdklgjsdfgj/sdfgsdfgs/sdfgsdfgfsdgsdfgljk/sdfgsdfgsdfg/helloworld", 512*1024));

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
