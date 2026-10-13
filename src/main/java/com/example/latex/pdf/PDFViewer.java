package com.example.latex.pdf;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.util.GraphicsRenderingHints;

public class PDFViewer extends JPanel {
    private List<BufferedImage> pageImages = new ArrayList<>();
    private final List<JLabel> pageLabels = new ArrayList<>();
    private final JPanel pagesPanel;
    private final JLabel statusLabel;
    private final JScrollPane scrollPane;

    private double zoom = 1.0;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 3.0;
    private static final double ZOOM_STEP = 0.1;

    private Path currentPDF;
    private SwingWorker<List<BufferedImage>, Void> renderWorker;
    private int renderGeneration = 0;

    private static final float BASE_DPI = 200f;

    public PDFViewer() {
        setLayout(new BorderLayout());

        pagesPanel = new JPanel();
        pagesPanel.setLayout(
            new BoxLayout(pagesPanel, BoxLayout.Y_AXIS)
        );

        statusLabel = new JLabel("No PDF Loaded");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        scrollPane = new JScrollPane(pagesPanel);

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setBlockIncrement(100);

        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(100);

        scrollPane.addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                e.consume();

                double newZoom = zoom - e.getPreciseWheelRotation() * ZOOM_STEP;
                setZoom(newZoom);
            } else {
                JScrollBar verticalBar = scrollPane.getVerticalScrollBar();

                int rotation = e.getWheelRotation();
                int increment = verticalBar.getUnitIncrement(rotation);

                verticalBar.setValue(verticalBar.getValue() + rotation * increment);

                e.consume();
            }
        });

        add(statusLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void loadPDF(Path pdfFile) {
        closePDF();

        if (!Files.isRegularFile(pdfFile)) {
            showError("PDF file not found: " + pdfFile);
            return;
        }

        currentPDF = pdfFile;
        zoom = 1.0;

        renderPages();
    }

    public void renderPages() {
        if (currentPDF == null) {
            return;
        }

        // Invalidation
        int generation = ++renderGeneration;

        if (renderWorker != null) {
            renderWorker.cancel(true);
        }

        Path pdfFile = currentPDF;
        float dpi = (float) (BASE_DPI * zoom);

        statusLabel.setText(String.format("Rendering PDF at %.0f of DPI...", dpi));

        renderWorker = new SwingWorker<>() {
            @Override 
            protected List<BufferedImage> doInBackground() throws Exception {
                List<BufferedImage> images = new ArrayList<>();
                Document document = new Document();

                try {
                    document.setFile(pdfFile.toAbsolutePath().toString());

                    for (int i = 0; i < document.getNumberOfPages(); i++) {
                        if (isCancelled()) {
                            break;
                        }

                        BufferedImage image = (BufferedImage) document.getPageImage(
                            i,
                            GraphicsRenderingHints.SCREEN,
                            Page.BOUNDARY_CROPBOX,
                            0f,
                            (float) zoom
                        );

                        if (image != null) {
                            images.add(image);
                        }
                    }
                } finally {
                    document.dispose();
                }

                return images;
            }

            @Override 
            protected void done() {
                if (generation != renderGeneration || isCancelled()) {
                    return;
                }

                try {
                    List<BufferedImage> images = get();

                    pageImages.clear();
                    pageImages.addAll(images);

                    pagesPanel.removeAll();
                    pageLabels.clear();

                    for (BufferedImage image : pageImages) {
                        JLabel label = new JLabel(new ImageIcon(image));

                        label.setAlignmentX(Component.CENTER_ALIGNMENT);
                        label.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

                        pageLabels.add(label);
                        pagesPanel.add(label);
                    }

                    statusLabel.setText(
                        pageImages.size() + " page(s) - "
                        + pdfFile.getFileName()
                        + String.format(" (%.0f%%)", zoom * 100)
                    );

                    pagesPanel.revalidate();
                    pagesPanel.repaint();
                } catch (Exception e) {
                    if (generation == renderGeneration) {
                        showError("Could not render PDF: " + e.getMessage());
                    }
                }
            }
        };

        renderWorker.execute();
    }

    private void showError(String message) {
        statusLabel.setText(message);
        pagesPanel.removeAll();
        pagesPanel.revalidate();
        pagesPanel.repaint();
    }

    private void setZoom(double newZoom) {
        double clampedZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        if (Math.abs(clampedZoom - zoom) < 0.001) {
            return;
        }
        zoom = clampedZoom;

        renderPages();
    }

    public void closePDF() {
        renderGeneration++;

        if (renderWorker != null) {
            renderWorker.cancel(true);
            renderWorker = null;
        }

        currentPDF = null;

        pageImages.clear();
        pageLabels.clear();

        pagesPanel.removeAll();
        pagesPanel.revalidate();
        pagesPanel.repaint();

        zoom = 1.0;
        statusLabel.setText("No PDF Loaded");
    }
}