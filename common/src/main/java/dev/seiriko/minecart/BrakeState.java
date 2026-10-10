package dev.seiriko.minecart;

/** Server-tick state machine: 32 ticks to fill, 40 ticks to cool completely. */
public final class BrakeState {
    public static final int MAX_HEAT = 160;
    public static final int FAILURE_TICKS = 60;
    private int heat;
    private boolean broken;
    private int forcedTicks;

    public enum Result { NONE, BREAK, DESTROY }

    public Result tick(boolean pressed) {
        heat = Math.max(0, Math.min(MAX_HEAT, heat + (pressed ? 5 : -4)));
        if (broken) {
            forcedTicks = pressed ? forcedTicks + 1 : 0;
            return forcedTicks >= FAILURE_TICKS ? Result.DESTROY : Result.NONE;
        }
        if (heat >= MAX_HEAT) {
            broken = true;
            forcedTicks = 0;
            return Result.BREAK;
        }
        return Result.NONE;
    }

    public int heat() { return heat; }
    public boolean broken() { return broken; }
    public void restore(boolean broken, int heat) {
        this.broken = broken;
        this.heat = Math.max(0, Math.min(MAX_HEAT, heat));
        this.forcedTicks = 0;
    }

    public static double speedAfterBraking(double speed, double maxSpeed) {
        return Math.max(0, Math.min(speed, maxSpeed) - maxSpeed / 100.0);
    }
}
