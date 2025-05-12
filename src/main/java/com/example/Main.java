package com.example;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {

        // Initialize the board
        Game.loadFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        // Print initial evaluation
        System.out.println("Evaluering af stilling: " + Evaluation.evaluatePosition());

        // Display the initial board
        printBoard();

        // Launch the GUI
        SwingUtilities.invokeLater(() -> {
            ChessGUI gui = new ChessGUI();

            // Hvis spilleren er sort, lad AI trække først
            if (Game.aiPlaysFirst) {
                makeAIMove(gui);
            }
        });
    }

    // Hjælpemetode til at lave AI-træk
    public static void makeAIMove(ChessGUI gui) {
        // Tjek at det er AI's tur
        if ((Game.isWhiteTurn && !Game.playerIsWhite) || (!Game.isWhiteTurn && Game.playerIsWhite)) {
            Move aiMove = AI.findBestMove(4);
            if (aiMove != null) {
                System.out.println("🤖 AI trækker: " + aiMove);
                Game.makeMove(aiMove);
                //Game.changeTurn(); // Vi skal manuelt skifte tur her
                gui.drawBoard();
            }
        }
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