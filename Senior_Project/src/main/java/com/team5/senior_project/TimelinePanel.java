package com.team5.senior_project;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.BorderFactory;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.border.LineBorder;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import javax.swing.SwingWorker;
import java.awt.datatransfer.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.FileReader;
import java.io.FileWriter;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import static javax.swing.TransferHandler.COPY_OR_MOVE;
import static javax.swing.TransferHandler.MOVE;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import net.coobird.thumbnailator.Thumbnails;
import org.json.JSONArray;

/**
 * TimelinePanel displays a list of image thumbnails in a horizontal timeline
 * and allows drag-and-drop reordering.
 *
 * @author Team 5
 */
public class TimelinePanel extends javax.swing.JPanel {

    // Card names
    private final String CARD_LIST = "LIST";
    private final String CARD_PLACEHOLDER = "PLACEHOLDER";

    private DefaultListModel<File> listModel;
    private JList<File> imageList;
    private JLabel placeholderLabel;
    public static boolean SHOW_IMAGE_NAMES = false; // Toggle for showing image names in the timeline preview.
    private JPopupMenu contextMenu;
    private JMenuItem removeMenuItem;
    private File jsonFile;

    // Listener interface for timeline changes.
    public interface TimelineChangeListener {
        void onTimelineChanged();
    }
    private TimelineChangeListener timelineChangeListener;

    public void setTimelineChangeListener(TimelineChangeListener listener) {
        this.timelineChangeListener = listener;
    }
    
    // Constructor that loads images from a JSON file.
    public TimelinePanel(File jsonFile) {
        this();
        this.jsonFile = jsonFile; // Store the JSON file reference.
        loadImagesFromJson(jsonFile);
    }

