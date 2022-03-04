package autoweka;

import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.File;

public class SetEnvironmentVariablesForAutoWeka {

    public static LibCWrapper INSTANCE;

    private static final Object library;

    interface Msvcrt extends Library {
        int _putenv(String name);

        String getenv(String name);
    }

    interface LibC extends Library {
        int setenv(String name, String value, int overwrite);

        String getenv(String name);

        int unsetenv(String name);
    }

    public static class LibCWrapper {

        public int setenv(String name, String value, int overwrite) {
            if (library instanceof LibC) {
                return ((LibC) library).setenv(name, value, overwrite);
            } else {
                return ((Msvcrt) library)._putenv(name + "=" + value);
            }
        }

        public String getenv(String name) {
            if (library instanceof LibC) {
                return ((LibC) library).getenv(name);
            } else {
                return ((Msvcrt) library).getenv(name);
            }
        }

        public int unsetenv(String name) {
            if (library instanceof LibC) {
                return ((LibC) library).unsetenv(name);
            } else {
                return ((Msvcrt) library)._putenv(name + "=");
            }
        }
    }

    public static boolean needJavaOptions() {
        String version = System.getProperty("java.version");
        String major = version.split("\\.")[0];
        return major.compareTo("1") > 0;
    }

    static {
        if (System.getProperty("os.name").contains("Windows")) {
            library = Native.load("msvcrt", Msvcrt.class);
        } else {
            library = Native.load("c", LibC.class);
        }
        INSTANCE = new LibCWrapper();

        String javaHome = System.getProperty("java.home");
        INSTANCE.setenv("JAVA_HOME", javaHome, 1);
        String pathVar = INSTANCE.getenv("PATH");
        if (pathVar == null) {
            pathVar = "";
        }
        INSTANCE.setenv("PATH", javaHome + "/bin" + File.pathSeparatorChar + pathVar, 1);

        if (needJavaOptions()) {
            String existingOps = INSTANCE.getenv("_JAVA_OPTIONS");
            if (existingOps == null) {
                existingOps = "";
            }
            INSTANCE.setenv("_JAVA_OPTIONS", existingOps + " --add-opens=java.base/java.lang=ALL-UNNAMED", 1);
        }
    }
}
