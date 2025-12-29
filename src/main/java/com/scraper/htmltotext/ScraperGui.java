package com.scraper.htmltotext;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScraperGui {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ScraperGui::new);
    }

    private final DefaultListModel<String> urlModel = new DefaultListModel<>();

    // Palette
    private static final Color BG = new Color(0xF7, 0xF7, 0xFB); // #F7F7FB
    private static final Color SURFACE = new Color(0xFF, 0xFF, 0xFF); // #FFFFFF
    private static final Color SURFACE_2 = new Color(0xF1, 0xF3, 0xF6); // #F1F3F6
    private static final Color BORDER = new Color(0xD7, 0xDB, 0xE3); // #D7DBE3
    private static final Color TEXT = new Color(0x11, 0x18, 0x27); // #111827
    private static final Color ACCENT = new Color(0x1F, 0x53, 0xC4); // #1F53C4
    private static final Color ACCENT_HOVER = new Color(0x1A, 0x47, 0xA7); // #1A47A7
    private static final Color DANGER = new Color(0xDC, 0x26, 0x26); // #DC2626
    private static final Color SELECTION_BG = new Color(0xDB, 0xEA, 0xFE); // #DBEAFE
    private static final Color SELECTION_FG = TEXT;
    private static final Cursor POINTER_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    public ScraperGui() {
        JFrame frame = new JFrame("HTML to Text Scraper");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLocationByPlatform(false);

        JTextField urlField = new JTextField();
        JButton addBtn = new JButton("Add URL");
        JButton clearBtn = new JButton("Clear");
        JButton saveBtn = new JButton("Save");

        JList<String> urlList = new JList<>(urlModel);
        urlList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final int xButtonWidth = 26;
        final int gapAfterX = 10;

        // Apply styles to base components
        applyTheme(frame, urlField, addBtn, clearBtn, saveBtn, urlList);

        // Add X for URLs
        urlList.setCellRenderer(new DefaultListCellRenderer() {
            private final Border rowPadding = new EmptyBorder(6, 8, 6, 8);

            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                JLabel base = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                Color rowBg = isSelected ? SELECTION_BG : SURFACE;
                Color rowFg = isSelected ? SELECTION_FG : TEXT;

                JPanel row = new JPanel();
                row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
                row.setOpaque(true);
                row.setBackground(rowBg);
                row.setBorder(rowPadding);

                JLabel xBox = new JLabel("X", SwingConstants.CENTER);
                xBox.setPreferredSize(new Dimension(xButtonWidth, base.getPreferredSize().height));
                xBox.setMaximumSize(new Dimension(xButtonWidth, Integer.MAX_VALUE));
                xBox.setMinimumSize(new Dimension(xButtonWidth, 0));
                xBox.setOpaque(true);
                xBox.setBackground(DANGER);
                xBox.setForeground(Color.WHITE);
                xBox.setFont(base.getFont().deriveFont(Font.BOLD));
                xBox.setBorder(new CompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xB9, 0x1C, 0x1C)),
                        new EmptyBorder(2, 0, 2, 0)));

                JLabel text = new JLabel(String.valueOf(value));
                text.setOpaque(false);
                text.setForeground(rowFg);
                text.setFont(base.getFont());

                row.add(xBox);
                row.add(Box.createRigidArea(new Dimension(gapAfterX, 0)));
                row.add(text);

                return row;
            }
        });

        
        urlList.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = urlList.locationToIndex(e.getPoint());
                if (index < 0) {
                    urlList.setCursor(Cursor.getDefaultCursor());
                    return;
                }

                Rectangle cellBounds = urlList.getCellBounds(index, index);
                if (cellBounds == null) {
                    urlList.setCursor(Cursor.getDefaultCursor());
                    return;
                }
                int xWithinCell = e.getX() - cellBounds.x;
                if (xWithinCell <= (xButtonWidth + 12)) {
                    urlList.setCursor(POINTER_CURSOR);
                } else {
                    urlList.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        // Remove on click
        urlList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = urlList.locationToIndex(e.getPoint());
                if (index < 0)
                    return;

                Rectangle cellBounds = urlList.getCellBounds(index, index);
                if (cellBounds == null)
                    return;

                int xWithinCell = e.getX() - cellBounds.x;

                if (xWithinCell <= (xButtonWidth + 12)) {
                    urlModel.remove(index);
                }
            }
        });

        addBtn.addActionListener(e -> {
            String u = urlField.getText().trim();
            if (u.isEmpty()) {
                showMessageCentered(frame, "URL cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            urlModel.addElement(u);
            urlField.setText("");
        });

        clearBtn.addActionListener(e -> urlModel.clear());

        saveBtn.addActionListener(e -> {
            if (urlModel.isEmpty()) {
                showMessageCentered(frame, "Add at least one URL.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JFileChooser chooser = new JFileChooser() {
                @Override
                protected JDialog createDialog(Component parent) throws HeadlessException {
                    JDialog dialog = super.createDialog(parent);
                    dialog.setModal(true);
                    centerOnScreen(dialog);
                    return dialog;
                }
            };
            chooser.setDialogTitle("Save Markdown Output");
            chooser.setSelectedFile(new java.io.File("notes.md"));

            int result = chooser.showSaveDialog(frame);
            if (result != JFileChooser.APPROVE_OPTION)
                return;

            Path out = chooser.getSelectedFile().toPath();
            saveBtn.setEnabled(false);

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try (BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
                        for (int i = 0; i < urlModel.size(); i++) {
                            String url = urlModel.get(i);

                            writer.write("---\n");
                            writer.write("Source: " + url + "\n");
                            writer.write("Text:\n\n");

                            try {
                                String html = Scraper.fetchHtml(url);
                                String text = Scraper.extractReadableText(html);

                                if (text != null && !text.isBlank()) {
                                    writer.write(text);
                                    writer.write("\n");
                                } else {
                                    writer.write("Couldn't extract readable text.\n");
                                }
                            } catch (Exception ex) {
                                String msg = ex.getMessage();
                                if (msg == null || msg.isBlank())
                                    msg = ex.getClass().getSimpleName();

                                writer.write("Couldn't reach website or something went wrong.\n");
                                writer.write("Reason: " + msg + "\n");
                            }

                            writer.write("\n");
                            writer.flush();
                        }
                    }
                    return null;
                }

                @Override
                protected void done() {
                    saveBtn.setEnabled(true);
                    try {
                        get();
                        showMessageCentered(frame, "Saved:\n" + out.toAbsolutePath(),
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        showMessageCentered(frame, "Error:\n" + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };

            worker.execute();
        });

        // Layout
        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setOpaque(false);

        JPanel addPanel = new JPanel(new BorderLayout(10, 10));
        addPanel.setOpaque(false);
        addPanel.add(urlField, BorderLayout.CENTER);
        addPanel.add(addBtn, BorderLayout.EAST);
        top.add(addPanel, BorderLayout.NORTH);

        JPanel bottomButtons = new JPanel(new BorderLayout(10, 10));
        bottomButtons.setOpaque(false);
        bottomButtons.add(clearBtn, BorderLayout.WEST);
        bottomButtons.add(saveBtn, BorderLayout.EAST);

        JScrollPane listScroll = new JScrollPane(urlList);
        listScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        listScroll.getViewport().setBackground(SURFACE);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBackground(BG);
        content.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        content.add(top, BorderLayout.NORTH);
        content.add(listScroll, BorderLayout.CENTER);
        content.add(bottomButtons, BorderLayout.SOUTH);

        frame.setContentPane(content);

        frame.pack();
        frame.setVisible(true);

        enforceAlwaysCentered(frame);
    }

    private static void applyTheme(JFrame frame,
            JTextField urlField,
            JButton addBtn,
            JButton clearBtn,
            JButton saveBtn,
            JList<String> urlList) {

        frame.getContentPane().setBackground(BG);

        urlField.setBackground(SURFACE);
        urlField.setForeground(TEXT);
        urlField.setCaretColor(TEXT);
        urlField.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 10, 8, 10)));

        urlList.setBackground(SURFACE);
        urlList.setForeground(TEXT);
        urlList.setSelectionBackground(SELECTION_BG);
        urlList.setSelectionForeground(SELECTION_FG);
        urlList.setFixedCellHeight(36);

        styleButtonPrimary(addBtn);
        styleButtonPrimary(clearBtn);
        styleButtonPrimary(saveBtn);
    }

    private static void styleButtonPrimary(JButton b) {
        b.setForeground(Color.WHITE);
        b.setBackground(ACCENT);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(POINTER_CURSOR);
        b.setBorder(new EmptyBorder(8, 14, 8, 14));

        b.addChangeListener(e -> {
            ButtonModel m = b.getModel();
            if (m.isPressed()) {
                b.setBackground(ACCENT_HOVER);
            } else if (m.isRollover()) {
                b.setBackground(ACCENT_HOVER);
            } else {
                b.setBackground(ACCENT);
            }
        });
    }

    private static void enforceAlwaysCentered(Window w) {
        Timer t = new Timer(200, e -> {
            if (!w.isShowing())
                return;
            centerOnScreen(w);
        });
        t.setRepeats(true);
        t.start();
    }

    private static void centerOnScreen(Window w) {
        if (w == null)
            return;

        GraphicsConfiguration gc = w.getGraphicsConfiguration();
        Rectangle bounds = (gc != null)
                ? gc.getBounds()
                : GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice()
                        .getDefaultConfiguration()
                        .getBounds();

        Insets insets = (gc != null)
                ? Toolkit.getDefaultToolkit().getScreenInsets(gc)
                : new Insets(0, 0, 0, 0);

        int usableX = bounds.x + insets.left;
        int usableY = bounds.y + insets.top;
        int usableW = bounds.width - insets.left - insets.right;
        int usableH = bounds.height - insets.top - insets.bottom;

        int x = usableX + Math.max(0, (usableW - w.getWidth()) / 2);
        int y = usableY + Math.max(0, (usableH - w.getHeight()) / 2);

        w.setLocation(x, y);
    }

    private static void showMessageCentered(Component parent, String message, String title, int messageType) {
        JOptionPane optionPane = new JOptionPane(message, messageType);
        JDialog dialog = optionPane.createDialog(parent, title);
        dialog.setModal(true);
        centerOnScreen(dialog);
        dialog.setVisible(true);
    }
}
