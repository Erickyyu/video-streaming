package com.example.videostreaming.utils;

import java.io.Closeable;

public class IOUtil {
    private IOUtil() {}

    public static void closeQuietly(Closeable... closeables) {
        for (Closeable c : closeables) {
            if (c != null) try {
                c.close();
            } catch(Exception ex) {}
        }
    }
}
