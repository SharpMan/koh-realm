package koh.realm.utils;


import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;


/**
 *
 * @author SharpMan
 */
public class Util {

   
    private static final char[] hash = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
        'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3',
        '4', '5', '6', '7', '8', '9', '-', '_'};
    
    
    private static final SecureRandom secureRandom = new SecureRandom();
    
    public static int getRandomValue(int i1, int i2) {
        Random rand = new Random();
        return rand.nextInt(i2 - i1 + 1) + i1;
    }

    public static StringBuilder genTicketID() {
        int length = getRandomValue(10, 20);
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(hash[secureRandom.nextInt(hash.length)]);
        }
        return builder;
    }
    
     public static StringBuilder genTicketID(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(hash[secureRandom.nextInt(hash.length)]);
        }
        return builder;
    }

   
}
