package Developer;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;

public class Renderer {
    private final Robot robot;
    final FieldPanel fieldPanel; // package-visible for tests; keep private usage through API
    private JFrame frame;
    private Timer timer;

    public Renderer(Robot robot) {
        this.robot = robot;
        this.fieldPanel = new FieldPanel(robot);
    }

    public void initWindow() {
        frame = new JFrame("Robot Simulator");
        frame.add(fieldPanel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // start the continuous loop; calls the provided loopFunction every tick
    public void startLoop(Runnable loopFunction, int delayMs) {
        timer = new Timer(delayMs, e -> {
            loopFunction.run();   // your update logic
            fieldPanel.repaint(); // then redraw
        });
        timer.start();
    }

    public void stopLoop() {
        if (timer != null) {
            timer.stop();
        }
    }

    // convenience to run everything from Main
    public void runOnEDT(Runnable r) {
        SwingUtilities.invokeLater(r);
    }

    // New helper to place a marker on the field
    public void markPose(Pose pose, Color color) {
        fieldPanel.markPose(pose, color);
    }

    // New helper to clear markers
    public void clearMarkers() {
        fieldPanel.clearMarkers();
    }
}