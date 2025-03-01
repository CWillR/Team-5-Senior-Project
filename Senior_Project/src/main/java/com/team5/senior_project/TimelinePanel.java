package com.team5.senior_project;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.TransferHandler;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import java.awt.datatransfer.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
// Removed: import javax.activation.DataHandler;

/**
 * TimelinePanel displays a list of image thumbnails in a horizontal timeline
 * and allows drag-and-drop reordering.
 *
 * @author jackh
 */
public class TimelinePanel extends javax.swing.JPanel {

    private DefaultListModel<File> listModel;
    private JList<File> imageList;
    
    // Toggle for showing image names in the timeline preview.
    public static boolean SHOW_IMAGE_NAMES = false;

    // Define the listener interface
    public interface TimelineChangeListener {
        void onTimelineChanged();
    }

    // Listener reference
    private TimelineChangeListener timelineChangeListener;

    // Setter for the listener
    public void setTimelineChangeListener(TimelineChangeListener listener) {
        this.timelineChangeListener = listener;
    }

    /**
     * Creates new form TimelinePanel
     */
    public TimelinePanel() {
        initComponents();
        listModel = new DefaultListModel<>();
        imageList = new JList<>(listModel);

        // Use a custom renderer and configure list appearance
        imageList.setCellRenderer(new ImageListCellRenderer());
        imageList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        imageList.setVisibleRowCount(1);
        imageList.setDragEnabled(true);
        imageList.setDropMode(DropMode.INSERT);
        imageList.setTransferHandler(new ListItemTransferHandler());

        setLayout(new BorderLayout());
        add(new JScrollPane(imageList), BorderLayout.CENTER);
        
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem removeItem = new JMenuItem("Remove");
        
        removeItem.addActionListener(e -> {
            int selectedIndex = imageList.getSelectedIndex();
            if (selectedIndex != -1) {
                listModel.remove(selectedIndex);
                if (timelineChangeListener != null) {
                    timelineChangeListener.onTimelineChanged();
                }
            }
        });

        popupMenu.add(removeItem);

        imageList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int index = imageList.locationToIndex(e.getPoint());
                    if (index != -1) {
                        imageList.setSelectedIndex(index); // Ensure right-click selects item
                        popupMenu.show(imageList, e.getX(), e.getY());
                    }
                }
            }
        });
        
        // ListDataListener to notify when the list changes (reordering, additions, removals)
        listModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                if (timelineChangeListener != null) {
                    timelineChangeListener.onTimelineChanged();
                }
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                if (timelineChangeListener != null) {
                    timelineChangeListener.onTimelineChanged();
                }
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                if (timelineChangeListener != null) {
                    timelineChangeListener.onTimelineChanged();
                }
            }
        });
    }

    // Method to update the timeline with a new list of images
    public void setImages(List<File> images) {
        listModel.clear();
        for (File file : images) {
            listModel.addElement(file);
        }
    }

    // Retrieve the current ordering from the timeline
    public List<File> getImages() {
        List<File> images = new ArrayList<>();
        for (int i = 0; i < listModel.getSize(); i++) {
            images.add(listModel.get(i));
        }
        return images;
    }

    public JList<File> getImageList() {
        return imageList;
    }

    // --- Custom Cell Renderer for Thumbnails ---
    private static class ImageListCellRenderer extends JLabel implements ListCellRenderer<File> {
        public ImageListCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends File> list, File value,
            int index, boolean isSelected, boolean cellHasFocus) {
            // Create an icon from the image file and scale it
            ImageIcon icon = new ImageIcon(value.getAbsolutePath());
            Image image = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            setIcon(new ImageIcon(image));

            // Toggle image name display
            if (TimelinePanel.SHOW_IMAGE_NAMES) {
                setText(value.getName());
            } else {
                setText("");
            }

            // Add a border if selected
            if (isSelected) {
                setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.BLUE, 2));
            } else {
                setBorder(null);
            }

            // Set background and foreground colors
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            return this;
        }
    }

    // --- TransferHandler for Reordering Items in the JList ---
    private static class ListItemTransferHandler extends TransferHandler {
        private int[] indices = null;
        private int addIndex = -1; // Where items were inserted.
        private int addCount = 0;  // Number of items inserted.

        @Override
        protected Transferable createTransferable(JComponent c) {
            JList<?> list = (JList<?>) c;
            indices = list.getSelectedIndices();
            List<?> values = list.getSelectedValuesList();
            return new ListTransferable(values);
        }

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        public boolean canImport(TransferSupport info) {
            if (!info.isDrop()) {
                return false;
            }
            return info.isDataFlavorSupported(ListTransferable.localFlavor);
        }

        @Override
        public boolean importData(TransferSupport info) {
            if (!canImport(info)) {
                return false;
            }
            JList<?> target = (JList<?>) info.getComponent();
            DefaultListModel model = (DefaultListModel) target.getModel();
            JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
            int index = dl.getIndex();
            if (index < 0) {
                index = model.getSize();
            }
            addIndex = index;
            try {
                @SuppressWarnings("unchecked")
                List<Object> values = (List<Object>) info.getTransferable().getTransferData(ListTransferable.localFlavor);
                addCount = values.size();
                // Insert items at the drop index.
                for (Object o : values) {
                    model.add(index++, o);
                }
                return true;
            } catch (UnsupportedFlavorException | IOException ex) {
                ex.printStackTrace();
            }
            return false;
        }

        @Override
        protected void exportDone(JComponent c, Transferable data, int action) {
            if (action == MOVE && indices != null) {
                JList source = (JList) c;
                DefaultListModel model = (DefaultListModel) source.getModel();
                // Adjust indices if items were inserted before removal.
                if (addCount > 0) {
                    for (int i = indices.length - 1; i >= 0; i--) {
                        if (indices[i] >= addIndex) {
                            indices[i] += addCount;
                        }
                    }
                }
                // Remove the original items.
                for (int i = indices.length - 1; i >= 0; i--) {
                    model.remove(indices[i]);
                }
            }
            indices = null;
            addCount = 0;
            addIndex = -1;
        }
    }

    // --- Custom Transferable for a List of Objects ---
    private static class ListTransferable implements Transferable {
        private final List<?> data;
        public static final DataFlavor localFlavor;

        static {
            DataFlavor flavor = null;
            try {
                // Create a DataFlavor that represents a List
                flavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=java.util.List");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            localFlavor = flavor;
        }

        public ListTransferable(List<?> data) {
            this.data = data;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{localFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return localFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (isDataFlavorSupported(flavor)) {
                return data;
            }
            throw new UnsupportedFlavorException(flavor);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
