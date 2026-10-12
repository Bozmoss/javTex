package com.example.latex.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PDFViewer extends JPanel {
    private List<BufferedImage> pageImages = new ArrayList<>();
    private final List<JLabel> pageLabels = new ArrayList<>();
    private final JPanel pagesPanel;
    private final JLabel statusLabel;

    private double zoom = 1.0;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 3.0;
    private static final double ZOOM_STEP = 0.1;

    public PDFViewer() {
        setLayout(new BorderLayout());

        pagesPanel = new JPanel();
        pagesPanel.setLayout(
            new BoxLayout(pagesPanel, BoxLayout.Y_AXIS)
        );

        statusLabel = new JLabel("No PDF Loaded");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JScrollPane scrollPane = new JScrollPane(pagesPanel);

        pagesPanel.addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                e.consume();

                double newZoom = zoom - e.getPreciseWheelRotation() * ZOOM_STEP;
                setZoom(newZoom);
            }
        });

        // Adjust vertical scrolling speed (pixels per unit)
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setBlockIncrement(100);

        // Adjust horizontal scrolling speed (pixels per unit)
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(100);

        add(statusLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void loadPDF(Path pdfFile) {
        if (!Files.isRegularFile(pdfFile)) {
            showError("PDF file not found: " + pdfFile);
            return;
        }

        statusLabel.setText("Loading PDF...");
        pagesPanel.removeAll();
        pagesPanel.revalidate();
        pagesPanel.repaint();

        SwingWorker<List<BufferedImage>, Void> worker = new SwingWorker<>() {
            @Override 
            protected List<BufferedImage> doInBackground() throws IOException {
                List<BufferedImage> images = new ArrayList<>();

                try (PDDocument document = Loader.loadPDF(pdfFile.toFile())) {
                    PDFRenderer renderer = new PDFRenderer(document);
                    for (int i = 0; i < document.getNumberOfPages(); i++) {
                        images.add(renderer.renderImageWithDPI(i, 200));
                    }
                }
                return images;
            }

            @Override 
            protected void done() {
                try {
                    pageImages.clear();
                    pageLabels.clear();
                    pagesPanel.removeAll();
                    pageImages = get();

                    for (BufferedImage image : pageImages) {
                        JLabel pageLabel = new JLabel();
                        pageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                        pageLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

                        pageLabels.add(pageLabel);
                        pagesPanel.add(pageLabel);
                    }

                    zoom = 0.0;
                    setZoom(1.0);

                    statusLabel.setText(
                        pageImages.size() + " page(s) - " + pdfFile.getFileName()
                    );

                    pagesPanel.revalidate();
                    pagesPanel.repaint();
                } catch (Exception e) {
                    showError("Could no load PDF: " + e.getMessage());
                }
            }
        };

        worker.execute();
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

        for (int i = 0; i < pageImages.size(); i++) {
            BufferedImage original = pageImages.get(i);

            int width = (int) (original.getWidth() * zoom);
            int height = (int) (original.getHeight() * zoom);

            BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );

            g.drawImage(original, 0, 0, width, height, null);
            g.dispose();

            pageLabels.get(i).setIcon(new ImageIcon(scaled));
        }

        pagesPanel.revalidate();
        pagesPanel.repaint();
    }
}