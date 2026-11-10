package alpha.ambient;

/** Routine tracing is off in distributed launches; real faults remain in session diagnostics. */
final class AmbientHooksLog {
    static void debug(String message){if(Boolean.getBoolean("alpha.debugRuntime"))System.out.println(message);}
    private AmbientHooksLog(){}
}
