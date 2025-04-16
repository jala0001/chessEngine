package com.example;

public class Main {

    public static void main(String[] args) {
        // Initialize the board from the standard starting position using FEN
        Game.loadFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        // Evaluér stillingen og skriv til konsollen
         System.out.println("Evaluering af stilling: " + AI.evaluatePosition());

        // Kør en alpha-beta evaluering på depth 3
        int score = AI.alphaBeta(6, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
         System.out.println("Alpha-beta evaluering (depth 3): " + score);

        // Display the initial board state in the console
        printBoard();

        //Move aiBestMove = AI.findBestMove(3);
        // System.out.println("Bedste træk for AI (hvid): " + aiBestMove);


        // Launch the graphical user interface
        new ChessGUI();
    }



    /**
     * Prints the current board state to the console.
     * Uses 0x88 indexing to traverse only valid squares.
     * Each square shows an integer representing the piece.
     */
    static void printBoard() {
        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                int sq = rank * 16 + file; // 0x88 indexing
                int piece = Game.board[sq];
                System.out.printf("%2d ", piece);
            }
            System.out.println();
        }
        System.out.println();
    }
}