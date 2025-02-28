package com.team5.senior_project;

import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.ExpandVetoException;

public class FileExplorerPanel extends JPanel {

    private JTree fileTree;
    private DefaultTreeModel treeModel;
    private FileSystemView fileSystemView;

    public FileExplorerPanel() {
        fileSystemView = FileSystemView.getFileSystemView();
        // Use the user's home directory as the root.
        File root = fileSystemView.getHomeDirectory();
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(root);
        treeModel = new DefaultTreeModel(rootNode);
        fileTree = new JTree(treeModel);
        fileTree.setCellRenderer(new FileTreeCellRenderer());
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        fileTree.setDragEnabled(true);
        fileTree.setTransferHandler(new FileTreeTransferHandler());

        // Initially, create children for the root with lazy loading.
        createChildren(rootNode, root);
        fileTree.expandRow(0);

        // Add a listener for lazy loading deeper nodes.
        fileTree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                // If the node has a single dummy child, then load its real children.
                if (node.getChildCount() == 1) {
                    DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) node.getFirstChild();
                    if ("dummy".equals(firstChild.getUserObject())) {
                        node.removeAllChildren();
                        File dir = (File) node.getUserObject();
                        createChildren(node, dir);
                        treeModel.reload(node);
                    }
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                // No action needed on collapse
            }
        });

        setLayout(new BorderLayout());
        add(new JScrollPane(fileTree), BorderLayout.CENTER);
    }

    /**
     * Create child nodes for a given directory node.
     * For each subdirectory, if it contains at least one subdirectory,
     * a dummy node is added to indicate that it can be expanded.
     */
    private void createChildren(DefaultMutableTreeNode node, File file) {
        File[] files = fileSystemView.getFiles(file, true);
        for (File child : files) {
            if (child.isDirectory()) {
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
                node.add(childNode);
                // Check if the directory has subdirectories. If so, add a dummy node.
                File[] grandChildren = fileSystemView.getFiles(child, true);
                boolean hasSubDir = false;
                for (File gc : grandChildren) {
                    if (gc.isDirectory()) {
                        hasSubDir = true;
                        break;
                    }
                }
                if (hasSubDir) {
                    // Add a dummy node to show expandable icon.
                    childNode.add(new DefaultMutableTreeNode("dummy"));
                }
            }
        }
    }

    // Custom renderer to display system icons and names.
    private class FileTreeCellRenderer extends javax.swing.tree.DefaultTreeCellRenderer {
        @Override
        public java.awt.Component getTreeCellRendererComponent(JTree tree, Object value,
                                                               boolean sel, boolean expanded,
                                                               boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            // If the node represents a dummy, show nothing.
            if ("dummy".equals(node.getUserObject())) {
                setText("");
                setIcon(null);
            } else {
                File file = (File) node.getUserObject();
                setText(fileSystemView.getSystemDisplayName(file));
                setIcon(fileSystemView.getSystemIcon(file));
            }
            return this;
        }
    }
    
    public JTree getFileTree() {
        return fileTree;
    }
    
    public File getSelectedDirectory() {
        javax.swing.tree.TreePath path = fileTree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object userObj = node.getUserObject();
            if (userObj instanceof File) {
                File f = (File) userObj;
                if (f.isDirectory()) {
                    return f;
                }
            }
        }
        return null;
    }

    // TransferHandler to allow dragging of image files only.
    private class FileTreeTransferHandler extends TransferHandler {
        @Override
        protected Transferable createTransferable(javax.swing.JComponent c) {
            JTree tree = (JTree) c;
            TreePath path = tree.getSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                // Ensure we're not dragging a dummy node.
                if ("dummy".equals(node.getUserObject())) {
                    return null;
                }
                File file = (File) node.getUserObject();
                String name = file.getName().toLowerCase();
                if (name.endsWith(".jpg") || name.endsWith(".jpeg")
                        || name.endsWith(".png") || name.endsWith(".gif")) {
                    return new FileTransferable(file);
                }
            }
            return null;
        }

        @Override
        public int getSourceActions(javax.swing.JComponent c) {
            return COPY;
        }
    }

    // Wraps a file (as a list) for drag-and-drop.
    private class FileTransferable implements Transferable {
        private final File file;

        public FileTransferable(File file) {
            this.file = file;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.javaFileListFlavor};
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
            List<File> files = new ArrayList<>();
            files.add(file);
            return files;
        }
    }
}
