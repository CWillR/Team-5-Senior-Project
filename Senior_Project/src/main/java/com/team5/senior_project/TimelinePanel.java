package com.team5.senior_project;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class TimelinePanel extends JPanel {

    private DefaultListModel<File> listModel;
    private JList<File> imageList;
    
    // Toggle for showing image names in the timeline preview.
    public static boolean SHOW_IMAGE_NAMES = false;

    // Listener interface for timeline changes.
    public interface TimelineChangeListener {
        void onTimelineChanged();
    }
    private TimelineChangeListener timelineChangeListener;
    
    public void setTimelineChangeListener(TimelineChangeListener listener) {
        this.timelineChangeListener = listener;
    }
    
    public TimelinePanel() {
        // Build the UI directly without calling initComponents()
        listModel = new DefaultListModel<>();
        imageList = new JList<>(listModel);
        imageList.setCellRenderer(new ImageListCellRenderer());
        imageList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        imageList.setVisibleRowCount(1);
        imageList.setDragEnabled(true);
        imageList.setDropMode(DropMode.INSERT);
        imageList.setTransferHandler(new ListItemTransferHandler());
        
        setLayout(new BorderLayout());
        add(new JScrollPane(imageList), BorderLayout.CENTER);
        
        // Notify listener whenever the list changes.
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
    
    // Sets the timeline images.
    public void setImages(List<File> images) {
        listModel.clear();
        for (File file : images) {
            listModel.addElement(file);
        }
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
        public ImageListCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
        }
        @Override
        public Component getListCellRendererComponent(JList<? extends File> list, File value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            ImageIcon icon = new ImageIcon(value.getAbsolutePath());
            Image image = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            setIcon(new ImageIcon(image));
            setText(SHOW_IMAGE_NAMES ? value.getName() : "");
            if (isSelected) {
                setBorder(new LineBorder(Color.BLUE, 2));
            } else {
                setBorder(null);
            }
            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            return this;
        }
    }
    
    // --- TransferHandler for Drag-and-Drop Reordering ---
    private static class ListItemTransferHandler extends TransferHandler {
        private int[] indices = null;
        private int addIndex = -1;
        private int addCount = 0;
        
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
        public boolean canImport(TransferHandler.TransferSupport info) {
            return info.isDataFlavorSupported(ListTransferable.localFlavor)
                    || info.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }
        
        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
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
                List<?> values;
                if (info.isDataFlavorSupported(ListTransferable.localFlavor)) {
                    @SuppressWarnings("unchecked")
                    List<Object> localValues = (List<Object>) info.getTransferable().getTransferData(ListTransferable.localFlavor);
                    values = localValues;
                } else if (info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    @SuppressWarnings("unchecked")
                    List<Object> fileList = (List<Object>) info.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    values = fileList;
                } else {
                    return false;
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
            if (action == MOVE && indices != null) {
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
}
