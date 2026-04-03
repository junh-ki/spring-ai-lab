package com.example.springailab.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class HashUtils {

    public static String computeHash(final String content) {
        try {
            return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                    MessageDigest.getInstance("SHA-256")
                        .digest(
                            content.getBytes(StandardCharsets.UTF_8)
                        )
                );
        } catch (final NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new RuntimeException(
                "SHA-256 not available",
                noSuchAlgorithmException
            );
        }
    }
}
