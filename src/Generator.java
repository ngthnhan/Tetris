import java.util.HashSet;
import java.util.Random;

/**
 * Created by nhan on 14/4/16.
 */
public class Generator {
    private HashSet<String> explored;
    private final int  NUM_OF_ENCODED = 7;
    private Random rand;

    public Generator() {
        explored = new HashSet<String>();
        rand = new Random();
    }

    public String convertToStr(int[] nums) {
        StringBuilder sb = new StringBuilder();
        for (int n: nums) {
            sb.append(n);
            sb.append(',');
        }

        return sb.substring(0, sb.length()-1);
    }

    /**
     * The state is encoded into a string. The string will contains integer (32-bit)
     * separated by commas. There will be 7 integers (224 bits) to represent a complete
     * state. The first 200 LSB bits will represent the status of the cells. The next 3 LSB
     * represent the next piece.
     * @return the encoded string of a complete state
     */
    public String generateUniqueState() {
        String encodedStr = "";
        int[] encodedNums = new int[NUM_OF_ENCODED];

        do {
            for (int i = 0; i < NUM_OF_ENCODED; i++) {
                encodedNums[i] = rand.nextInt();
            }

            encodedStr = convertToStr(encodedNums);
        } while (!explored.contains(encodedStr));

        explored.add(encodedStr);
        return encodedStr;
    }
}
