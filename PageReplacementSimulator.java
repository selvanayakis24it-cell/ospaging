import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class PageReplacementSimulator {

    private static final Pattern SPLIT_PATTERN = Pattern.compile("[\\s,]+");
    private static final Color PAGE_BG_TOP = new Color(7, 18, 34);
    private static final Color PAGE_BG_BOTTOM = new Color(10, 56, 79);
    private static final Color CARD_BG = new Color(241, 248, 253);
    private static final Color INK_DARK = new Color(16, 39, 64);
    private static final Color OUTPUT_BG = new Color(17, 24, 39);
    private static final Color OUTPUT_TEXT = new Color(228, 238, 246);

    private static class Palette {
        final Color primary;
        final Color secondary;

        Palette(Color primary, Color secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }
    }

    private static class StepResult {
        int step;
        int page;
        int[] frames;
        boolean fault;

        StepResult(int step, int page, int[] frames, boolean fault) {
            this.step = step;
            this.page = page;
            this.frames = frames;
            this.fault = fault;
        }
    }

    private static class SimulationResult {
        String algorithm;
        int faults;
        int hits;
        List<StepResult> steps = new ArrayList<>();

        double faultRatio() {
            int total = faults + hits;
            return total == 0 ? 0.0 : (double) faults / total;
        }
    }

    private static int findPageIndex(int[] frames, int page) {
        for (int i = 0; i < frames.length; i++) {
            if (frames[i] == page) {
                return i;
            }
        }
        return -1;
    }

    private static int findEmptyFrame(int[] frames) {
        for (int i = 0; i < frames.length; i++) {
            if (frames[i] == -1) {
                return i;
            }
        }
        return -1;
    }

    private static SimulationResult simulateFIFO(int[] pages, int capacity) {
        SimulationResult result = new SimulationResult();
        result.algorithm = "FIFO";

        int[] frames = new int[capacity];
        Arrays.fill(frames, -1);
        int pointer = 0;

        for (int i = 0; i < pages.length; i++) {
            int page = pages[i];
            boolean fault = false;

            if (findPageIndex(frames, page) == -1) {
                fault = true;
                int empty = findEmptyFrame(frames);
                if (empty != -1) {
                    frames[empty] = page;
                } else {
                    frames[pointer] = page;
                    pointer = (pointer + 1) % capacity;
                }
                result.faults++;
            } else {
                result.hits++;
            }

            result.steps.add(new StepResult(i + 1, page, Arrays.copyOf(frames, capacity), fault));
        }

        return result;
    }

    private static SimulationResult simulateLRU(int[] pages, int capacity) {
        SimulationResult result = new SimulationResult();
        result.algorithm = "LRU";

        int[] frames = new int[capacity];
        int[] lastUsed = new int[capacity];
        Arrays.fill(frames, -1);
        Arrays.fill(lastUsed, -1);

        for (int i = 0; i < pages.length; i++) {
            int page = pages[i];
            boolean fault = false;
            int idx = findPageIndex(frames, page);

            if (idx == -1) {
                fault = true;
                int empty = findEmptyFrame(frames);
                if (empty != -1) {
                    frames[empty] = page;
                    lastUsed[empty] = i;
                } else {
                    int lruIdx = 0;
                    int minUse = lastUsed[0];
                    for (int j = 1; j < capacity; j++) {
                        if (lastUsed[j] < minUse) {
                            minUse = lastUsed[j];
                            lruIdx = j;
                        }
                    }
                    frames[lruIdx] = page;
                    lastUsed[lruIdx] = i;
                }
                result.faults++;
            } else {
                lastUsed[idx] = i;
                result.hits++;
            }

            result.steps.add(new StepResult(i + 1, page, Arrays.copyOf(frames, capacity), fault));
        }

        return result;
    }

    private static SimulationResult simulateOptimal(int[] pages, int capacity) {
        SimulationResult result = new SimulationResult();
        result.algorithm = "Optimal";

        int[] frames = new int[capacity];
        Arrays.fill(frames, -1);

        for (int i = 0; i < pages.length; i++) {
            int page = pages[i];
            boolean fault = false;

            if (findPageIndex(frames, page) == -1) {
                fault = true;
                int empty = findEmptyFrame(frames);

                if (empty != -1) {
                    frames[empty] = page;
                } else {
                    int replaceIdx = 0;
                    int farthest = -1;

                    for (int j = 0; j < capacity; j++) {
                        int nextUse = Integer.MAX_VALUE;
                        for (int k = i + 1; k < pages.length; k++) {
                            if (frames[j] == pages[k]) {
                                nextUse = k;
                                break;
                            }
                        }

                        if (nextUse == Integer.MAX_VALUE) {
                            replaceIdx = j;
                            break;
                        }

                        if (nextUse > farthest) {
                            farthest = nextUse;
                            replaceIdx = j;
                        }
                    }

                    frames[replaceIdx] = page;
                }
                result.faults++;
            } else {
                result.hits++;
            }

            result.steps.add(new StepResult(i + 1, page, Arrays.copyOf(frames, capacity), fault));
        }

        return result;
    }

    private static class HeroBackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            GradientPaint gradient = new GradientPaint(
                    0, 0, PAGE_BG_TOP,
                    width, height, PAGE_BG_BOTTOM
            );
            g2.setPaint(gradient);
            g2.fillRect(0, 0, width, height);

            g2.setColor(new Color(72, 162, 214, 28));
            g2.fillOval(-90, -70, 320, 280);
            g2.setColor(new Color(244, 184, 96, 35));
            g2.fillOval(width - 300, -60, 360, 260);
            g2.setColor(new Color(35, 211, 179, 22));
            g2.fillOval(width / 2 - 150, height - 120, 320, 230);
        }
    }

    private static class RoundedPanel extends JPanel {
        private final Color fill;
        private final int arc;

        RoundedPanel(Color fill, int arc) {
            this.fill = fill;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class SimulatorUI extends JFrame {
        private final JTextField frameInput = new JTextField("3", 8);
        private final JTextField refsInput = new JTextField("7 0 1 2 0 3 0 4 2 3 0 3 2", 36);
        private final JCheckBox fifoCheck = new JCheckBox("FIFO", true);
        private final JCheckBox lruCheck = new JCheckBox("LRU", true);
        private final JCheckBox optimalCheck = new JCheckBox("Optimal", true);
        private final JTabbedPane resultTabs = new JTabbedPane();
        private final JLabel totalRequestsValue = createMetricValueLabel();
        private final JLabel bestAlgoValue = createMetricValueLabel();
        private final JLabel bestFaultsValue = createMetricValueLabel();

        SimulatorUI() {
            setTitle("Page Replacement Simulator");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(1080, 730);
            setMinimumSize(new Dimension(920, 620));
            setLocationRelativeTo(null);

            HeroBackgroundPanel root = new HeroBackgroundPanel();
            root.setLayout(new BorderLayout(18, 18));
            root.setBorder(new EmptyBorder(20, 24, 20, 24));

            root.add(buildHeroPanel(), BorderLayout.NORTH);
            root.add(buildCenterPanel(), BorderLayout.CENTER);

            setContentPane(root);
        }

        private JPanel buildHeroPanel() {
            JPanel header = new JPanel(new BorderLayout(12, 8));
            header.setOpaque(false);

            JLabel title = new JLabel("Page Replacement Simulator");
            title.setFont(new Font("Segoe UI Variable", Font.BOLD, 36));
            title.setForeground(new Color(242, 247, 252));

            JLabel subtitle = new JLabel("Interactive visual dashboard for FIFO, LRU, and Optimal page replacement");
            subtitle.setFont(new Font("Segoe UI Variable", Font.PLAIN, 15));
            subtitle.setForeground(new Color(179, 207, 225));

            JPanel textWrap = new JPanel(new BorderLayout());
            textWrap.setOpaque(false);
            textWrap.add(title, BorderLayout.NORTH);
            textWrap.add(subtitle, BorderLayout.SOUTH);

            JLabel chip = new JLabel("OS MEMORY LAB");
            chip.setHorizontalAlignment(SwingConstants.CENTER);
            chip.setOpaque(true);
            chip.setBackground(new Color(245, 197, 99));
            chip.setForeground(new Color(46, 33, 7));
            chip.setBorder(new EmptyBorder(8, 14, 8, 14));
            chip.setFont(new Font("Segoe UI", Font.BOLD, 12));

            header.add(textWrap, BorderLayout.WEST);
            header.add(chip, BorderLayout.EAST);
            return header;
        }

        private JPanel buildCenterPanel() {
            JPanel center = new JPanel(new BorderLayout(14, 14));
            center.setOpaque(false);

            RoundedPanel controlCard = new RoundedPanel(CARD_BG, 20);
            controlCard.setLayout(new GridBagLayout());
            controlCard.setBorder(new EmptyBorder(16, 16, 16, 16));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(7, 7, 7, 7);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0;

            JLabel frameLabel = new JLabel("Frames:");
            frameLabel.setForeground(INK_DARK);
            frameLabel.setFont(new Font("Segoe UI Variable", Font.BOLD, 14));
            gbc.gridx = 0;
            gbc.gridy = 0;
            controlCard.add(frameLabel, gbc);

            styleInput(frameInput);
            gbc.gridx = 1;
            gbc.weightx = 0.18;
            controlCard.add(frameInput, gbc);

            JLabel refsLabel = new JLabel("Reference String:");
            refsLabel.setForeground(INK_DARK);
            refsLabel.setFont(new Font("Segoe UI Variable", Font.BOLD, 14));
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0;
            controlCard.add(refsLabel, gbc);

            styleInput(refsInput);
            gbc.gridx = 1;
            gbc.weightx = 1;
            controlCard.add(refsInput, gbc);

            JPanel checks = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            checks.setOpaque(false);
            configureCheck(fifoCheck, new Color(18, 120, 78));
            configureCheck(lruCheck, new Color(19, 93, 164));
            configureCheck(optimalCheck, new Color(153, 84, 17));
            checks.add(fifoCheck);
            checks.add(lruCheck);
            checks.add(optimalCheck);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 2;
            gbc.weightx = 1;
            controlCard.add(checks, gbc);

            JButton simulateBtn = createButton("Run Simulation", new Palette(new Color(10, 138, 84), new Color(16, 164, 102)));
            simulateBtn.addActionListener(e -> runSimulation());

            JButton clearBtn = createButton("Clear Results", new Palette(new Color(160, 79, 40), new Color(191, 98, 52)));
            clearBtn.addActionListener(e -> clearResults());

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            actions.setOpaque(false);
            actions.add(simulateBtn);
            actions.add(clearBtn);

            gbc.gridy = 3;
            controlCard.add(actions, gbc);

            RoundedPanel metrics = new RoundedPanel(new Color(230, 244, 252), 18);
            metrics.setLayout(new GridLayout(1, 3, 10, 0));
            metrics.setBorder(new EmptyBorder(10, 10, 10, 10));
            metrics.add(metricTile("Total Requests", totalRequestsValue, new Color(61, 132, 180)));
            metrics.add(metricTile("Best Algorithm", bestAlgoValue, new Color(36, 152, 112)));
            metrics.add(metricTile("Best Fault Count", bestFaultsValue, new Color(183, 105, 46)));

            RoundedPanel resultCard = new RoundedPanel(new Color(232, 242, 251), 20);
            resultCard.setLayout(new BorderLayout(8, 8));
            resultCard.setBorder(new EmptyBorder(12, 12, 12, 12));

            styleTabs(resultTabs);
            resultTabs.add("Welcome", createWelcomePanel());
            resultCard.add(resultTabs, BorderLayout.CENTER);

            center.add(controlCard, BorderLayout.NORTH);
            center.add(metrics, BorderLayout.CENTER);
            center.add(resultCard, BorderLayout.SOUTH);
            return center;
        }

        private JPanel metricTile(String title, JLabel value, Color accent) {
            RoundedPanel tile = new RoundedPanel(new Color(249, 252, 255), 14);
            tile.setLayout(new BorderLayout());
            tile.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 110), 1),
                    new EmptyBorder(10, 12, 10, 12)
            ));

            JLabel t = new JLabel(title);
            t.setFont(new Font("Segoe UI", Font.BOLD, 12));
            t.setForeground(accent.darker());

            value.setForeground(accent.darker());

            tile.add(t, BorderLayout.NORTH);
            tile.add(value, BorderLayout.CENTER);
            return tile;
        }

        private JLabel createMetricValueLabel() {
            JLabel label = new JLabel("-");
            label.setFont(new Font("Segoe UI Variable", Font.BOLD, 20));
            return label;
        }

        private void styleInput(JTextField textField) {
            textField.setFont(new Font("JetBrains Mono", Font.PLAIN, 14));
            textField.setForeground(new Color(22, 44, 71));
            textField.setBackground(Color.WHITE);
            textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(113, 156, 190), 1),
                    new EmptyBorder(7, 8, 7, 8)
            ));
        }

        private void configureCheck(JCheckBox checkBox, Color fg) {
            checkBox.setOpaque(false);
            checkBox.setForeground(fg);
            checkBox.setFont(new Font("Segoe UI Variable", Font.BOLD, 13));
        }

        private JButton createButton(String text, Palette palette) {
            JButton button = new JButton(text);
            button.setFocusPainted(false);
            button.setBackground(palette.primary);
            button.setForeground(Color.WHITE);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setFont(new Font("Segoe UI Variable", Font.BOLD, 13));
            button.setBorder(new EmptyBorder(9, 16, 9, 16));
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    button.setBackground(palette.secondary);
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    button.setBackground(palette.primary);
                }
            });
            return button;
        }

        private void styleTabs(JTabbedPane tabs) {
            tabs.setFont(new Font("Segoe UI Variable", Font.BOLD, 13));
            tabs.setBackground(new Color(225, 238, 249));
            tabs.setForeground(new Color(17, 40, 66));
            tabs.setBorder(BorderFactory.createLineBorder(new Color(120, 162, 195), 1));
        }

        private JComponent createWelcomePanel() {
            JTextArea area = new JTextArea();
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            area.setBackground(OUTPUT_BG);
            area.setForeground(OUTPUT_TEXT);
            area.setBorder(new EmptyBorder(14, 14, 14, 14));
            area.setText("Enter your frame count and reference string, choose algorithms, then click Run Simulation.\n\n"
                    + "Highlights:\n"
                    + "- Tabbed output for each algorithm\n"
                    + "- Instant best-algorithm summary\n"
                    + "- Step-by-step frame transitions with HIT/FAULT status");

            return new JScrollPane(area);
        }

        private JComponent createResultPanel(SimulationResult result, int frameCount) {
            JTextArea area = new JTextArea();
            area.setEditable(false);
            area.setFont(new Font("JetBrains Mono", Font.PLAIN, 14));
            area.setBackground(OUTPUT_BG);
            area.setForeground(OUTPUT_TEXT);
            area.setCaretColor(OUTPUT_TEXT);
            area.setBorder(new EmptyBorder(12, 12, 12, 12));

            StringBuilder sb = new StringBuilder();
            sb.append(result.algorithm).append(" Simulation\n");
            sb.append("==============================================================\n");
            sb.append("Step  Page  ");
            for (int i = 0; i < frameCount; i++) {
                sb.append(String.format(Locale.US, "F%-3d", i + 1));
            }
            sb.append("Status\n");

            for (StepResult step : result.steps) {
                sb.append(String.format(Locale.US, "%-5d %-5d ", step.step, step.page));
                for (int value : step.frames) {
                    String print = value == -1 ? "-" : String.valueOf(value);
                    sb.append(String.format(Locale.US, "%-4s", print));
                }
                sb.append(step.fault ? "FAULT" : "HIT").append("\n");
            }

            sb.append("--------------------------------------------------------------\n");
            sb.append(String.format(Locale.US,
                    "Faults: %d | Hits: %d | Fault Ratio: %.2f\n",
                    result.faults, result.hits, result.faultRatio()));

            area.setText(sb.toString());
            area.setCaretPosition(0);
            return new JScrollPane(area);
        }

        private void clearResults() {
            resultTabs.removeAll();
            resultTabs.add("Welcome", createWelcomePanel());
            totalRequestsValue.setText("-");
            bestAlgoValue.setText("-");
            bestFaultsValue.setText("-");
        }

        private void runSimulation() {
            try {
                int frames = Integer.parseInt(frameInput.getText().trim());
                if (frames <= 0) {
                    showError("Frames must be greater than 0.");
                    return;
                }

                int[] pages = parseReferenceString(refsInput.getText());
                if (pages.length == 0) {
                    showError("Reference string cannot be empty.");
                    return;
                }

                List<SimulationResult> selected = new ArrayList<>();
                if (fifoCheck.isSelected()) {
                    selected.add(simulateFIFO(pages, frames));
                }
                if (lruCheck.isSelected()) {
                    selected.add(simulateLRU(pages, frames));
                }
                if (optimalCheck.isSelected()) {
                    selected.add(simulateOptimal(pages, frames));
                }

                if (selected.isEmpty()) {
                    showError("Select at least one algorithm.");
                    return;
                }

                renderResults(selected, frames, pages.length);
            } catch (NumberFormatException ex) {
                showError("Frames and references must be valid integers.");
            }
        }

        private int[] parseReferenceString(String input) {
            String trimmed = input == null ? "" : input.trim();
            if (trimmed.isEmpty()) {
                return new int[0];
            }

            String[] tokens = SPLIT_PATTERN.split(trimmed);
            int[] pages = new int[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                pages[i] = Integer.parseInt(tokens[i]);
            }
            return pages;
        }

        private void renderResults(List<SimulationResult> results, int frameCount, int totalRequests) {
            resultTabs.removeAll();

            SimulationResult best = null;
            for (SimulationResult r : results) {
                if (best == null || r.faults < best.faults) {
                    best = r;
                }
                resultTabs.add(r.algorithm, createResultPanel(r, frameCount));
            }

            totalRequestsValue.setText(String.valueOf(totalRequests));
            if (best != null) {
                bestAlgoValue.setText(best.algorithm);
                bestFaultsValue.setText(String.valueOf(best.faults));
            }
        }

        private void showError(String message) {
            JOptionPane.showMessageDialog(this, message, "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            SimulatorUI ui = new SimulatorUI();
            ui.setVisible(true);
        });
    }
}