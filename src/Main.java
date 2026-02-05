import Developer.Pose;
import Developer.Renderer;
import Developer.Robot;
import Developer.Telemetry;


import java.awt.Color;


public class Main{
    private Robot robot;
    private Renderer renderer;
    private OzPathing pathing;


    private Pose[] poseList;

    // state machine
    private int state = 0;
    private static final long WAIT_MS = 1000L; // pause length
    private long PauseStartTime = 0L;
    private boolean timerGoing = false;

    public static void main(String[] args) {
        new Main().start();
    }

    public void start() {
        robot = new Robot();
        pathing = new OzPathing(robot);
        renderer = new Renderer(robot);

        robot.setInconsistency(0.05);

        poseList = new Pose[] {
                savedPoses.BlueFarStartPose,      // 0
                savedPoses.BlueFarShootPose,      // 1
                savedPoses.BlueSpikeAInsidePose,  // 2
                savedPoses.BlueSpikeAOutsidePose, // 3
                savedPoses.BlueFarShootPose,      // 4
                savedPoses.BlueSpikeBInsidePose,  // 5
                savedPoses.BlueSpikeBOutsidePose, // 6
                savedPoses.BlueFarShootPose,      // 7
                savedPoses.BlueFarParkPose        // 8
        };

        // place robot at the starting pose (index 0)
        robot.setPose(poseList[0]);


        for (Pose p : poseList) {
            renderer.markPose(p, Color.YELLOW);
        }

        renderer.runOnEDT(() -> {
            renderer.initWindow();
            renderer.startLoop(this::loop, 20); // ~50 Hz
        });
    }

    public void loop() {
        double dt = 0.02;

        // state machine
        switch (state) {
            case 0:
                if (poseList.length > 1) {
                    pathing.follow(poseList[0], poseList[1]);
                    Telemetry.getInstance().addLine("State", "Commanded first move 0 - 1");
                    state = 1;
                } else {
                    state = 9;
                }
                break;

            case 1: case 2: case 3: case 4: case 5: case 6: case 7: case 8:
                if (!pathing.isBusy() && !timerGoing) {
                    timerGoing = true;
                    PauseStartTime = System.currentTimeMillis();
                    Telemetry.getInstance().addLine("State", "Arrived at location " + state + " - starting pause");
                }

                // If timer is running and the wait has expired, mark and command next
                if (timerGoing && (System.currentTimeMillis() - PauseStartTime >= WAIT_MS)) {
                    timerGoing = false;
                    PauseStartTime = 0L;

                    if (state < poseList.length) {
                        renderer.markPose(poseList[state], Color.GREEN);
                    }

                    // command next move if any
                    int next = state + 1;
                    if (next < poseList.length) {
                        pathing.follow(poseList[state], poseList[next]);
                        Telemetry.getInstance().addLine("State", "Commanded move " + state + " - " + next);
                        state = next; // now monitor arrival at 'next' in subsequent ticks
                    } else {

                        state = 9;
                    }
                } else if (timerGoing) {
                    long remaining = Math.max(0L, WAIT_MS - (System.currentTimeMillis() - PauseStartTime));
                    Telemetry.getInstance().addLine("PauseRemainingMs", remaining);
                }
                break;

            case 9:
                Telemetry.getInstance().addLine("Am Finished", true);
                // ensure motors are stopped
                robot.setMFL(0.0);
                robot.setMFR(0.0);
                robot.setMBL(0.0);
                robot.setMBR(0.0);
                break;

            default:
                // safety: reset
                state = 9;
                break;
        }

        // run the pathing controller and advance simulation (pathing.update BEFORE robot.update)
        pathing.update();
        robot.update(dt);

        // common telemetry
        Telemetry.getInstance().addLine("Robot X", robot.getX());
        Telemetry.getInstance().addLine("Robot Y", robot.getY());
        Telemetry.getInstance().addLine("Heading", robot.getHeading());
        Pose tp = pathing.getTargetPose();
        if (tp != null) {
            Telemetry.getInstance().addLine("TargetX", tp.x);
            Telemetry.getInstance().addLine("TargetY", tp.y);
        }
        Telemetry.getInstance().addLine("PathingBusy", pathing.isBusy());
        Telemetry.getInstance().removeStale();
    }
}