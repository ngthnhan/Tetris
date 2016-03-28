/**
 * Created by ThanhNhan on 23/03/2016.
 */

public class MainFunction {
    // Write your functions here

    // Feature 1


    // Feature 2

    // Feature 3


    // Feature 4

    // Feature 5

    // Feature 6

    // Feature 7

    // Feature 8

    // Feature 9

    // Feature 10


    // Implementation of f1
    double getLandingHeight(State s, int action) {
        int orient = s.legalMoves()[action][State.ORIENT];
        int slot = s.legalMoves()[action][State.SLOT];
        return getLandingHeight(s, orient, slot);
    }
    /**
     * The height at where the piece is put = height of column + (height of piece / 2)
     * @param s To get the board position and how it will change according to the action
     * @param orient To orientation of the piece
     * @param slot The slot index of the left most column
     * @return The heuristic score for the given state and action taken.
     */
    double getLandingHeight(State s, int orient, int slot) {
        int piece = s.getNextPiece();

        double maxHeight = -1;
        for (int i = 0, col = slot; i < s.getpWidth()[piece][orient]; i++, col++) {
            double height = s.getTop()[col] - s.getpBottom()[piece][orient][i];
            maxHeight = Math.max(maxHeight,  height + s.getpHeight()[piece][orient] / 2.0);
        }
        return maxHeight;
    }
    // Implementation of f2
    int getErodedPieces(State s, int action) {
        int orient = s.legalMoves()[action][State.ORIENT];
        int slot = s.legalMoves()[action][State.SLOT];
        return getErodedPieces(s, orient, slot);
    }

    int getErodedPieces(State s, int orient, int slot) {
        State nextState = new State(s);
        nextState.makeMove(orient, slot);
        return s.getRowsCleared() - nextState.getRowsCleared();
    }
    // Implementation of f3
    int getRowTransition(State s) {
        int transCount = 0;
        for (int i = 0; i < State.ROWS - 1; i++) {
            if(s.getField()[i][0] == 0) transCount++;
            if(s.getField()[i][State.COLS-1] == 0) transCount++;
            for(int j=1;j<State.COLS;j++) {
                if (s.getField()[i][j] != s.getField()[i][j - 1]) {
                    transCount++;
                }
            }
        }
        return transCount;
    }

    int getRowTransition(State s, int action) {
        State nextState = new State(s);
        nextState.makeMove(action);

        int transCount = 0;
        for (int i = 0; i < State.ROWS - 1; i++) {
            if(nextState.getField()[i][0] == 0) transCount++;
            if(nextState.getField()[i][State.COLS-1] == 0) transCount++;
            for(int j=1;j<State.COLS;j++) {
                if (nextState.getField()[i][j] != nextState.getField()[i][j - 1]) {
                    transCount++;
                }
            }
        }
        return transCount;
    }
    // Implementation of f4

    // Implementation of f5

    // Implementation of f6

    // Implementation of f7

    // Implementation of f8

    // Implementation of f9

    // Implementation of f10

}
