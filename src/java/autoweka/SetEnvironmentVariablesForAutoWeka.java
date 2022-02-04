package autoweka;

import com.sun.jna.Library;
import com.sun.jna.Native;

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

    static {
        if (System.getProperty("os.name").contains("Windows")) {
            library = Native.load("msvcrt", Msvcrt.class);
        } else {
            library = Native.load("c", LibC.class);
        }
        INSTANCE = new LibCWrapper();

        String existingOps = INSTANCE.getenv("_JAVA_OPTIONS");
        if (existingOps == null) {
            existingOps = "";
        }

        INSTANCE.setenv("_JAVA_OPTIONS", existingOps + "--add-opens=java.base/java.lang=ALL-UNNAMED", 0);
    }

    public static void main(String[] args) {

    }
}
