import Developer.Pose;
import Developer.Renderer;
import Developer.Robot;

public class blank2 {
    private Robot decbot;
    private Renderer renderer;
    //------------------





    //------------------
    public static void main(String[] args) {
        new blank2().start();
    }
    private void start() {
        decbot = new Robot();
        renderer = new Renderer(decbot);
        decbot.setPose(new Pose(10.0, 0.0, 0.0));
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
