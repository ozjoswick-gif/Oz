package Developer;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Telemetry {
    private JFrame frame;
    private JTextArea textArea;
    private List<LineData> lines = new ArrayList<>();
    private static Telemetry instance;

    private static class LineData {
        String label;
        Object value;
        long lastUpdatedMs;

        LineData(String label, Object value) {
            this.label = label;
            this.value = value;
            this.lastUpdatedMs = System.currentTimeMillis();
        }

        void updateValue(Object value) {
            this.value = value;
            this.lastUpdatedMs = System.currentTimeMillis();
        }
    }

    private Telemetry() {
        createWindow();
    }

    public static Telemetry getInstance() {
        if (instance == null) {
            instance = new Telemetry();
        }
        return instance;
    }

    private void createWindow() {
        frame = new JFrame("Telemetry Console");
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        frame.add(scrollPane);
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void addLine(String label, Object value) {
        synchronized (lines) {
            // update existing line if present
            for (LineData ld : lines) {
                if (ld.label.equals(label)) {
                    ld.updateValue(value);
                    updateDisplay();
                    return;
                }
            }
            // otherwise add new one
            lines.add(new LineData(label, value));
        }
        updateDisplay();
    }

    public void removeStale(long idleMs) {
        long now = System.currentTimeMillis();
        synchronized (lines) {
            lines.removeIf(line -> (now - line.lastUpdatedMs) > idleMs);
        }
        updateDisplay();
    }

    public void removeStale() {
        removeStale(2000L);
    }

    public void updateDisplay() {
        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder();
            synchronized (lines) {
                for (LineData line : lines) {
                    String valueStr;
                    if (line.value instanceof Number) {
                        // round numbers to 0.01 precision
                        double val = ((Number) line.value).doubleValue();
                        valueStr = String.format("%.2f", val);
                    } else {
                        valueStr = String.valueOf(line.value);
                    }
                    sb.append(String.format("%-25s: %s%n", line.label, valueStr));
                }
            }
            textArea.setText(sb.toString());
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }
}