    /**
     * Creates new form TimelinePanel
     */
    public TimelinePanel() {
        // Use CardLayout to switch between list view and placeholder.
        setLayout(new CardLayout());
        // Create the list model and image list.
        listModel = new DefaultListModel<>();
        imageList = new JList<>(listModel);
        imageList.setCellRenderer(new ImageListCellRenderer());
        imageList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        imageList.setVisibleRowCount(1);
        imageList.setDragEnabled(true);
        imageList.setDropMode(DropMode.INSERT);
        imageList.setTransferHandler(new ListItemTransferHandler());
        // Create a scroll pane for the image list.
        JScrollPane listScrollPane = new JScrollPane(imageList);
        add(listScrollPane, CARD_LIST);
        // Create a placeholder label for an empty timeline.
        placeholderLabel = new JLabel("Drop images here to start timeline", SwingConstants.CENTER);
        placeholderLabel.setOpaque(true);
        placeholderLabel.setBackground(Color.LIGHT_GRAY);
        add(placeholderLabel, CARD_PLACEHOLDER);
        // Initially show placeholder if no images are present.
        updateCard();
        // Listen for changes in the model to update the card.
        listModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                if (timelineChangeListener != null) {
                    timelineChangeListener.onTimelineChanged();
                }
                updateCard();
            }
            @Override
            public void intervalRemoved(ListDataEvent e) {
                if (timelineChangeListener != null) {
                    timelineChangeListener.onTimelineChanged();
                }
                updateCard();
            }
            @Override
            public void contentsChanged(ListDataEvent e) {
                if (timelineChangeListener != null) {
                    timelineChangeListener.onTimelineChanged();
                }
                updateCard();
            }
        });
        initializeContextMenu();
    }
    
    private void initializeContextMenu() {
        contextMenu = new JPopupMenu();
        removeMenuItem = new JMenuItem("Remove");

        removeMenuItem.addActionListener(e -> removeSelectedImage());

        contextMenu.add(removeMenuItem);

        imageList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }
        });
    }
    
    private void showContextMenu(MouseEvent e) {
        int index = imageList.locationToIndex(e.getPoint());
        if (index != -1) {
            imageList.setSelectedIndex(index); // Select the item under right-click
            contextMenu.show(imageList, e.getX(), e.getY());
        }
    }
    
    private void removeSelectedImage() {
        int selectedIndex = imageList.getSelectedIndex();
        if (selectedIndex != -1) {
            listModel.remove(selectedIndex);
            updateJsonFile();
        }
    }

    private void updateJsonFile() {
        if (jsonFile == null) return;

        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < listModel.size(); i++) {
            jsonArray.put(listModel.get(i).getAbsolutePath());
        }

        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write(jsonArray.toString(4)); // Pretty-print JSON with indentation.
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    // Loads image file paths from a JSON file
    public void loadImagesFromJson(File jsonFile) {
        try (FileReader reader = new FileReader(jsonFile)) {
            StringBuilder content = new StringBuilder();
            int i;
            while ((i = reader.read()) != -1) {
                content.append((char) i);
            }
            JSONArray jsonArray = new JSONArray(content.toString());
            List<File> files = new ArrayList<>();
            for (int j = 0; j < jsonArray.length(); j++) {
                files.add(new File(jsonArray.getString(j)));
            }
            setImages(files);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Updates the visible card based on whether there are images.
    private void updateCard() {
        CardLayout cl = (CardLayout) getLayout();
        if (listModel.isEmpty()) {
            cl.show(this, CARD_PLACEHOLDER);
        } else {
            cl.show(this, CARD_LIST);
        }
    }

    // Sets the timeline images.
    public void setImages(List<File> images) {
        listModel.clear();
        if (images != null) {
            for (File file : images) {
                listModel.addElement(file);
            }
        }
        updateCard();
    }

    // Retrieves the current ordering.
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
        // Shared cache for thumbnails
        private static final Map<File, ImageIcon> thumbnailCache = new HashMap<>();
        // Placeholder icon while loading
        private static final ImageIcon placeholderIcon = new ImageIcon(
                new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB));

        public ImageListCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends File> list, File value,
                                                    int index, boolean isSelected, boolean cellHasFocus) {
            // Try to get a cached thumbnail
            ImageIcon icon = thumbnailCache.get(value);
            if (icon == null) {
                // No cached thumbnail yet: show placeholder and start asynchronous loading
                setIcon(placeholderIcon);
                new SwingWorker<ImageIcon, Void>() {
                    @Override
                    protected ImageIcon doInBackground() throws Exception {
                        BufferedImage thumb = Thumbnails.of(value)
                                .size(100, 100)
                                .imageType(BufferedImage.TYPE_INT_RGB)
                                .keepAspectRatio(true)
                                .asBufferedImage();
                        return new ImageIcon(thumb);
                    }
                    @Override
                    protected void done() {
                        try {
                            ImageIcon loadedIcon = get();
                            thumbnailCache.put(value, loadedIcon);
                            list.repaint(); // Refresh the list so the new icon shows
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }.execute();
            } else {
                // Thumbnail is available; use it
                setIcon(icon);
            }
            // Optionally show the image name
            setText(SHOW_IMAGE_NAMES ? value.getName() : "");
            // Handle selection styling
            setBorder(isSelected ? new LineBorder(Color.BLUE, 2) : null);
            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            return this;
        }
    }

    // --- TransferHandler for Drag-and-Drop Reordering ---
    private static class ListItemTransferHandler extends TransferHandler {
        private int[] indices = null; // Stores the indices of the dragged items.
        private int addIndex = -1;    // Stores the index where items are dropped.
        private int addCount = 0;     // Stores the number of items added during the drop.

        @Override
        protected Transferable createTransferable(JComponent c) {
            // Creates a Transferable object containing the selected items.
            JList<?> list = (JList<?>) c;
            indices = list.getSelectedIndices();
            List<?> values = list.getSelectedValuesList();
            return new ListTransferable(values); // Returns a custom Transferable.
        }

        @Override
        public int getSourceActions(JComponent c) {
            // Defines the allowed source actions for the drag operation.
            return COPY_OR_MOVE; // Allows both copying and moving items.
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            // Checks if the transfer data can be imported.
            return info.isDataFlavorSupported(ListTransferable.localFlavor)
                    || info.isDataFlavorSupported(DataFlavor.javaFileListFlavor); // Checks for custom list flavor or file list flavor.
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            // Imports the transfer data into the target JList.
            if (!canImport(info)) {
                return false;
            }
            JList<?> target = (JList<?>) info.getComponent();
            DefaultListModel model = (DefaultListModel) target.getModel();
            JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
            int index = dl.getIndex(); // Gets the drop index.
            if (index < 0) {
                index = model.getSize(); // If no drop index, append to the end.
            }
            addIndex = index;
            try {
                List<?> values;
                if (info.isDataFlavorSupported(ListTransferable.localFlavor)) {
                    // If the data flavor is the custom list flavor, get the list of objects.
                    @SuppressWarnings("unchecked")
                    List<Object> localValues = (List<Object>) info.getTransferable().getTransferData(ListTransferable.localFlavor);
                    values = localValues;
                } else if (info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    // If the data flavor is the file list flavor, get the list of files.
                    @SuppressWarnings("unchecked")
                    List<Object> fileList = (List<Object>) info.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    values = fileList;
                } else {
                    return false; // If no supported flavor, return false.
                }
                addCount = values.size(); // Set the add count.
                for (Object o : values) {
                    model.add(index++, o); // Add each object to the model.
                }
                return true; // Return true if import is successful.
            } catch (UnsupportedFlavorException | IOException ex) {
                ex.printStackTrace();
            }
            return false; // Return false if import fails.
        }

        @Override
        protected void exportDone(JComponent c, Transferable data, int action) {
            // Cleans up after the transfer is complete.
            if (action == MOVE && indices != null) {
                // If the action was a move and indices are valid.
                JList source = (JList) c;
                DefaultListModel model = (DefaultListModel) source.getModel();
                if (addCount > 0) {
                    // Adjust indices if items were added before the original indices.
                    for (int i = indices.length - 1; i >= 0; i--) {
                        if (indices[i] >= addIndex) {
                            indices[i] += addCount;
                        }
                    }
                }
                for (int i = indices.length - 1; i >= 0; i--) {
                    model.remove(indices[i]); // Remove the original items.
                }
            }
            indices = null; // Reset indices.
            addCount = 0;   // Reset add count.
            addIndex = -1;  // Reset add index.
        }
    }

    // --- Custom Transferable for a List of Objects ---
    private static class ListTransferable implements Transferable {
        private final List<?> data; // The list of objects to transfer.
        public static final DataFlavor localFlavor; // Custom data flavor for list transfer.

        static {
            DataFlavor flavor = null;
            try {
                flavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=java.util.List"); // Define the custom flavor.
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
            return new DataFlavor[]{localFlavor}; // Return the custom data flavor.
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return localFlavor.equals(flavor); // Check if the flavor is supported.
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (isDataFlavorSupported(flavor)) {
                return data; // Return the data if the flavor is supported.
            }
            throw new UnsupportedFlavorException(flavor); // Throw exception if flavor is not supported.
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
