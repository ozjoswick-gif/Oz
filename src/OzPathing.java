import Developer.Pose;
import Developer.Robot;
import Developer.Telemetry;

public class OzPathing {
    private Pose startPose; // never ended up using this for this version
    private Pose targetPose;
    private boolean busy = false;
    private final Robot robot;

    // Robot footprint (must match FieldPanel.ROBOT_SIZE_UNITS)
    private static final double ROBOT_SIZE_UNITS = 15.0;
    private static final double ROBOT_HALF = ROBOT_SIZE_UNITS * 0.5;

    // Translation tuning
    private static final double KP_TRANSLATION = 2.0;      // speed per unit distance (units/sec per unit error)
    private static final double POSITION_TOLERANCE = 0.5;  // units (stop radius) -- measured at center
    private static final double MAX_LINEAR_SPEED = 36.0;   // units/sec (allow full-power)
    private static final double MIN_APPROACH_SPEED = 1.0;  // floor when near to avoid crawling
    private static final double SLOWDOWN_RADIUS = 6.0;     // begin slowing inside this radius

    // Heading (rotation) tuning
    private static final double KP_HEADING = 2.2;          // rad/sec per rad of heading error
    private static final double MAX_ANGULAR_SPEED = 6.0;   // rad/sec cap

    // Lateral correction (helps counter wheel inconsistencies / drift)
    private static final double KP_LATERAL = 0.35;         // vy correction per unit lateral error (robot-frame units)

    // Robot constants (must match Robot.java)
    private static final double M = 36.0;  // MOTOR_TO_LINEAR
    private static final double G = 12.0;  // ROTATION_GAIN

    // small deadband for tiny wheel commands
    private static final double MOTOR_DEADBAND = 0.005;

    public OzPathing(Robot robot) {
        this.robot = robot;

    }

    public void follow(Pose start, Pose target) {
        if (!busy) {
            this.startPose = start;
            this.targetPose = target;
            this.busy = true;
        } else {
            // allow updating target while running
            this.targetPose = target;
        }
    }

    public boolean isBusy() {
        return busy;
    }

    public Pose getTargetPose() {
        return targetPose;
    }

    public void update() {
        if (!busy || targetPose == null) {
            zeroMotors();
            return;
        }

        // compute robot center in world coordinates
        double robotCenterX = robot.getX() + ROBOT_HALF;
        double robotCenterY = robot.getY() + ROBOT_HALF;

        // world-frame error measured from robot center to the target
        double dx = targetPose.x - robotCenterX;
        double dy = targetPose.y - robotCenterY;
        double dist = Math.hypot(dx, dy);

        // stop condition (center-based)
        if (dist <= POSITION_TOLERANCE) {
            busy = false;
            zeroMotors();
            return;
        }

        // robot-frame vector to the target (forward = +X, right = +Y)
        // compute using robot center and robot heading (same transform as Robot.getRelX/Y)
        double heading = robot.getHeading();
        double cos = Math.cos(heading);
        double sin = Math.sin(heading);

        // relX = dx * cos + dy * sin
        // relY = -dx * sin + dy * cos
        double relX = dx * cos + dy * sin;
        double relY = -dx * sin + dy * cos;

        // direction unit vector (guard against zero)
        double ux = 0.0, uy = 0.0;
        double mag = Math.hypot(relX, relY);
        if (mag > 1e-8) {
            ux = relX / mag;
            uy = relY / mag;
        }

        // desired translation speed magnitude (distance-proportional)
        double speedCmd = KP_TRANSLATION * dist;
        speedCmd = Math.min(speedCmd, MAX_LINEAR_SPEED);

        // When inside slowdown radius, scale but keep a minimum
        if (dist < SLOWDOWN_RADIUS) {
            double frac = Math.max(0.25, dist / SLOWDOWN_RADIUS); // floor frac to avoid collapse
            speedCmd = Math.max(MIN_APPROACH_SPEED, KP_TRANSLATION * dist * frac);
            speedCmd = Math.min(speedCmd, MAX_LINEAR_SPEED);
        }

        // base translation command toward target in robot frame
        double vx = speedCmd * ux; // forward
        double vy = speedCmd * uy; // right

        // lateral feedback correction: push vy so relY is driven toward zero
        double lateralCorrection = KP_LATERAL * relY;
        vy += lateralCorrection;

        // heading control: rotate toward final heading while moving
        double headingErr = normalizeAngle(targetPose.heading - heading);
        double omega = KP_HEADING * headingErr;

        // moderate rotation while translating faster (don't let rotation dominate translation)
        double transMag = Math.hypot(vx, vy);
        double transScale = Math.min(1.0, transMag / MAX_LINEAR_SPEED);
        omega *= (1.0 - 0.3 * transScale);

        // clamp angular speed
        if (omega > MAX_ANGULAR_SPEED) omega = MAX_ANGULAR_SPEED;
        if (omega < -MAX_ANGULAR_SPEED) omega = -MAX_ANGULAR_SPEED;

        // Convert vx, vy, omega (robot-frame) to motor powers using pseudoinverse (minimum-norm)
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

        // Normalize if any magnitude exceeds 1.0
        double max = Math.max(Math.max(Math.abs(mFL), Math.abs(mFR)),
                Math.max(Math.abs(mBL), Math.abs(mBR)));
        if (max > 1.0) {
            mFL /= max;
            mFR /= max;
            mBL /= max;
            mBR /= max;
        }

        // apply small deadband so Robot smoothing can settle to zero when commanded tiny values
        mFL = applyDeadband(mFL);
        mFR = applyDeadband(mFR);
        mBL = applyDeadband(mBL);
        mBR = applyDeadband(mBR);

        // set motors
        robot.setMFL(mFL);
        robot.setMFR(mFR);
        robot.setMBL(mBL);
        robot.setMBR(mBR);

        // telemetry for tuning and diagnosing inconsistencies (include center coords)
        Telemetry.getInstance().addLine("DistToTarget", dist);
        Telemetry.getInstance().addLine("HeadingErr", headingErr);
        Telemetry.getInstance().addLine("omega_cmd", omega);
    }

    private double applyDeadband(double v) {
        if (Math.abs(v) < MOTOR_DEADBAND) return 0.0;
        return v;
    }

    private void zeroMotors() {
        robot.setMFL(0.0);
        robot.setMFR(0.0);
        robot.setMBL(0.0);
        robot.setMBR(0.0);
    }

    // Normalize angle to [-PI, PI)
    private double normalizeAngle(double a) {
        while (a >= Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }
}