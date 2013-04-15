package put.roadef;

public class LogUtility {

    private final String loggingFrom;

    public static LogUtility getLogger() {
        StackTraceElement [] s = new RuntimeException().getStackTrace();
        return new LogUtility( s[1].getClassName() );
    }

    private LogUtility( String loggingClassName ) {
        this.loggingFrom = "("+loggingClassName+") ";
    }

    public void log( String message ) {
        System.out.println( loggingFrom + message );
    }
}