package com.example;

import java.util.ArrayList;
import java.util.List;

public class Rules {


    public static boolean isInCheck() {
        boolean whiteToMove = Main.isWhiteTurn;
        int kingSquare = -1;
        int kingValue = whiteToMove ? Main.KING : -Main.KING;

        // Find kongens position
        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) != 0) continue;
            if (Main.board[i] == kingValue) {
                kingSquare = i;
                break;
            }
        }

        // Hvis kongen ikke findes, antag skakmat
        if (kingSquare == -1) return true;

        // Tjek om nogen modstandertræk går til kongens felt
        List<Move> enemyMoves = Main.generateAllMoves(!whiteToMove, true);
        for (Move move : enemyMoves) {
            if (move.to == kingSquare) return true;
        }

        return false;
    }

    public static List<Move> generateLegalMoves() {
        boolean whiteToMove = Main.isWhiteTurn;
        List<Move> legalMoves = new ArrayList<>();
        List<Move> pseudoMoves = Main.generateAllMoves(whiteToMove, false);

        for (Move move : pseudoMoves) {
            Move testMove = new Move(move);
            int originalEnPassant = Main.enPassantSquare;

            int captured = Main.makeMove(testMove);
            boolean stillLegal = !isInCheck();
            Main.undoMove(testMove, captured);
            Main.enPassantSquare = originalEnPassant;

            if (stillLegal) {
                legalMoves.add(move);
            }
        }
        return legalMoves;
    }


    /**
     * Tjekker om et felt er truet af den givne farve.
     *
     * @param square feltet vi vil tjekke (0x88)
     * @param byWhite true hvis vi vil tjekke om hvid truer feltet
     * @return true hvis feltet er truet
     */
    public static boolean isSquareAttacked(int square, boolean byWhite) {
        List<Move> moves = Main.generateAllMoves(byWhite, true);
        for (Move move : moves) {
            if (move.to == square) return true;
        }
        return false;
    }
}

