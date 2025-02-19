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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.TransferHandler;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import java.awt.datatransfer.*;
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

        // Add a ListDataListener to notify when the list changes (reordering, additions, removals)
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
            // Create an icon from the image file
            ImageIcon icon = new ImageIcon(value.getAbsolutePath());
            // Scale the image to a thumbnail (adjust the size as needed)
            Image image = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            setIcon(new ImageIcon(image));
            
            // Only show the image name if the toggle is true
            if (TimelinePanel.SHOW_IMAGE_NAMES) {
                setText(value.getName());
            } else {
                setText("");
            }

            // Optional: Change background when selected
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
        private int addIndex = -1; // Location where items were added
        private int addCount = 0;  // Number of items added

        @Override
        protected Transferable createTransferable(JComponent c) {
            JList<?> list = (JList<?>) c;
            indices = list.getSelectedIndices();
            List<?> values = list.getSelectedValuesList();
            // Instead of using DataHandler, we use our own ListTransferable.
            return new ListTransferable(values);
        }

        @Override
        public boolean canImport(TransferSupport info) {
            // Only support drops (not clipboard paste)
            if (!info.isDrop()) {
                return false;
            }
            return info.isDataFlavorSupported(ListTransferable.localFlavor);
        }

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        public boolean importData(TransferSupport info) {
            if (!canImport(info)) {
                return false;
            }
            JList<?> target = (JList<?>) info.getComponent();
            DefaultListModel listModel = (DefaultListModel) target.getModel();
            JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
            int index = dl.getIndex();
            try {
                @SuppressWarnings("unchecked")
                List<Object> values = (List<Object>) info.getTransferable()
                        .getTransferData(ListTransferable.localFlavor);
                addIndex = index;
                addCount = values.size();
                for (Object o : values) {
                    listModel.add(index++, o);
                    target.getSelectionModel().addSelectionInterval(index - 1, index - 1);
                }
                return true;
            } catch (UnsupportedFlavorException | IOException ex) {
                ex.printStackTrace();
            }
            return false;
        }

        @Override
        protected void exportDone(JComponent c, Transferable data, int action) {
            if ((action == MOVE) && (indices != null)) {
                JList source = (JList) c;
                DefaultListModel model = (DefaultListModel) source.getModel();
                // Adjust indices if items were added before removal
                if (addCount > 0) {
                    for (int i = indices.length - 1; i >= 0; i--) {
                        if (indices[i] >= addIndex) {
                            indices[i] += addCount;
                        }
                    }
                }
                // Remove the original items
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
