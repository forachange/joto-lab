package com.joto.lab.es.core.utils;

/**
 * @author joey
 * @date 2024/1/4 15:03
 */
public final class EncryptProcessor {
    private EncryptProcessor(){}

    private static final String PREFIX = "ENCC(";
    private static final String SUFFIX = ")";

    public static String unwrapEncryptedValue(String property) {
        return property.substring(
                PREFIX.length(),
                (property.length() - SUFFIX.length()));
    }
}
