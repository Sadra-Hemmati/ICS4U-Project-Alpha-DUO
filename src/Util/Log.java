package Util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Tiny logging helper used for quick debug statements. Keeps the API minimal
 * so it can be used from any package without pulling in external deps.
 */
public final class Log {
    private static final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");

    private Log() {}

    private static String ts() { return fmt.format(new Date()); }

    public static void d(String tag, String msg) {
        System.out.println("[D] " + ts() + " " + tag + " - " + msg);
    }

    public static void i(String tag, String msg) {
        System.out.println("[I] " + ts() + " " + tag + " - " + msg);
    }

    public static void e(String tag, String msg, Throwable t) {
        System.err.println("[E] " + ts() + " " + tag + " - " + msg);
        if (t != null) t.printStackTrace(System.err);
    }
}
