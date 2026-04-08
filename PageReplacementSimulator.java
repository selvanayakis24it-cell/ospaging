import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * Page Replacement Algorithms Simulator (Console + Optional Swing UI).
 *
 * Algorithms:
 *  - FIFO
 *  - LRU
 *  - Optimal
 *
 * Console UI:
 *  - Menu-driven interface
 *  - Neat, aligned table per algorithm
 *  - Page faults highlighted with symbols and optional ANSI colors
 *
 * Swing UI:
 *  - Input fields + buttons per algorithm
 *  - JTable step-by-step display
 *  - Styled (HTML) summary panel + best-algorithm highlight
 *
 * Single-file, ready to run:
 *   javac PageReplacementSimulator.java
 *   java PageReplacementSimulator
 */
public class PageReplacementSimulator {

    // =========================
    // Entry point
    // =========================
    public static void main(String[] args) {
        // If user passes "gui", open Swing UI directly.
        if (args != null && args.length > 0 && "gui".equalsIgnoreCase(args[0])) {
            SwingUtilities.invokeLater(PageReplacementSimulator::launchSwingUI);
            return;
        }

        // Console first (required). Offer GUI launch from menu as well.
        runConsole();
    }

    // =========================
    // Console UI
    // =========================
    private static void runConsole() {
        Scanner sc = new Scanner(System.in);

        ConsoleStyle style = ConsoleStyle.autoDetect();
        int delayMs = 0; // default no delay

        int[] ref = null;
        int framesCount = -1;
        int declaredPages = -1;

        while (true) {
            clearConsoleIfSupported();
            System.out.println("============================================================");
            System.out.println("             PAGE REPLACEMENT SIMULATOR (Java)              ");
            System.out.println("============================================================");
            System.out.println("Current input:");
            System.out.println("  - Pages (declared): " + (declaredPages < 0 ? "(not set)" : declaredPages));
            System.out.println("  - Reference string: " + (ref == null ? "(not set)" : Arrays.toString(ref)));
            System.out.println("  - Frames:           " + (framesCount < 0 ? "(not set)" : framesCount));
            System.out.println("------------------------------------------------------------");
            System.out.println("Menu:");
            System.out.println("  0. Enter / Update Input");
            System.out.println("  1. Run FIFO");
            System.out.println("  2. Run LRU");
            System.out.println("  3. Run Optimal");
            System.out.println("  4. Run All Algorithms (with comparison)");
            System.out.println("  5. Console Options (colors / delay)");
            System.out.println("  6. Launch GUI (Swing)");
            System.out.println("  7. Exit");
            System.out.print("Choose: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "0" -> {
                    InputData data = readInput(sc);
                    declaredPages = data.declaredPages;
                    ref = data.reference;
                    framesCount = data.frames;
                    pause(sc, "Input captured. Press Enter to continue...");
                }
                case "1", "2", "3", "4" -> {
                    if (!isReady(ref, framesCount)) {
                        pause(sc, "Please enter input first (Menu 0). Press Enter...");
                        break;
                    }
                    if ("1".equals(choice)) {
                        SimulationResult r = simulateFIFO(ref, framesCount);
                        printSimulation("FIFO (First In First Out)", r, style, delayMs);
                    } else if ("2".equals(choice)) {
                        SimulationResult r = simulateLRU(ref, framesCount);
                        printSimulation("LRU (Least Recently Used)", r, style, delayMs);
                    } else if ("3".equals(choice)) {
                        SimulationResult r = simulateOptimal(ref, framesCount);
                        printSimulation("Optimal Page Replacement", r, style, delayMs);
                    } else {
                        SimulationResult fifo = simulateFIFO(ref, framesCount);
                        SimulationResult lru = simulateLRU(ref, framesCount);
                        SimulationResult opt = simulateOptimal(ref, framesCount);

                        printSimulation("FIFO (First In First Out)", fifo, style, delayMs);
                        printSimulation("LRU (Least Recently Used)", lru, style, delayMs);
                        printSimulation("Optimal Page Replacement", opt, style, delayMs);

                        printComparisonSummary(fifo, lru, opt, style);
                    }
                    pause(sc, "Press Enter to return to menu...");
                }
                case "5" -> {
                    ConsoleOptions opts = configureConsoleOptions(sc, style, delayMs);
                    style = opts.style;
                    delayMs = opts.delayMs;
                }
                case "6" -> {
                    SwingUtilities.invokeLater(PageReplacementSimulator::launchSwingUI);
                    pause(sc, "GUI launched. Press Enter to return to console menu...");
                }
                case "7" -> {
                    System.out.println("Goodbye.");
                    return;
                }
                default -> pause(sc, "Invalid choice. Press Enter...");
            }
        }
    }

    private static boolean isReady(int[] ref, int framesCount) {
        return ref != null && ref.length > 0 && framesCount > 0;
    }

    private static InputData readInput(Scanner sc) {
        Integer declaredPages = null;
        while (declaredPages == null) {
            System.out.print("Enter number of pages (positive integer): ");
            String s = sc.nextLine().trim();
            Integer v = tryParseInt(s);
            if (v == null || v <= 0) {
                System.out.println("Invalid. Try again.");
            } else {
                declaredPages = v;
            }
        }

        int[] reference = null;
        while (reference == null) {
            System.out.println("Enter reference string (space-separated integers).");
            System.out.print("Example: 7 0 1 2 0 3 0 4 2 ...\n> ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) {
                System.out.println("Reference string cannot be empty.");
                continue;
            }
            int[] parsed = parseIntList(line);
            if (parsed == null || parsed.length == 0) {
                System.out.println("Invalid reference string. Use space-separated integers.");
                continue;
            }
            reference = parsed;
        }

        Integer frames = null;
        while (frames == null) {
            System.out.print("Enter number of frames (positive integer): ");
            String s = sc.nextLine().trim();
            Integer v = tryParseInt(s);
            if (v == null || v <= 0) {
                System.out.println("Invalid. Try again.");
            } else {
                frames = v;
            }
        }

        if (declaredPages != reference.length) {
            System.out.println("Note: Declared pages (" + declaredPages + ") != reference length (" + reference.length + ").");
            System.out.println("      The simulator will use the reference string length (" + reference.length + ").");
        }

        return new InputData(declaredPages, reference, frames);
    }

    private static ConsoleOptions configureConsoleOptions(Scanner sc, ConsoleStyle currentStyle, int currentDelayMs) {
        while (true) {
            clearConsoleIfSupported();
            System.out.println("============================================================");
            System.out.println("                    CONSOLE OPTIONS                         ");
            System.out.println("============================================================");
            System.out.println("1. Toggle colors (currently: " + (currentStyle.useAnsiColors ? "ON" : "OFF") + ")");
            System.out.println("2. Set delay per step (currently: " + currentDelayMs + " ms)");
            System.out.println("3. Back");
            System.out.print("Choose: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> currentStyle = currentStyle.withColors(!currentStyle.useAnsiColors);
                case "2" -> {
                    System.out.print("Enter delay in ms (0..2000 recommended): ");
                    String s = sc.nextLine().trim();
                    Integer v = tryParseInt(s);
                    if (v == null || v < 0) {
                        System.out.println("Invalid delay.");
                        pause(sc, "Press Enter...");
                    } else {
                        currentDelayMs = v;
                    }
                }
                case "3" -> {
                    return new ConsoleOptions(currentStyle, currentDelayMs);
                }
                default -> pause(sc, "Invalid choice. Press Enter...");
            }
        }
    }

    private static void printSimulation(String title, SimulationResult result, ConsoleStyle style, int delayMs) {
        clearConsoleIfSupported();
        System.out.println("============================================================");
        System.out.println(" " + title);
        System.out.println("============================================================");

        // Compute column widths for neat alignment
        int frames = result.framesCount;

        // Determine widths
        int stepW = Math.max(4, String.valueOf(result.rows.size()).length());
        int pageW = Math.max(4, maxWidth(result, Cell.PAGE));
        int frameW = Math.max(6, maxWidth(result, Cell.FRAME)); // frames display often small
        // Widened so it stays aligned even with symbols / ANSI colors.
        int faultW = 10; // e.g., "Yes ✘ (F)"

        // Build and print header row
        String sep = buildSeparator(stepW, pageW, frameW, frames, faultW);
        System.out.println(sep);
        System.out.print("| " + pad("Step", stepW) + " | ");
        System.out.print(pad("Page", pageW) + " | ");
        for (int i = 1; i <= frames; i++) {
            System.out.print(pad("Frame" + i, frameW) + " | ");
        }
        System.out.println(pad("Fault", faultW) + " |");
        System.out.println(sep);

        // Print each step
        for (StepRow row : result.rows) {
            // Symbols make the output feel like a simulator; the (H)/(F) keeps it readable if glyphs don't render.
            String faultSymbol = row.pageFault ? "✘" : "✔";
            String faultText = row.pageFault ? "Yes " + faultSymbol + " (F)" : "No  " + faultSymbol + " (H)";

            String coloredFaultText = faultText;
            if (style.useAnsiColors) {
                coloredFaultText = row.pageFault
                        ? style.red(faultText)
                        : style.green(faultText);
            }

            System.out.print("| " + pad(String.valueOf(row.step), stepW) + " | ");
            System.out.print(pad(String.valueOf(row.currentPage), pageW) + " | ");
            for (int i = 0; i < frames; i++) {
                String cell = row.framesSnapshot[i] == -1 ? "-" : String.valueOf(row.framesSnapshot[i]);
                System.out.print(pad(cell, frameW) + " | ");
            }
            System.out.println(pad(coloredFaultText, faultW) + " |");

            if (delayMs > 0) sleep(delayMs);
        }

        System.out.println(sep);
        String pfLine = "Total Page Faults: " + result.pageFaults;
        System.out.println(style.useAnsiColors ? style.bold(pfLine) : pfLine);
        System.out.println(sep);
    }

    private static void printComparisonSummary(SimulationResult fifo, SimulationResult lru, SimulationResult opt, ConsoleStyle style) {
        System.out.println();
        System.out.println("============================================================");
        System.out.println("                 COMPARISON SUMMARY                         ");
        System.out.println("============================================================");

        int min = Math.min(fifo.pageFaults, Math.min(lru.pageFaults, opt.pageFaults));
        String bestName = (fifo.pageFaults == min) ? "FIFO"
                : (lru.pageFaults == min) ? "LRU"
                : "Optimal";

        String lineSep = "------------------------------------------------------------";
        System.out.println(lineSep);
        System.out.println(formatCompareLine("FIFO   faults", fifo.pageFaults, fifo.pageFaults == min, style));
        System.out.println(formatCompareLine("LRU    faults", lru.pageFaults, lru.pageFaults == min, style));
        System.out.println(formatCompareLine("Optimal faults", opt.pageFaults, opt.pageFaults == min, style));
        System.out.println(lineSep);

        String best = "Best (minimum faults): " + bestName + " (" + min + ")";
        System.out.println(style.useAnsiColors ? style.cyan(style.bold(best)) : best);
        System.out.println("============================================================");
    }

    private static String formatCompareLine(String label, int faults, boolean best, ConsoleStyle style) {
        String text = String.format("%-14s : %d%s", label, faults, best ? "  <-- best" : "");
        if (!style.useAnsiColors) return text;
        return best ? style.green(style.bold(text)) : text;
    }

    private static int maxWidth(SimulationResult result, Cell cellType) {
        int max = 0;
        for (StepRow r : result.rows) {
            if (cellType == Cell.PAGE) {
                max = Math.max(max, String.valueOf(r.currentPage).length());
            } else {
                for (int v : r.framesSnapshot) {
                    String s = (v == -1) ? "-" : String.valueOf(v);
                    max = Math.max(max, s.length());
                }
            }
        }
        return max;
    }

    private static String buildSeparator(int stepW, int pageW, int frameW, int frames, int faultW) {
        StringBuilder sb = new StringBuilder();
        sb.append("+").append(repeat("-", stepW + 2));
        sb.append("+").append(repeat("-", pageW + 2));
        for (int i = 0; i < frames; i++) {
            sb.append("+").append(repeat("-", frameW + 2));
        }
        sb.append("+").append(repeat("-", faultW + 2)).append("+");
        return sb.toString();
    }

    private static String pad(String s, int width) {
        // Strip ANSI for width calc, but keep original string.
        String plain = stripAnsi(s);
        int pad = Math.max(0, width - plain.length());
        return s + repeat(" ", pad);
    }

    private static String stripAnsi(String s) {
        return s.replaceAll("\\u001B\\[[;\\d]*m", "");
    }

    private static String repeat(String s, int n) {
        return s.repeat(Math.max(0, n));
    }

    private static void pause(Scanner sc, String message) {
        System.out.print(message);
        sc.nextLine();
    }

    private static void clearConsoleIfSupported() {
        // Best-effort: do not crash if unsupported.
        // On many terminals, printing newlines is acceptable.
        System.out.print("\n".repeat(2));
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    // =========================
    // Algorithms (OOP-ish: separate methods)
    // =========================

    /**
     * FIFO uses a queue to evict the oldest loaded page.
     */
    public static SimulationResult simulateFIFO(int[] reference, int framesCount) {
        int[] frames = new int[framesCount];
        Arrays.fill(frames, -1);

        Set<Integer> inFrames = new HashSet<>();
        Queue<Integer> queue = new ArrayDeque<>(); // maintains insertion order of pages in frames

        List<StepRow> rows = new ArrayList<>();
        int faults = 0;

        for (int i = 0; i < reference.length; i++) {
            int page = reference[i];
            boolean fault;

            if (inFrames.contains(page)) {
                fault = false; // hit
            } else {
                fault = true;
                faults++;

                // If there is an empty frame, place it there.
                int emptyIndex = indexOf(frames, -1);
                if (emptyIndex != -1) {
                    frames[emptyIndex] = page;
                    inFrames.add(page);
                    queue.add(page);
                } else {
                    // Evict the oldest page (front of queue)
                    int victim = queue.remove();
                    int victimIndex = indexOf(frames, victim);
                    frames[victimIndex] = page;
                    inFrames.remove(victim);
                    inFrames.add(page);
                    queue.add(page);
                }
            }

            rows.add(StepRow.of(i + 1, page, frames, fault));
        }

        return new SimulationResult("FIFO", framesCount, faults, rows);
    }

    /**
     * LRU tracks last-used index for each page; evicts the least recently used.
     */
    public static SimulationResult simulateLRU(int[] reference, int framesCount) {
        int[] frames = new int[framesCount];
        Arrays.fill(frames, -1);

        Set<Integer> inFrames = new HashSet<>();
        Map<Integer, Integer> lastUsedAt = new HashMap<>(); // page -> last index used

        List<StepRow> rows = new ArrayList<>();
        int faults = 0;

        for (int i = 0; i < reference.length; i++) {
            int page = reference[i];
            boolean fault;

            if (inFrames.contains(page)) {
                fault = false;
            } else {
                fault = true;
                faults++;

                int emptyIndex = indexOf(frames, -1);
                if (emptyIndex != -1) {
                    frames[emptyIndex] = page;
                    inFrames.add(page);
                } else {
                    // Choose victim with smallest lastUsedAt (least recently used)
                    int victim = -1;
                    int bestLastUse = Integer.MAX_VALUE;
                    for (int p : inFrames) {
                        int lu = lastUsedAt.getOrDefault(p, -1);
                        if (lu < bestLastUse) {
                            bestLastUse = lu;
                            victim = p;
                        }
                    }

                    int victimIndex = indexOf(frames, victim);
                    frames[victimIndex] = page;
                    inFrames.remove(victim);
                    inFrames.add(page);
                    lastUsedAt.remove(victim);
                }
            }

            lastUsedAt.put(page, i);
            rows.add(StepRow.of(i + 1, page, frames, fault));
        }

        return new SimulationResult("LRU", framesCount, faults, rows);
    }

    /**
     * Optimal looks ahead in the reference string and evicts the page whose next use is farthest
     * in the future (or not used again).
     */
    public static SimulationResult simulateOptimal(int[] reference, int framesCount) {
        int[] frames = new int[framesCount];
        Arrays.fill(frames, -1);

        Set<Integer> inFrames = new HashSet<>();
        List<StepRow> rows = new ArrayList<>();
        int faults = 0;

        for (int i = 0; i < reference.length; i++) {
            int page = reference[i];
            boolean fault;

            if (inFrames.contains(page)) {
                fault = false;
            } else {
                fault = true;
                faults++;

                int emptyIndex = indexOf(frames, -1);
                if (emptyIndex != -1) {
                    frames[emptyIndex] = page;
                    inFrames.add(page);
                } else {
                    // Compute next use for each page currently in frames
                    int victim = chooseOptimalVictim(frames, reference, i + 1);
                    int victimIndex = indexOf(frames, victim);
                    frames[victimIndex] = page;
                    inFrames.remove(victim);
                    inFrames.add(page);
                }
            }

            rows.add(StepRow.of(i + 1, page, frames, fault));
        }

        return new SimulationResult("Optimal", framesCount, faults, rows);
    }

    /**
     * Victim is the page with the farthest next reference. If a page is never referenced again,
     * it's the best victim immediately.
     */
    private static int chooseOptimalVictim(int[] frames, int[] reference, int startSearchIndex) {
        int victim = frames[0];
        int farthest = -1;

        for (int p : frames) {
            int next = nextIndexOf(reference, p, startSearchIndex);
            if (next == -1) {
                // Not used again -> evict immediately
                return p;
            }
            if (next > farthest) {
                farthest = next;
                victim = p;
            }
        }
        return victim;
    }

    private static int nextIndexOf(int[] arr, int value, int start) {
        for (int i = start; i < arr.length; i++) {
            if (arr[i] == value) return i;
        }
        return -1;
    }

    private static int indexOf(int[] arr, int value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == value) return i;
        }
        return -1;
    }

    // =========================
    // Parsing helpers
    // =========================
    private static Integer tryParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static int[] parseIntList(String line) {
        String[] parts = line.trim().split("\\s+");
        int[] out = new int[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        } catch (Exception e) {
            return null;
        }
        return out;
    }

    // =========================
    // Data models
    // =========================
    private enum Cell { PAGE, FRAME }

    private record InputData(int declaredPages, int[] reference, int frames) { }

    private record ConsoleOptions(ConsoleStyle style, int delayMs) { }

    public static final class SimulationResult {
        public final String algorithmName;
        public final int framesCount;
        public final int pageFaults;
        public final List<StepRow> rows;

        public SimulationResult(String algorithmName, int framesCount, int pageFaults, List<StepRow> rows) {
            this.algorithmName = algorithmName;
            this.framesCount = framesCount;
            this.pageFaults = pageFaults;
            this.rows = rows;
        }
    }

    public static final class StepRow {
        public final int step;
        public final int currentPage;
        public final int[] framesSnapshot;
        public final boolean pageFault;

        private StepRow(int step, int currentPage, int[] framesSnapshot, boolean pageFault) {
            this.step = step;
            this.currentPage = currentPage;
            this.framesSnapshot = framesSnapshot;
            this.pageFault = pageFault;
        }

        public static StepRow of(int step, int currentPage, int[] frames, boolean pageFault) {
            return new StepRow(step, currentPage, Arrays.copyOf(frames, frames.length), pageFault);
        }
    }

    // =========================
    // Console styling (ANSI)
    // =========================
    public static final class ConsoleStyle {
        public final boolean useAnsiColors;

        private ConsoleStyle(boolean useAnsiColors) {
            this.useAnsiColors = useAnsiColors;
        }

        public static ConsoleStyle autoDetect() {
            // Windows Terminal / modern consoles often support ANSI, but not guaranteed.
            // We'll default to OFF for safety; user can toggle ON.
            return new ConsoleStyle(false);
        }

        public ConsoleStyle withColors(boolean enabled) {
            return new ConsoleStyle(enabled);
        }

        public String red(String s) { return "\u001B[31m" + s + "\u001B[0m"; }
        public String green(String s) { return "\u001B[32m" + s + "\u001B[0m"; }
        public String cyan(String s) { return "\u001B[36m" + s + "\u001B[0m"; }
        public String bold(String s) { return "\u001B[1m" + s + "\u001B[0m"; }
    }

    // =========================
    // Swing UI (Optional "HTML/CSS-like" attractive output)
    // =========================
    private static void launchSwingUI() {
        JFrame frame = new JFrame("Page Replacement Simulator");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setSize(1100, 700);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        root.setBackground(new Color(245, 247, 250));

        // Header
        JLabel header = new JLabel("<html><div style='font-family:Segoe UI,Arial; font-size:18px;'>" +
                "<b>Page Replacement Algorithms Simulator</b> " +
                "<span style='color:#6b7280'>(FIFO / LRU / Optimal)</span></div></html>");
        root.add(header, BorderLayout.NORTH);

        // Left: inputs + buttons
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 232)),
                new EmptyBorder(12, 12, 12, 12)
        ));
        left.setBackground(Color.WHITE);

        JTextField pagesField = new JTextField();
        JTextArea refArea = new JTextArea(6, 22);
        JTextField framesField = new JTextField();

        pagesField.setToolTipText("Optional: number of pages (will be validated against reference length)");
        refArea.setLineWrap(true);
        refArea.setWrapStyleWord(true);
        refArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        framesField.setToolTipText("Number of frames, e.g., 3");

        left.add(label("Number of pages (optional)"));
        left.add(pagesField);
        left.add(Box.createVerticalStrut(10));
        left.add(label("Reference string (space-separated integers)"));
        left.add(new JScrollPane(refArea));
        left.add(Box.createVerticalStrut(10));
        left.add(label("Number of frames"));
        left.add(framesField);
        left.add(Box.createVerticalStrut(12));

        JCheckBox delayCheck = new JCheckBox("Simulation delay (150ms/step)");
        delayCheck.setBackground(Color.WHITE);
        JCheckBox colorCheck = new JCheckBox("Color hits/faults");
        colorCheck.setBackground(Color.WHITE);
        colorCheck.setSelected(true);
        left.add(delayCheck);
        left.add(colorCheck);
        left.add(Box.createVerticalStrut(12));

        JButton fifoBtn = new JButton("Run FIFO");
        JButton lruBtn = new JButton("Run LRU");
        JButton optBtn = new JButton("Run Optimal");
        JButton allBtn = new JButton("Run All + Compare");

        styleButton(fifoBtn, new Color(37, 99, 235));
        styleButton(lruBtn, new Color(16, 185, 129));
        styleButton(optBtn, new Color(245, 158, 11));
        styleButton(allBtn, new Color(99, 102, 241));

        left.add(fifoBtn);
        left.add(Box.createVerticalStrut(8));
        left.add(lruBtn);
        left.add(Box.createVerticalStrut(8));
        left.add(optBtn);
        left.add(Box.createVerticalStrut(8));
        left.add(allBtn);

        // Right: table + summary
        JPanel right = new JPanel(new BorderLayout(12, 12));
        right.setOpaque(false);

        SimulationTableModel tableModel = new SimulationTableModel();
        JTable table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 232)));
        right.add(tableScroll, BorderLayout.CENTER);

        JEditorPane summary = new JEditorPane("text/html", "");
        summary.setEditable(false);
        summary.setOpaque(true);
        summary.setBackground(Color.WHITE);
        summary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 232)),
                new EmptyBorder(10, 10, 10, 10)
        ));
        right.add(summary, BorderLayout.SOUTH);

        // Split pane
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.30);
        split.setBorder(null);
        root.add(split, BorderLayout.CENTER);

        // Renderer for fault coloring
        table.setDefaultRenderer(Object.class, new FaultAwareRenderer(tableModel, colorCheck));

        // Actions
        Action runFifo = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                ParsedInput in = parseSwingInput(pagesField, refArea, framesField, frame);
                if (in == null) return;
                SimulationResult r = simulateFIFO(in.reference, in.frames);
                showInSwing(tableModel, summary, "FIFO", r, null, null, delayCheck.isSelected());
            }
        };
        Action runLru = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                ParsedInput in = parseSwingInput(pagesField, refArea, framesField, frame);
                if (in == null) return;
                SimulationResult r = simulateLRU(in.reference, in.frames);
                showInSwing(tableModel, summary, "LRU", r, null, null, delayCheck.isSelected());
            }
        };
        Action runOpt = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                ParsedInput in = parseSwingInput(pagesField, refArea, framesField, frame);
                if (in == null) return;
                SimulationResult r = simulateOptimal(in.reference, in.frames);
                showInSwing(tableModel, summary, "Optimal", r, null, null, delayCheck.isSelected());
            }
        };
        Action runAll = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                ParsedInput in = parseSwingInput(pagesField, refArea, framesField, frame);
                if (in == null) return;
                SimulationResult fifo = simulateFIFO(in.reference, in.frames);
                SimulationResult lru = simulateLRU(in.reference, in.frames);
                SimulationResult opt = simulateOptimal(in.reference, in.frames);
                showInSwing(tableModel, summary, "All (showing Optimal table)", opt, fifo, lru, delayCheck.isSelected());
            }
        };

        fifoBtn.setAction(runFifo);
        fifoBtn.setText("Run FIFO");
        lruBtn.setAction(runLru);
        lruBtn.setText("Run LRU");
        optBtn.setAction(runOpt);
        optBtn.setText("Run Optimal");
        allBtn.setAction(runAll);
        allBtn.setText("Run All + Compare");

        // Prefill with the sample from prompt to make it easy to try
        pagesField.setText("20");
        refArea.setText("7 0 1 2 0 3 0 4 2 3 0 3 2 1 2 0 1 7 0 1");
        framesField.setText("3");
        summary.setText(htmlSummary("Ready", null, null, null));

        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel("<html><span style='font-family:Segoe UI,Arial; color:#111827;'><b>" + escapeHtml(text) + "</b></span></html>");
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static void styleButton(JButton b, Color bg) {
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
    }

    private record ParsedInput(int[] reference, int frames) { }

    private static ParsedInput parseSwingInput(JTextField pagesField, JTextArea refArea, JTextField framesField, Component parent) {
        String refLine = refArea.getText().trim();
        if (refLine.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Reference string cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        int[] ref = parseIntList(refLine);
        if (ref == null || ref.length == 0) {
            JOptionPane.showMessageDialog(parent, "Invalid reference string. Use space-separated integers.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        Integer frames = tryParseInt(framesField.getText().trim());
        if (frames == null || frames <= 0) {
            JOptionPane.showMessageDialog(parent, "Frames must be a positive integer.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        String pagesText = pagesField.getText().trim();
        if (!pagesText.isEmpty()) {
            Integer declared = tryParseInt(pagesText);
            if (declared == null || declared <= 0) {
                JOptionPane.showMessageDialog(parent, "Number of pages must be a positive integer (or leave blank).", "Input Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            if (declared != ref.length) {
                JOptionPane.showMessageDialog(parent,
                        "Note: Declared pages (" + declared + ") != reference length (" + ref.length + ").\nUsing reference length.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        return new ParsedInput(ref, frames);
    }

    private static void showInSwing(SimulationTableModel model,
                                   JEditorPane summary,
                                   String title,
                                   SimulationResult shown,
                                   SimulationResult fifo,
                                   SimulationResult lru,
                                   boolean withDelay) {
        // Populate table columns based on frames count.
        model.setData(shown);

        if (!withDelay) {
            model.setRevealUpTo(shown.rows.size());
            summary.setText(buildSwingSummary(title, shown, fifo, lru));
            return;
        }

        // Animate: reveal rows progressively without blocking the EDT too long.
        model.setRevealUpTo(0);
        summary.setText(buildSwingSummary(title, shown, fifo, lru));

        javax.swing.Timer t = new javax.swing.Timer(150, null);
        t.addActionListener(ev -> {
            int next = model.getRevealUpTo() + 1;
            model.setRevealUpTo(next);
            if (next >= shown.rows.size()) {
                t.stop();
            }
        });
        t.start();
    }

    private static String buildSwingSummary(String title, SimulationResult shown, SimulationResult fifo, SimulationResult lru) {
        SimulationResult opt = shown.algorithmName.equalsIgnoreCase("Optimal") ? shown : null;
        // When "Run All" we pass shown=opt, fifo and lru are non-null. For single-alg runs, fifo/lru null.
        if (fifo == null && lru == null) {
            return htmlSummary(title, shown, null, null);
        }
        // For Run All: shown is optimal table by default; compute best.
        SimulationResult optimal = opt != null ? opt : shown;
        return htmlSummary("Comparison", fifo, lru, optimal);
    }

    private static String htmlSummary(String title, SimulationResult fifo, SimulationResult lru, SimulationResult opt) {
        String baseStyle = """
                <style>
                  body { font-family: Segoe UI, Arial; color: #111827; }
                  .card { border: 1px solid #e5e7eb; border-radius: 10px; padding: 12px; }
                  .title { font-size: 14px; font-weight: 700; margin-bottom: 8px; }
                  .row { display: flex; gap: 10px; margin: 6px 0; }
                  .pill { display:inline-block; padding: 3px 8px; border-radius: 999px; font-size: 12px; }
                  .fifo { background:#dbeafe; color:#1d4ed8; }
                  .lru  { background:#d1fae5; color:#065f46; }
                  .opt  { background:#fef3c7; color:#92400e; }
                  .best { background:#111827; color:#ffffff; }
                  .muted{ color:#6b7280; font-size: 12px; }
                </style>
                """;

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head>").append(baseStyle).append("</head><body>");
        sb.append("<div class='card'>");
        sb.append("<div class='title'>").append(escapeHtml(title)).append("</div>");

        if (fifo == null && lru == null && opt == null) {
            sb.append("<div class='muted'>Enter input and run an algorithm to see results.</div>");
        } else if (lru == null && opt == null && fifo != null && "FIFO".equalsIgnoreCase(fifo.algorithmName)) {
            sb.append("<div class='row'><span class='pill fifo'>FIFO faults: ").append(fifo.pageFaults).append("</span></div>");
        } else if (fifo != null && lru == null && opt == null && "LRU".equalsIgnoreCase(fifo.algorithmName)) {
            sb.append("<div class='row'><span class='pill lru'>LRU faults: ").append(fifo.pageFaults).append("</span></div>");
        } else if (fifo != null && lru == null && opt == null && "Optimal".equalsIgnoreCase(fifo.algorithmName)) {
            sb.append("<div class='row'><span class='pill opt'>Optimal faults: ").append(fifo.pageFaults).append("</span></div>");
        } else {
            int f = fifo.pageFaults;
            int l = lru.pageFaults;
            int o = opt.pageFaults;
            int min = Math.min(f, Math.min(l, o));
            String best = (f == min) ? "FIFO" : (l == min) ? "LRU" : "Optimal";

            sb.append("<div class='row'>")
                    .append("<span class='pill fifo'>FIFO: ").append(f).append("</span>")
                    .append("<span class='pill lru'>LRU: ").append(l).append("</span>")
                    .append("<span class='pill opt'>Optimal: ").append(o).append("</span>")
                    .append("</div>");
            sb.append("<div class='row'><span class='pill best'>Best: ").append(best).append(" (").append(min).append(")</span></div>");
            sb.append("<div class='muted'>Table shows step-by-step for: ").append(escapeHtml(opt.algorithmName)).append("</div>");
        }

        sb.append("</div></body></html>");
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // =========================
    // JTable model + renderer
    // =========================
    private static final class SimulationTableModel extends AbstractTableModel {
        private SimulationResult data;
        private String[] columns = new String[]{"Step", "Page", "Fault"};
        private Object[][] rows = new Object[0][0];
        private int revealUpTo = Integer.MAX_VALUE; // for animated reveal

        public void setData(SimulationResult result) {
            this.data = result;
            int frames = result.framesCount;

            columns = new String[2 + frames + 1]; // Step, Page, Frame1..n, Fault
            columns[0] = "Step";
            columns[1] = "Page";
            for (int i = 0; i < frames; i++) columns[2 + i] = "Frame" + (i + 1);
            columns[columns.length - 1] = "Fault";

            rows = new Object[result.rows.size()][columns.length];
            for (int r = 0; r < result.rows.size(); r++) {
                StepRow sr = result.rows.get(r);
                rows[r][0] = sr.step;
                rows[r][1] = sr.currentPage;
                for (int i = 0; i < frames; i++) {
                    int v = sr.framesSnapshot[i];
                    rows[r][2 + i] = (v == -1) ? "-" : v;
                }
                rows[r][columns.length - 1] = sr.pageFault ? "Yes ✘" : "No ✔";
            }

            revealUpTo = rows.length;
            fireTableStructureChanged();
        }

        public boolean isFaultRow(int viewRow) {
            if (data == null) return false;
            if (viewRow < 0 || viewRow >= data.rows.size()) return false;
            return data.rows.get(viewRow).pageFault;
        }

        public int getRevealUpTo() {
            return revealUpTo;
        }

        public void setRevealUpTo(int revealUpTo) {
            this.revealUpTo = Math.max(0, Math.min(revealUpTo, rows.length));
            fireTableDataChanged();
        }

        @Override public int getRowCount() {
            return Math.min(rows.length, revealUpTo);
        }

        @Override public int getColumnCount() {
            return columns.length;
        }

        @Override public String getColumnName(int column) {
            return columns[column];
        }

        @Override public Object getValueAt(int rowIndex, int columnIndex) {
            return rows[rowIndex][columnIndex];
        }

        @Override public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    private static final class FaultAwareRenderer extends DefaultTableCellRenderer {
        private final SimulationTableModel model;
        private final JCheckBox colorToggle;

        FaultAwareRenderer(SimulationTableModel model, JCheckBox colorToggle) {
            this.model = model;
            this.colorToggle = colorToggle;
            setHorizontalAlignment(LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            boolean fault = model.isFaultRow(row);

            if (isSelected) {
                c.setBackground(new Color(229, 231, 235));
                c.setForeground(new Color(17, 24, 39));
                return c;
            }

            c.setForeground(new Color(17, 24, 39));
            c.setBackground(Color.WHITE);

            if (colorToggle.isSelected()) {
                if (fault) {
                    c.setBackground(new Color(254, 226, 226)); // light red
                } else {
                    c.setBackground(new Color(220, 252, 231)); // light green
                }
            }
            return c;
        }
    }
}

