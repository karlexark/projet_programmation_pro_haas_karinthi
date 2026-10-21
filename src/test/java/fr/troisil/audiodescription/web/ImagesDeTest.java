package fr.troisil.audiodescription.web;

import java.nio.charset.StandardCharsets;

/** Contenus minimaux qui commencent par la signature d'un format d'image. */
final class ImagesDeTest {

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0};
    static final byte[] TEXTE = "ceci n'est pas une image".getBytes(StandardCharsets.UTF_8);

    private ImagesDeTest() {}
}
