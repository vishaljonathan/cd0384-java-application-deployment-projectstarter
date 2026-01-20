package com.udacity.catpoint.image;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Fake implementation of ImageService for demos and local use.
 * Randomly determines whether an image contains a cat.
 */
public class FakeImageService implements ImageService {

    private final Random random = new Random();

    @Override
    public boolean imageContainsCat(BufferedImage image) {
        return random.nextBoolean();
    }
}
