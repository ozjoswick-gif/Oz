package Developer;
import java.util.Random;

public class Robot {
    private double x, y, heading;
    private double mFL, mFR, mBL, mBR;  // motors

    private double vx = 0, vy = 0, omega = 0;

    private static final double MOTOR_TO_LINEAR = 45.0;
    private static final double ROTATION_GAIN = 12.0;
    private double inconsistency = 0.0;
    private final Random rng = new Random();


    private static final double ACCEL_0_TO_FULL = 25.0;
    private static final double DECEL_FULL_TO_0 = 15.0;

    public Robot() {
        this.x = 0;
        this.y = 0;
        this.heading = 0;
    }

    public void update(double dt) {
        double mFL_eff = applyInconsistency(mFL);
        double mFR_eff = applyInconsistency(mFR);
        double mBL_eff = applyInconsistency(mBL);
        double mBR_eff = applyInconsistency(mBR);

        double vx_target = (mFL_eff + mFR_eff + mBL_eff + mBR_eff) / 4.0 * MOTOR_TO_LINEAR;
        double vy_target = (-mFL_eff + mFR_eff - mBL_eff + mBR_eff) / 4.0 * MOTOR_TO_LINEAR;
        double omega_target = (-mFL_eff + mFR_eff + mBL_eff - mBR_eff) / 4.0 * ROTATION_GAIN;

        vx = smoothAsymmetric(vx, vx_target, ACCEL_0_TO_FULL * dt, DECEL_FULL_TO_0 * dt);
        vy = smoothAsymmetric(vy, vy_target, ACCEL_0_TO_FULL * dt, DECEL_FULL_TO_0 * dt);
        omega = smoothAsymmetric(omega, omega_target, 8.0 * dt, 6.0 * dt);

        heading += omega * dt;
        if (heading > Math.PI) heading -= 2 * Math.PI;
        if (heading < -Math.PI) heading += 2 * Math.PI;

        x += (vx * Math.cos(heading) - vy * Math.sin(heading)) * dt;
        y += (vx * Math.sin(heading) + vy * Math.cos(heading)) * dt;
    }


    public void setPosition(double x, double y) { this.x = x; this.y = y; }
    public void setHeading(double heading) { this.heading = heading; }

    // MOTOR SETTERS = INSTANT
    public void setMFL(double mFL) { this.mFL = mFL; }
    public void setMFR(double mFR) { this.mFR = mFR; }
    public void setMBL(double mBL) { this.mBL = mBL; }
    public void setMBR(double mBR) { this.mBR = mBR; }

    // MOTOR GETTERS = INSTANT
    public double getMFL() { return mFL; }
    public double getMFR() { return mFR; }
    public double getMBL() { return mBL; }
    public double getMBR() { return mBR; }

    public double getX() { return x;}
    public double getY() { return y;}
    public double getHeading() { return heading; }

    // ALL INCONSISTENCY FUNCTIONS - UNCHANGED:
    public void setInconsistency(double inconsistency) { this.inconsistency = inconsistency; }

    public void setPose(Pose pose) {
        setPosition(pose.x, pose.y);
        setHeading(pose.heading);
    }


    private double applyInconsistency(double power) {
        if (power == 0.0 || inconsistency == 0.0) return power;
        double noise = (rng.nextDouble() * 2.0 - 1.0) * inconsistency;
        return power + noise;
    }

    // RELATIVE POSITION FUNCTIONS - UNCHANGED:
    public double getRelX(double worldX, double worldY) {
        double dx = worldX - this.x;
        double dy = worldY - this.y;
        return dx * Math.cos(heading) + dy * Math.sin(heading);
    }

    public double getRelY(double worldX, double worldY) {
        double dx = worldX - this.x;
        double dy = worldY - this.y;
        return -dx * Math.sin(heading) + dy * Math.cos(heading);
    }

    // NEW SMOOTHING:
    private double smoothAsymmetric(double current, double target, double accelRate, double decelRate) {
        if (current < target) {
            return current + accelRate * (target - current);
        } else if (current > target) {
            return current + decelRate * (target - current);
        }
        return current;
    }
}
