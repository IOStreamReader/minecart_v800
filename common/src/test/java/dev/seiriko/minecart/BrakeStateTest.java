package dev.seiriko.minecart;

/** Dependency-free boundary checks, run by Gradle's brakeStateTest task. */
public final class BrakeStateTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        var state = new BrakeState();
        for (int tick = 1; tick < 32; tick++) check(state.tick(true) == BrakeState.Result.NONE, "Broke before 32 ticks");
        check(state.tick(true) == BrakeState.Result.BREAK, "Must break on tick 32");
        check(state.broken() && state.heat() == 160, "Must be broken and full");
        for (int tick = 1; tick < 60; tick++) check(state.tick(true) == BrakeState.Result.NONE, "Destroyed before 60 broken ticks");
        check(state.tick(true) == BrakeState.Result.DESTROY, "Must destroy on broken tick 60");

        state.restore(true, 160);
        for (int i = 0; i < 20; i++) state.tick(false);
        check(state.heat() == 80, "Must cool half a bar in one second");
        for (int i = 0; i < 20; i++) state.tick(false);
        check(state.heat() == 0 && state.broken(), "Cooling must not repair brakes");
        for (int i = 0; i < 59; i++) state.tick(true);
        state.tick(false);
        for (int i = 0; i < 59; i++) check(state.tick(true) == BrakeState.Result.NONE, "Release must reset forced braking timer");
        check(state.tick(true) == BrakeState.Result.DESTROY, "Reset timer must still expire");

        state.restore(false, 0);
        for (int i = 0; i < 16; i++) state.tick(true);
        for (int i = 0; i < 10; i++) state.tick(false);
        check(state.heat() == 40, "Partial cooling must preserve accumulated heat");
        for (int i = 0; i < 23; i++) check(state.tick(true) == BrakeState.Result.NONE, "Partial heat broke early");
        check(state.tick(true) == BrakeState.Result.BREAK, "Accumulated heat must break brakes");
        for (double maxSpeed : new double[]{0.4, 1.0, 4.0}) {
            double speed = maxSpeed;
            for (int i = 0; i < 100; i++) speed = BrakeState.speedAfterBraking(speed, maxSpeed);
            check(speed < 1e-12, "Full speed must stop in 100 ticks");
            check(BrakeState.speedAfterBraking(0, maxSpeed) == 0, "Brakes must not reverse a stopped cart");
        }
        System.out.println("Brake state boundary checks passed");
    }
}
