import Developer.Pose;
import Developer.Renderer;
import Developer.Robot;
import Developer.Telemetry;


import java.awt.Color;

public class example {
    private Robot decbot;
    private Renderer renderer;
    public static void main(String[] args) {
        new example().start();
    }
    private void start() {
        decbot = new Robot();
        renderer = new Renderer(decbot);
        decbot.setPose(new Pose(0.0, 0.0, 0.0));
        decbot.setInconsistency(0.05);

        decbot.setPose(new Pose(5,5,0));

        renderer.runOnEDT(() -> {
            renderer.initWindow();
            renderer.startLoop(this::loop, 20);
        });
    }
    private void loop() {
        double dt = 0.02;

        if(decbot.getX() < 50){
            decbot.setMFL(1);
            decbot.setMFR(1);
            decbot.setMBL(1);
            decbot.setMBR(1);
        }
        else {
            decbot.setMFL(0.0);
            decbot.setMFR(0.0);
            decbot.setMBL(0.0);
            decbot.setMBR(0.0);
        }
        Telemetry.getInstance().addLine("Robot X (raw)", decbot.getX());
        Telemetry.getInstance().addLine("Robot Y", decbot.getY());
        Telemetry.getInstance().addLine("Heading", decbot.getHeading());
        Telemetry.getInstance().addLine("mFL", decbot.getMFL());
        Telemetry.getInstance().addLine("mFR", decbot.getMFR());
        Telemetry.getInstance().addLine("mBL", decbot.getMBL());
        Telemetry.getInstance().addLine("mBR", decbot.getMBR());


        decbot.update(dt);
    }

}
