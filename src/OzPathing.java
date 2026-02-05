import Developer.Pose;
import Developer.Robot;
import Developer.Telemetry;

public class OzPathing {
    private Pose startPose; // never ended up using this for this version
    private Pose targetPose;
    private boolean busy = false;
    private final Robot robot;


    private static final double ROBOT_SIZE_UNITS = 15.0; //idk how to use this from robot

    // Translation tuning
    private static final double speedErrorRate = 2.0;      // speed per unit distance (units/sec per unit error)
    private static final double PosTol = 0.5;
    private static final double MaxSpeed = 36.0;
    private static final double MinSpeed = 1.0;
    private static final double SlowRadius = 6.0;

    // Heading (rotation) tuning
    private static final double SpeedHeading = 2.2;
    private static final double MaxRotSpeedR = 6.0;   // rad/sec cap

    // Lateral correction (helps counter wheel inconsistencies / drift)
    private static final double LateralSpeedCorrection = 0.35;         // vy correction per unit lateral error (robot-frame units)

    // Robot constants (must match Robot.java)
    private static final double M = 36.0;  // MOTOR_TO_LINEAR
    private static final double G = 12.0;  // ROTATION_GAIN

    // small deadband for tiny wheel commands
    private static final double MinMotorAmt = 0.005;

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


    public Pose getTargetPose() {
        return targetPose;
    }

    public void update() {  //actualy moves robot
        if (!busy || targetPose == null) {
            zeroMotors();
            return;
        }

        // compute robot center in world coordinates
        double robotCenterX = robot.getX() + ROBOT_SIZE_UNITS/2;
        double robotCenterY = robot.getY() + ROBOT_SIZE_UNITS/2;
        double dx = targetPose.x - robotCenterX; // distance from target
        double dy = targetPose.y - robotCenterY;
        double dist = Math.hypot(dx, dy);

        if (dist <= PosTol) { // stop command
            busy = false;
            zeroMotors();
            return;
        }

        double heading = robot.getHeading();
        double cos = Math.cos(heading);
        double sin = Math.sin(heading);


        double relX = dx * cos + dy * sin;
        double relY = -dx * sin + dy * cos;

        double ux = 0.0, uy = 0.0;
        double mag = Math.hypot(relX, relY);
        if (mag > 1e-8) {
            ux = relX / mag;
            uy = relY / mag;
        }

        // desired translation speed magnitude (distance-proportional)
        double speedCmd = speedErrorRate * dist;
        speedCmd = Math.min(speedCmd, MaxSpeed);

        // When inside slowdown radius, scale but keep a minimum
        if (dist < SlowRadius) {
            double frac = Math.max(0.25, dist / SlowRadius); // floor frac to avoid collapse
            speedCmd = Math.max(MinSpeed, speedErrorRate * dist * frac);
            speedCmd = Math.min(speedCmd, MaxSpeed);
        }

        //had to break out the unit circle for this one if you get a triangle and the hypotonus form the strait line
        double vx = speedCmd * ux; // forward
        double vy = speedCmd * uy; // right

        // later amt robot has strayed off line
        double lateralCorrection = LateralSpeedCorrection * relY;
        vy += lateralCorrection;

        // heading control: rotate toward final heading while moving
        //not going to like this was ai
        double headingErr = normalizeAngle(targetPose.heading - heading);
        double omega = SpeedHeading * headingErr;

        // moderate rotation while translating faster (don't let rotation dominate translation)
        double transMag = Math.hypot(vx, vy);
        double transScale = Math.min(1.0, transMag / MaxSpeed);
        omega *= (1.0 - 0.3 * transScale);

        // clamp angular speed
        if (omega > MaxRotSpeedR) omega = MaxRotSpeedR;
        if (omega < -MaxRotSpeedR) omega = -MaxRotSpeedR;

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

        // makes sure forces are in limits
        mFL = MinCheck(mFL);
        mFR = MinCheck(mFR);
        mBL = MinCheck(mBL);
        mBR = MinCheck(mBR);

        // set motors
        robot.setMFL(mFL);
        robot.setMFR(mFR);
        robot.setMBL(mBL);
        robot.setMBR(mBR);

        Telemetry.getInstance().addLine("DistToTarget", dist); // distance of hypotonues
        Telemetry.getInstance().addLine("HeadingErr", headingErr);
        Telemetry.getInstance().addLine("omega_cmd", omega);
    }

    private double MinCheck(double v) {
        if (Math.abs(v) < MinMotorAmt) return 0.0;
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
    public boolean isBusy() {
        return busy;
    }
}