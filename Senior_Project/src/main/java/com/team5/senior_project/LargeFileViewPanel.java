package com.team5.senior_project;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.border.LineBorder;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

public class LargeFileViewPanel extends JPanel {

    private JList<File> fileList;
    private DefaultListModel<File> listModel;
    private File currentFolder;

    public LargeFileViewPanel(File folder) {
        // Use a default folder if null is passed.
        if (folder == null) {
            folder = new File(System.getProperty("user.home"));
        }
        setLayout(new BorderLayout());
        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        fileList.setVisibleRowCount(-1);
        fileList.setCellRenderer(new LargeFileCellRenderer());
        // Enable drag support:
        fileList.setDragEnabled(true);
        fileList.setTransferHandler(new TransferHandler() {
            @Override
            protected Transferable createTransferable(javax.swing.JComponent c) {
                List<File> selectedFiles = fileList.getSelectedValuesList();
                return new Transferable() {
                    @Override
                    public DataFlavor[] getTransferDataFlavors() {
                        return new DataFlavor[] { DataFlavor.javaFileListFlavor };
                    }
                    @Override
                    public boolean isDataFlavorSupported(DataFlavor flavor) {
                        return DataFlavor.javaFileListFlavor.equals(flavor);
                    }
                    @Override
                    public Object getTransferData(DataFlavor flavor) 
                            throws UnsupportedFlavorException, IOException {
                        if (!isDataFlavorSupported(flavor)) {
                            throw new UnsupportedFlavorException(flavor);
                        }
                        return selectedFiles;
                    }
                };
            }
            @Override
            public int getSourceActions(javax.swing.JComponent c) {
                return TransferHandler.COPY;
            }
        });
        add(new JScrollPane(fileList), BorderLayout.CENTER);
        updateFolder(folder);
    }
    
    /**
     * Updates the panel to display files from the given folder.
     */
    public void updateFolder(File folder) {
        currentFolder = folder;
        loadFiles(folder);
    }
    
    private void loadFiles(File folder) {
        listModel.clear();
        if (folder != null && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File f : files) {
                    String name = f.getName().toLowerCase();
                    // Only add image files (adjust filters as needed)
                    if (name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                        name.endsWith(".png") || name.endsWith(".gif")) {
                        listModel.addElement(f);
                    }
                }
            }
        }
    }
    
    /**
     * Returns the list of selected files.
     */
    public List<File> getSelectedFiles() {
        return fileList.getSelectedValuesList();
    }
    
    /**
     * Custom ListCellRenderer that displays larger icons.
     */
    private class LargeFileCellRenderer extends JPanel implements ListCellRenderer<File> {
        private final javax.swing.JLabel label;
        
        public LargeFileCellRenderer() {
            setLayout(new BorderLayout());
            label = new javax.swing.JLabel();
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);
            add(label, BorderLayout.CENTER);
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        }
        
        @Override
        public Component getListCellRendererComponent(JList<? extends File> list, File value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            ImageIcon icon = new ImageIcon(value.getAbsolutePath());
            // Scale image to 100x100 pixels; adjust size as desired.
            Image image = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(image));
            label.setText(value.getName());
            if (isSelected) {
                setBorder(new LineBorder(Color.BLUE, 2));
            } else {
                setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }
            return this;
        }
    }
}
