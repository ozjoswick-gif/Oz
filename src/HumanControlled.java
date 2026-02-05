import Developer.Pose;
import Developer.Renderer;
import Developer.Robot;
import Developer.Telemetry;
import Developer.FieldPanel;
import java.awt.Color;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

public class HumanControlled {
    private Robot decbot;
    private Renderer renderer;

    // key state (updated by KeyEventDispatcher)
    private volatile boolean keyW = false;
    private volatile boolean keyA = false;
    private volatile boolean keyS = false;
    private volatile boolean keyD = false;
    private volatile boolean keyQ = false;
    private volatile boolean keyE = false;

    // tuning
    private static final double MAX_LINEAR_SPEED = 24.0; // units/sec at full WASD
    private static final double MAX_ANGULAR_SPEED = 3.0; // rad/sec at full Q/E

    // Robot constants (match OzPathing / Robot.java)
    private static final double M = 36.0;  // MOTOR_TO_LINEAR
    private static final double G = 12.0;  // ROTATION_GAIN

    private static final double MOTOR_DEADBAND = 0.02;

    public static void main(String[] args) {
        new HumanControlled().start();
    }

    private void start() {
        decbot = new Robot();
        renderer = new Renderer(decbot);

        // start position
        decbot.setPose(new Pose(5.0, 5.0, 0.0));
        decbot.setInconsistency(0.05);

        // mark a visual starting point
        renderer.markPose(new Pose(5.0, 5.0, 0.0), Color.CYAN);

        // register a global key dispatcher to capture WASDEQ presses/releases
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(createKeyDispatcher());

        renderer.runOnEDT(() -> {
            renderer.initWindow();
            // request focus on the UI so key events are delivered (may depend on platform)

            renderer.startLoop(this::loop, 20);
        });
    }

    private KeyEventDispatcher createKeyDispatcher() {
        return new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                int id = e.getID();
                int code = e.getKeyCode();

                boolean pressed = (id == KeyEvent.KEY_PRESSED);

                switch (code) {
                    case KeyEvent.VK_W: keyW = pressed; break;
                    case KeyEvent.VK_A: keyD = pressed; break;
                    case KeyEvent.VK_S: keyS = pressed; break;
                    case KeyEvent.VK_D: keyA = pressed; break;
                    case KeyEvent.VK_Q: keyQ = pressed; break;
                    case KeyEvent.VK_E: keyE = pressed; break;
                    default:
                        // not a key we care about
                        break;
                }
                // return false so other listeners (and text components) still get the event
                return false;
            }
        };
    }

    private void loop() {
        double dt = 0.02;

        // compute desired translation/rotation inputs from key booleans
        double forward = (keyW ? 1.0 : 0.0) - (keyS ? 1.0 : 0.0);   // +X
        double strafe  = (keyD ? 1.0 : 0.0) - (keyA ? 1.0 : 0.0);   // +Y (right)
        double rotIn   = (keyQ ? 1.0 : 0.0) - (keyE ? 1.0 : 0.0);   // + = ccw, - = cw

        // normalize so diagonal input isn't faster
        double mag = Math.abs(forward) + Math.abs(strafe);
        if (mag > 1.0) {
            forward /= mag;
            strafe /= mag;
        }

        // desired velocities in robot-frame units/sec and rad/sec
        double vx = forward * MAX_LINEAR_SPEED; // forward
        double vy = strafe  * MAX_LINEAR_SPEED; // right
        double omega = rotIn * MAX_ANGULAR_SPEED;

        // convert to motor powers using same pseudoinverse mapping used elsewhere:
        // mFL = (vx - vy)/M - omega/G
        // mFR = (vx + vy)/M + omega/G
        // mBL = (vx - vy)/M + omega/G
        // mBR = (vx + vy)/M - omega/G
        double a = vx / M;
        double b = vy / M;
        double c = omega / G;

        double mFL = a - b - c;
        double mFR = a + b + c;
        double mBL = a - b + c;
        double mBR = a + b - c;

        // normalize if any > 1.0
        double max = Math.max(Math.max(Math.abs(mFL), Math.abs(mFR)),
                Math.max(Math.abs(mBL), Math.abs(mBR)));
        if (max > 1.0) {
            mFL /= max;
            mFR /= max;
            mBL /= max;
            mBR /= max;
        }

        // apply small deadband
        mFL = applyDeadband(mFL);
        mFR = applyDeadband(mFR);
        mBL = applyDeadband(mBL);
        mBR = applyDeadband(mBR);

        // set motors
        decbot.setMFL(mFL);
        decbot.setMFR(mFR);
        decbot.setMBL(mBL);
        decbot.setMBR(mBR);

        // advance physics
        decbot.update(dt);

        // telemetry for debugging
        Telemetry.getInstance().addLine("W A S D Q E",
                String.format("W=%b A=%b S=%b D=%b Q=%b E=%b", keyW, keyA, keyS, keyD, keyQ, keyE));
        Telemetry.getInstance().addLine("vx_cmd", vx);
        Telemetry.getInstance().addLine("vy_cmd", vy);
        Telemetry.getInstance().addLine("omega_cmd", omega);
        Telemetry.getInstance().addLine("mFL", mFL);
        Telemetry.getInstance().addLine("mFR", mFR);
        Telemetry.getInstance().addLine("mBL", mBL);
        Telemetry.getInstance().addLine("mBR", mBR);
        Telemetry.getInstance().addLine("Robot X", decbot.getX());
        Telemetry.getInstance().addLine("Robot Y", decbot.getY());
        Telemetry.getInstance().addLine("Heading", decbot.getHeading());
    }

    private double applyDeadband(double v) {
        if (Math.abs(v) < MOTOR_DEADBAND) return 0.0;
        return v;
    }
}