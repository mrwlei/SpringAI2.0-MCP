package com.example.enterprise.query.mysql.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SM4UtilsTest {

    @Test
    void shouldKeepLegacyEcbCipherTextCompatible() {
        assertThat(SM4Utils.encryptDataForECB("15075665686"))
                .isEqualTo("wKjOo8JCGXPjoYMTdjNe4g==$");
        assertThat(SM4Utils.decryptDataForECB("4yqeH3sr9fPy51bWROnTDfb4AqHp6Qt5GcdAvl9Jeuc=$"))
                .isEqualTo("370911197811042049");
    }

    @Test
    void shouldEncryptAndDecryptCbcData() {
        var cipherText = SM4Utils.encryptDataForCBC(
                "370911197811042049",
                "72ApzP8ppkuAylCV",
                "UISwD9fW6cFh9SNS");

        assertThat(SM4Utils.decryptDataForCBC(
                        cipherText,
                        "72ApzP8ppkuAylCV",
                        "UISwD9fW6cFh9SNS"))
                .isEqualTo("370911197811042049");
    }
}
