package com.mindbridge.chat;

import java.security.SecureRandom;
import java.util.List;

/** Friendly random names such as "Quiet Fox". A new pair is made for every chat. */
public final class PseudonymGenerator {

    private static final List<String> ADJECTIVES = List.of(
        "Quiet", "Gentle", "Brave", "Calm", "Bright", "Kind", "Patient", "Curious",
        "Steady", "Warm", "Cheerful", "Thoughtful", "Humble", "Lively", "Honest", "Sunny");
    private static final List<String> ANIMALS = List.of(
        "Fox", "Owl", "Otter", "Heron", "Panda", "Finch", "Lynx", "Dolphin",
        "Sparrow", "Koala", "Falcon", "Rabbit", "Turtle", "Robin", "Badger", "Swan");
    private static final SecureRandom RANDOM = new SecureRandom();

    private PseudonymGenerator() {
    }

    public static String next() {
        return ADJECTIVES.get(RANDOM.nextInt(ADJECTIVES.size())) + " "
            + ANIMALS.get(RANDOM.nextInt(ANIMALS.size()));
    }

    /** Two different names, one for each participant. */
    public static List<String> pair() {
        String first = next();
        String second;
        do {
            second = next();
        } while (second.equals(first));
        return List.of(first, second);
    }
}
