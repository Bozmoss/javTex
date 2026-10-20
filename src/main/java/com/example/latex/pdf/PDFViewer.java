package com.example.latex.pdf;

import java.awt.BorderLayout;
import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.SwingViewBuilder;
import org.icepdf.ri.common.views.DocumentViewControllerImpl;

public class PDFViewer extends JPanel {
    private final SwingController controller;
    private final JLabel statusLabel;

    private Path currentPDF;

    public PDFViewer() {
        setLayout(new BorderLayout());

        controller = new SwingController();
        JPanel icePdfPanel = new SwingViewBuilder(controller).buildViewerPanel();
        BorderLayout icePdfLayout = (BorderLayout) icePdfPanel.getLayout();

        Component toolbar = icePdfLayout.getLayoutComponent(BorderLayout.NORTH);
        if (toolbar != null) {
            icePdfPanel.remove(toolbar);
        }

        Component icePdfStatus = icePdfLayout.getLayoutComponent(BorderLayout.SOUTH);
        if (icePdfStatus != null) {
            icePdfPanel.remove(icePdfStatus);
        }

        Component documentArea = icePdfLayout.getLayoutComponent(BorderLayout.CENTER);
        if (!(documentArea instanceof JSplitPane documentSplitPane)) {
            throw new IllegalStateException("ICEpdf did not create its document split pane");
        }
        documentSplitPane.setLeftComponent(null);
        documentSplitPane.setDividerSize(0);

        statusLabel = new JLabel("No PDF Loaded");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        add(statusLabel, BorderLayout.NORTH);
        add(icePdfPanel, BorderLayout.CENTER);
    }

    public void loadPDF(Path pdfFile) {
        closePDF();

        if (!Files.isRegularFile(pdfFile)) {
            showError("PDF file not found: " + pdfFile);
            return;
        }

        try {
            controller.openDocument(pdfFile.toAbsolutePath().toString());
            controller.setPageViewMode(DocumentViewControllerImpl.ONE_COLUMN_VIEW, true);
            currentPDF = pdfFile;
            statusLabel.setText("Loaded: " + pdfFile.getFileName());
        } catch (RuntimeException e) {
            showError("Could not load PDF: " + e.getMessage());
        }
    }

    private void showError(String message) {
        statusLabel.setText(message);
    }

    public void closePDF() {
        if (currentPDF != null) {
            controller.closeDocument();
            currentPDF = null;
        }

        statusLabel.setText("No PDF Loaded");
    }
}
