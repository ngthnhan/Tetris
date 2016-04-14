import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by nhan on 14/4/16.
 */
public class Generator {
    private HashSet<String> explored;
    private static final int  NUM_OF_ENCODED = 7;
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

    public static NextState decodeState(String encoded) {
        NextState s = new NextState();

        String[] strs = encoded.split(",");
        int[] nums = new int[NUM_OF_ENCODED];
        int[][] fields = new int[NextState.ROWS][NextState.COLS];
        for (int i = 0; i < strs.length; i++) {
            nums[i] = Integer.parseInt(strs[i]);
        }

        // Decode the nums by shifting bits
        int bits = 0;
        int[] tops = new int[NextState.COLS];
        int t;
        for (int i = 0; i < NextState.ROWS; i++) {
            t = 0;
            for (int j = 0; j < NextState.COLS; j++) {
                int num = bits / 32;
                fields[i][j] = nums[num] & 1;
                nums[bits / 32] <<= 1;
                bits++;
                if (fields[i][j] == 1 && tops[j] < i) {
                    tops[j] = i;
                }
            }
        }

        int nextPiece = nums[NUM_OF_ENCODED-1] & ((1 << 3) - 1);
        System.out.println(nextPiece);

        s.setNextPiece(nextPiece);
        s.setFieldDeep(fields);
        s.setTopDeep(tops);

        return s;
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
            if (!explored.contains(encodedStr)) break;
        } while (!explored.contains(encodedStr));

        return encodedStr;
    }

    public void generate(int limit, String fName) {
        boolean append = readStates(fName);
        ArrayList<String> newStates = new ArrayList<String>();
        String s;

        for (int i = 0; i < limit; i++) {
            s = generateUniqueState();
            newStates.add(s);
            explored.add(s);
        }

        writeStates(fName, append, newStates);
    }

    public boolean readStates(String fName) {
        boolean append;
        try (BufferedReader br = new BufferedReader(new FileReader(fName))) {
            Scanner sc = new Scanner(br);
            while(sc.hasNext()) {
                explored.add(sc.nextLine());
            }
            append = true;
        } catch (IOException e) {
            append = false;
        }

        return append;
    }

    public void writeStates(String fName, boolean append, ArrayList<String> newStates) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fName, append))) {
            for (String s: newStates) {
                bw.write(s);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int limit = (args.length >= 1) ? Integer.parseInt(args[0]) : 100;
        String fName = (args.length >= 2) ? args[1] : "states.txt";

        Generator g = new Generator();
        g.generate(limit, fName);

    }
}
