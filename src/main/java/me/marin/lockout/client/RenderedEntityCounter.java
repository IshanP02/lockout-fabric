package me.marin.lockout.client;

/**
 * Tracks the number of entities actually rendered each frame.
 * Used to fix the broken E: counter in 1.21.11 debug HUD.
 */
public final class RenderedEntityCounter {
    private static int rendered;

    public static void reset() {
        rendered = 0;
    }

    public static void increment() {
        rendered++;
    }

    public static int get() {
        return rendered;
    }
}
