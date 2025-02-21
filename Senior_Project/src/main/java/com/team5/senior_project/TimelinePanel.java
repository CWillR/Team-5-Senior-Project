package com.team5.senior_project;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.TransferHandler;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import java.awt.datatransfer.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * TimelinePanel displays a list of image thumbnails and transition items in a horizontal timeline
 * and allows drag-and-drop reordering.
 *
 * Transition items represent transitions between images, holding a transition type and duration.
 * 
 * In the timeline, a transition box is automatically inserted between images.
 * Additionally, if a transition is dragged onto another transition, they swap.
 *
 * The ordering is always normalized to: image, transition, image, transition, ... ending with an image.
 * For backward compatibility, getImageList() returns a JList<File> containing only image files.
 *
 * @author jackh
 */
public class TimelinePanel extends javax.swing.JPanel {

    // The list model now holds objects (File or TransitionItem)
    private DefaultListModel<Object> listModel;
    private JList<Object> timelineList;
    
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
        timelineList = new JList<>(listModel);

        // Use a custom renderer that handles both images and transitions.
        timelineList.setCellRenderer(new TimelineListCellRenderer());
        timelineList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        timelineList.setVisibleRowCount(1);
        timelineList.setDragEnabled(true);
        timelineList.setDropMode(DropMode.INSERT);
        timelineList.setTransferHandler(new ListItemTransferHandler());
        
        // Add mouse listener for editing transition durations on double-click.
        timelineList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = timelineList.locationToIndex(e.getPoint());
                if (index != -1) {
                    Object item = listModel.get(index);
                    if (item instanceof TransitionItem && e.getClickCount() == 2) {
                        TransitionItem transition = (TransitionItem) item;
                        String input = JOptionPane.showInputDialog(
                                TimelinePanel.this, 
                                "Enter transition duration in seconds:",
                                transition.getDuration() / 1000);
                        if (input != null) {
                            try {
                                int seconds = Integer.parseInt(input.trim());
                                if (seconds < 0) {
                                    throw new NumberFormatException();
                                }
                                transition.setDuration(seconds * 1000);
                                // Update the model to reflect the change.
                                listModel.set(index, transition);
                            } catch (NumberFormatException ex) {
                                JOptionPane.showMessageDialog(
                                    TimelinePanel.this, 
                                    "Please enter a valid non-negative integer.",
                                    "Invalid Input",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                }
            }
        });

        setLayout(new BorderLayout());
        add(new JScrollPane(timelineList), BorderLayout.CENTER);

        // ListDataListener to notify when the list changes.
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

    /**
     * Updates the timeline with a new list of image files.
     * This method automatically inserts a default transition box
     * (default: type "Fade", duration 2 seconds) between each adjacent pair of images.
     *
     * @param images a List of image files.
     */
    public void setImages(List<File> images) {
        listModel.clear();
        for (int i = 0; i < images.size(); i++) {
            listModel.addElement(images.get(i));
            // Insert a default transition between images (except after the last image)
            if (i < images.size() - 1) {
                listModel.addElement(new TransitionItem("Fade", 2000));
            }
        }
    }

    /**
     * Inserts a transition item at the specified index.
     *
     * @param index the index where the transition should be inserted.
     * @param transition the TransitionItem to insert.
     */
    public void insertTransition(int index, TransitionItem transition) {
        listModel.add(index, transition);
        normalizeTimeline();
    }

    /**
     * Retrieves only the image Files from the timeline (ignoring transitions).
     *
     * @return a List of Files.
     */
    public List<File> getImages() {
        List<File> images = new ArrayList<>();
        for (int i = 0; i < listModel.getSize(); i++) {
            Object obj = listModel.get(i);
            if (obj instanceof File) {
                images.add((File) obj);
            }
        }
        return images;
    }

    /**
     * Retrieves the complete list of timeline items (images and transitions).
     *
     * @return a List of Objects (File and TransitionItem).
     */
    public List<Object> getTimelineItems() {
        List<Object> items = new ArrayList<>();
        for (int i = 0; i < listModel.getSize(); i++) {
            items.add(listModel.get(i));
        }
        return items;
    }

    /**
     * Returns the timeline JList containing images and transitions.
     *
     * @return the JList of timeline items.
     */
    public JList<Object> getTimelineList() {
        return timelineList;
    }
    
    /**
     * For backward compatibility, getImageList() returns a JList<File> containing only image files.
     *
     * @return a JList of Files.
     */
    public JList<File> getImageList() {
        DefaultListModel<File> fileModel = new DefaultListModel<>();
        for (int i = 0; i < listModel.getSize(); i++) {
            Object obj = listModel.get(i);
            if (obj instanceof File) {
                fileModel.addElement((File) obj);
            }
        }
        return new JList<>(fileModel);
    }

    /**
     * Normalizes the timeline ordering so that:
     * - The first item is an image.
     * - Items alternate: image, transition, image, transition, ... ending with an image.
     * - For each adjacent pair of images, the corresponding transition is taken from the
     *   collected transitions (preserving its custom duration) if available; otherwise,
     *   a default transition (Fade, 2 seconds) is inserted.
     */
    private void normalizeTimeline() {
        List<File> images = new ArrayList<>();
        List<TransitionItem> transitions = new ArrayList<>();
        // Collect images and transitions from the current model (in order)
        for (int i = 0; i < listModel.getSize(); i++) {
            Object obj = listModel.get(i);
            if (obj instanceof File) {
                images.add((File) obj);
            } else if (obj instanceof TransitionItem) {
                transitions.add((TransitionItem) obj);
            }
        }
        List<Object> normalized = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            normalized.add(images.get(i));
            if (i < images.size() - 1) {
                if (i < transitions.size()) {
                    normalized.add(transitions.get(i));
                } else {
                    normalized.add(new TransitionItem("Fade", 2000));
                }
            }
        }
        listModel.clear();
        for (Object obj : normalized) {
            listModel.addElement(obj);
        }
    }

