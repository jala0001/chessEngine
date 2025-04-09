package com.example;

public class Main {

    public static void main(String[] args) {
        // Initialize the board from the standard starting position using FEN
        Game.loadFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        // Display the initial board state in the console
        printBoard();

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