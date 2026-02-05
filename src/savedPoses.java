import Developer.Pose;

public class savedPoses {
    public static final Pose BlueGoalStartPose = new Pose(22, 122, Math.toRadians(135));
    public static final Pose BlueNearParkPose = new Pose(50, 130, Math.toRadians(270));
    public static final Pose BlueFarStartPose = new Pose(44, 8, Math.toRadians(90)); // Left corner of small triangle
    public static final Pose BlueMidShootPose = new Pose(57, 83, Math.toRadians(133));// Blue goal scoring pose
    public static final Pose BlueFarShootPose = new Pose(60, 20, Math.toRadians(110));// Blue goal scoring pose from small triangle
    public static final Pose BlueFarParkPose = new Pose(43, 12, Math.toRadians(90)); // Blue Home (park)

    // Spike closest to human
    public static final Pose BlueSpikeAInsidePose = new Pose(48, 31, Math.toRadians(180));
    public static final Pose BlueSpikeAOutsidePose = new Pose(17, 31, Math.toRadians(180));

    // Middle spike
    public static final Pose BlueSpikeBInsidePose = new Pose(48, 57, Math.toRadians(180));;
    public static final Pose BlueSpikeBOutsidePose = new Pose(17, 57, Math.toRadians(180));

    //Farthest Spike
    public static final Pose BlueSpikeCInsidePose = new Pose(48, 81, Math.toRadians(180));
    public static final Pose BlueSpikeCOutsidePose = new Pose(17, 81, Math.toRadians(180));
    //----------------------------------------------------------

    //------------Red-----------------
    public static final Pose RedFarStartPose =  new Pose(100, 8, Math.toRadians(90));
    public static final Pose RedMidShootPose = new Pose(85, 81, Math.toRadians(45));// Red goal scoring pose

    public static final Pose RedFarShootPose =  new Pose(84, 20, Math.toRadians(64)); // Red goal scoring pose from small triangle
    public static final Pose RedFarParkPose = new Pose(101, 12, Math.toRadians(90)); // Red Home (park)

    public static final Pose RedSpikeAInsidePose = new Pose(105, 32, Math.toRadians(0)); // closest to human 23
    public static final Pose RedSpikeAOutsidePose = new Pose(124, 32, Math.toRadians(0));

    public static final Pose RedSpikeBInsidePose = new Pose(105, 57, Math.toRadians(0)); // secount clostest 22
    public static final Pose RedSpikeBOutsidePose = new Pose(124, 57, Math.toRadians(0));

    public static final Pose RedSpikeCInsidePose = new Pose(105, 81, Math.toRadians(0)); // third closest 21
    public static final Pose RedSpikeCOutsidePose = new Pose(124, 81, Math.toRadians(0));

}
