import Developer.Pose;
import Developer.Renderer;
import Developer.Robot;
import Developer.Telemetry;

public class blank {
    private Robot decbot;
    private Renderer renderer;
    //------------------





    //------------------
    public static void main(String[] args) {
        new blank().start();
    }
    private void start() {
        decbot = new Robot();
        renderer = new Renderer(decbot);
        decbot.setPose(new Pose(0.0, 0.0, 0.0));
        decbot.setInconsistency(0.05);
        //------------------



        //------------------
        renderer.runOnEDT(() -> {
            renderer.initWindow();
            renderer.startLoop(this::loop, 20);
        });
    }
    private void loop() {
        double dt = 0.02;
        //------------------

        //------------------
        decbot.update(dt);
    }

}
