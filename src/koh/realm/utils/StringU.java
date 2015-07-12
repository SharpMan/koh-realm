package koh.realm.utils;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Neo-Craft
 */
public class StringU {
    
    private static final String ALPHABET = "azertyuiopqsdfghjklmwxcvbnAZERTYUIOPQSDFGHJKLMWXCVBN0123456789_-+*/.";
	
	private static final AtomicReference<Random> RAND = new AtomicReference<>(new Random(System.nanoTime()));
	
	public static char random() {
		return ALPHABET.charAt(RAND.get().nextInt(ALPHABET.length()));
	}
	
	public static String random(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; ++i) sb.append(random());
		return sb.toString();
	}
	
	public static String fillRight(char c, int maxLength, String s) {
		if (s.length() >= maxLength) return s;
		StringBuilder sb = new StringBuilder(s);
		while (sb.length() < maxLength) {
			sb.insert(0, c);
		}
		return sb.toString();
	}
    
}