    // --- Custom Cell Renderer for Images and Transitions ---
    private static class TimelineListCellRenderer extends JLabel implements ListCellRenderer<Object> {
        public TimelineListCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
        }
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof File) {
                // Render image thumbnail.
                File file = (File) value;
                ImageIcon icon = new ImageIcon(file.getAbsolutePath());
                Image image = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                setIcon(new ImageIcon(image));
                setText(SHOW_IMAGE_NAMES ? file.getName() : "");
                setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
                setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            } else if (value instanceof TransitionItem) {
                // Render transition placeholder with duration in seconds.
                TransitionItem transition = (TransitionItem) value;
                setIcon(null);
                setText("<html><center>Transition:<br>" 
                        + transition.getTransitionType() 
                        + "<br>Duration: " + (transition.getDuration() / 1000) + " s</center></html>");
                setBackground(Color.LIGHT_GRAY);
                setForeground(Color.BLACK);
            } else {
                setText(value.toString());
                setIcon(null);
            }
            if (isSelected) {
                setBorder(javax.swing.BorderFactory.createLineBorder(Color.BLUE, 2));
            } else {
                setBorder(null);
            }
            return this;
        }
    }

    // --- TransferHandler for Reordering Items in the JList ---
    // (Non-static so we can call normalizeTimeline())
    private class ListItemTransferHandler extends TransferHandler {
        private int[] indices = null;
        private int addIndex = -1; // Where items were inserted.
        private int addCount = 0;  // Number of items inserted.
        private boolean swapOccurred = false; // Indicates if a swap was performed.
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
                // If exactly one element is dragged and it is a TransitionItem, and the target slot already contains a TransitionItem, swap them.
                if (values.size() == 1 && values.get(0) instanceof TransitionItem) {
                    if (index < model.getSize() && model.getElementAt(index) instanceof TransitionItem) {
                        Object draggedItem = values.get(0);
                        int originalIndex = indices[0];
                        Object targetItem = model.getElementAt(index);
                        model.set(index, draggedItem);
                        model.set(originalIndex, targetItem);
                        swapOccurred = true;
                        return true;
                    }
                }
                addCount = values.size();
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
            if (!swapOccurred && action == MOVE && indices != null) {
                JList source = (JList) c;
                DefaultListModel model = (DefaultListModel) source.getModel();
                if (addCount > 0) {
                    for (int i = indices.length - 1; i >= 0; i--) {
                        if (indices[i] >= addIndex) {
                            indices[i] += addCount;
                        }
                    }
                }
                for (int i = indices.length - 1; i >= 0; i--) {
                    model.remove(indices[i]);
                }
            }
            indices = null;
            addCount = 0;
            addIndex = -1;
            swapOccurred = false;
            normalizeTimeline();
        }
    }

    // --- Custom Transferable for a List of Objects ---
    private static class ListTransferable implements Transferable {
        private final List<?> data;
        public static final DataFlavor localFlavor;
        static {
            DataFlavor flavor = null;
            try {
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
     * TransitionItem represents a transition between images.
     * It holds a transition type and a duration (in milliseconds).
     */
    public static class TransitionItem {
        private String transitionType;
        private int duration; // Duration in milliseconds
        public TransitionItem(String transitionType, int duration) {
            this.transitionType = transitionType;
            this.duration = duration;
        }
        public String getTransitionType() {
            return transitionType;
        }
        public void setTransitionType(String transitionType) {
            this.transitionType = transitionType;
        }
        public int getDuration() {
            return duration;
        }
        public void setDuration(int duration) {
            this.duration = duration;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
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
    }// </editor-fold>
}
