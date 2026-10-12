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
    private final JPanel pagesPanel;
    private final JLabel statusLabel;

    public PDFViewer() {
        setLayout(new BorderLayout());

        pagesPanel = new JPanel();
        pagesPanel.setLayout(
            new BoxLayout(pagesPanel, BoxLayout.Y_AXIS)
        );

        statusLabel = new JLabel("No PDF Loaded");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JScrollPane scrollPane = new JScrollPane(pagesPanel);

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
                        images.add(renderer.renderImageWithDPI(i, 110));
                    }
                }
                return images;
            }

            @Override 
            protected void done() {
                try {
                    List<BufferedImage> images = get();

                    for (int i = 0; i < images.size(); i++) {
                        JLabel pageLabel = new JLabel(new ImageIcon(images.get(i)));
                        pageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                        pageLabel.setBorder(
                            BorderFactory.createEmptyBorder(8, 8, 8, 8)
                        );
                        pagesPanel.add(pageLabel);
                    }

                    statusLabel.setText(
                        images.size() + " page(s) - " + pdfFile.getFileName()
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
}