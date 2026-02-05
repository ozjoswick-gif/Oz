package Developer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JPanel;


public class FieldPanel extends JPanel {
    private static final int FIELD_UNITS = 144;
    public static final int ROBOT_SIZE_UNITS = 15;
    private static final int SCALE = 4;

    // NEW: for bottom-right origin
    private static final double FIELD_CENTER_X = FIELD_UNITS / 2.0;
    private static final double FIELD_CENTER_Y = FIELD_UNITS / 2.0;
    private final Robot robot;

    private boolean rotateLabels = false;

    // background image (already rotated to correct orientation)
    private BufferedImage fieldImage;

    private List<Pose> poseMarkers = new ArrayList<>();
    private List<Color> markerColors = new ArrayList<>();


    public FieldPanel(Robot robot) {
        this.robot = robot;
        setPreferredSize(new Dimension(FIELD_UNITS * SCALE, FIELD_UNITS * SCALE));

        // optional fallback color while loading or if image fails
        setBackground(Color.BLACK);

        loadAndRotateFieldImage();
    }

    private void loadAndRotateFieldImage() {
        try {
            // load from classpath root
            // if FieldPanel is in a package, still use the leading "/" to search from root
            java.io.InputStream in = getClass().getResourceAsStream("/Developer/FieldImage.png");
            if (in == null) {
                System.err.println("Could not find FieldImage.png on classpath");
                fieldImage = null;
                return;
            }

            BufferedImage original = javax.imageio.ImageIO.read(in);

            int w = original.getWidth();
            int h = original.getHeight();
            BufferedImage rotated = new BufferedImage(h, w, original.getType());

            Graphics2D g2 = rotated.createGraphics();
            java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();
            at.translate(h / 2.0, w / 2.0);
            at.rotate(Math.PI / 2); // 90° clockwise
            at.translate(-w / 2.0, -h / 2.0);
            g2.drawRenderedImage(original, at);
            g2.dispose();

            fieldImage = rotated;
        } catch (Exception e) {
            e.printStackTrace();
            fieldImage = null;
        }
    }


    public void setRotateLabels(boolean rotateLabels) {
        this.rotateLabels = rotateLabels;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        AffineTransform old = g2.getTransform();

        // background first (unchanged)
        if (fieldImage != null) {
            g2.drawImage(fieldImage, 0, 0, getWidth(), getHeight(), null);
        }

        // NEW: Flip Y only - (0,0) becomes bottom-left
        g2.translate(0, FIELD_UNITS * SCALE);
        g2.scale(1, -1);

        // field border
        g2.setColor(Color.GRAY);
        g2.drawRect(0, 0, FIELD_UNITS * SCALE, FIELD_UNITS * SCALE);

        // DRAW POSE MARKERS IN WORLD COORDINATES (before transforming to robot space)
        // This is the important change: draw markers in the flipped world coordinate system
        // so they are not affected by the robot's translate/rotate transform.
        for (int i = 0; i < poseMarkers.size(); i++) {
            Pose p = poseMarkers.get(i);
            Color c = markerColors.get(i);

            g2.setColor(c);
            int mx = (int) Math.round(p.x * SCALE);
            int my = (int) Math.round(p.y * SCALE);
            int markerSize = 8;

            // Optional: small circle
            g2.fillOval(mx - 3, my - 3, 6, 6);
        }

        // robot drawing
        int rSize = ROBOT_SIZE_UNITS * SCALE;
        double heading = robot.getHeading();
        double cx = robot.getX() * SCALE + rSize / 2.0;
        double cy = robot.getY() * SCALE + rSize / 2.0;

        // transform to robot center and rotate
        g2.translate(cx, cy);
        g2.rotate(heading);

        int half = rSize / 2;

        // robot body
        g2.setColor(Color.RED);
        g2.fillRect(-half, -half, rSize, rSize);

        // direction line (front = +X in robot space)
        int lineLen = rSize / 2;
        g2.setColor(Color.BLUE);
        g2.drawLine(0, 0, lineLen, 0);

        // wheel numbers
        String fl = String.format("%.2f", robot.getMFL());
        String fr = String.format("%.2f", robot.getMFR());
        String bl = String.format("%.2f", robot.getMBL());
        String br = String.format("%.2f", robot.getMBR());

        if (rotateLabels) {
            g2.setColor(Color.WHITE);

            // outside
            g2.drawString(fl, -half - 35, -half + 12);
            g2.drawString(fr,  half + 5,  -half + 12);
            g2.drawString(bl, -half - 35,  half - 5);
            g2.drawString(br,  half + 5,   half - 5);

            g2.setTransform(old);
        } else {
            // restore for upright labels and draw relative to axis-aligned box
            g2.setTransform(old);
            g2.setColor(Color.BLACK);
            int rx = (int) (robot.getX() * SCALE);
            int screenY = getHeight() - (int)(robot.getY() * SCALE) - rSize;

            // outside
            g2.drawString(fl, rx - 35,          screenY + 12);
            g2.drawString(fr, rx + rSize + 5,   screenY + 12);
            g2.drawString(bl, rx - 35,          screenY + rSize - 5);
            g2.drawString(br, rx + rSize + 5,   screenY + rSize - 5);

            g2.setTransform(old);
        }
    }
    public void markPose(Pose pose, Color color) {
        poseMarkers.add(pose);
        markerColors.add(color != null ? color : Color.YELLOW);
        repaint();
    }
    public void clearMarkers() {
        poseMarkers.clear();
        markerColors.clear();
        repaint();
    }


}