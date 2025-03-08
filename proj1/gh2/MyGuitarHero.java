package gh2;
import edu.princeton.cs.algs4.StdAudio;
import edu.princeton.cs.algs4.StdDraw;

/**
 * A client that uses the synthesizer package to replicate a plucked guitar string sound
 */
public class MyGuitarHero {
    public static final String KEYBOARD = "q2we4r5ty7u8i9op-[=zxdcfvgbnjmk,.;/' ";
    public static final GuitarString[] STRINGS = createStrings();

    private static GuitarString[] createStrings() {
        GuitarString[] strings = new GuitarString[KEYBOARD.length()];
        for (int i = 0; i < strings.length; i++) {
            double frequency = 440 * Math.pow(2, ((double) (i - 24) / 12));
            strings[i] = new GuitarString(frequency);
        }
        return strings;
    }
}

