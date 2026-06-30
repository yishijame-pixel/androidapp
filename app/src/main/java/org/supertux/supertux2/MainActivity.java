package org.supertux.supertux2;

import java.util.Locale;

/**
 * JNI locale stub — native findlocale hardcodes {@code org/supertux/supertux2/MainActivity}.
 * Not an Android Activity; only static helpers for SuperTux C++.
 */
public final class MainActivity {

    private static Locale currLocale = Locale.getDefault();

    private MainActivity() {}

    public static void syncLocale() {
        currLocale = Locale.getDefault();
    }

    public static String getLocale() {
        return currLocale.toString();
    }

    public static String getCountry() {
        return currLocale.getCountry();
    }

    public static String getLang() {
        return currLocale.getLanguage();
    }
}
