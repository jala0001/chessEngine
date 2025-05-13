package com.example;

import com.example.Game;
import com.example.MoveGenerator;

import java.util.ArrayList;
import java.util.List;

public class Evaluation {
    static final int[] pawnPST = {
            0,  5,  5, -10, -10,  5,  5,  0,
            0, 10, 10,   0,   0, 10, 10,  0,
            0, 10, 20,  20,  20, 20, 10,  0,
            5, 15, 15,  25,  25, 15, 15,  5,
            10, 20, 20,  30,  30, 20, 20, 10,
            5, 10, 10,  20,  20, 10, 10,  5,
            0,  0,  0,   0,   0,  0,  0,  0,
            0,  0,  0,   0,   0,  0,  0,  0
    };
    static final int[] knightPST = {
            -50, -40, -30, -30, -30, -30, -40, -50,
            -40, -20,   0,   0,   0,   0, -20, -40,
            -30,   0,  10,  15,  15,  10,   0, -30,
            -30,   5,  15,  20,  20,  15,   5, -30,
            -30,   0,  15,  20,  20,  15,   0, -30,
            -30,   5,  10,  15,  15,  10,   5, -30,
            -40, -20,   0,   5,   5,   0, -20, -40,
            -50, -40, -30, -30, -30, -30, -40, -50
    };
    static final int[] bishopPST = {
            -20, -10, -10, -10, -10, -10, -10, -20,
            -10,   5,   0,   0,   0,   0,   5, -10,
            -10,  10,  10,  10,  10,  10,  10, -10,
            -10,   0,  10,  10,  10,  10,   0, -10,
            -10,   5,   5,  10,  10,   5,   5, -10,
            -10,   0,   5,  10,  10,   5,   0, -10,
            -10,   0,   0,   0,   0,   0,   0, -10,
            -20, -10, -10, -10, -10, -10, -10, -20
    };
    static final int[] rookPST = {
            0,   0,   0,   5,   5,   0,   0,   0,
            -5,   0,   0,   0,   0,   0,   0,  -5,
            -5,   0,   0,   0,   0,   0,   0,  -5,
            -5,   0,   0,   0,   0,   0,   0,  -5,
            -5,   0,   0,   0,   0,   0,   0,  -5,
            -5,   0,   0,   0,   0,   0,   0,  -5,
            5,  10,  10,  10,  10,  10,  10,   5,
            0,   0,   0,   0,   0,   0,   0,   0
    };
    static final int[] queenPST = {
            -20, -10, -10,  -5,  -5, -10, -10, -20,
            -10,   0,   0,   0,   0,   0,   0, -10,
            -10,   0,   5,   5,   5,   5,   0, -10,
            -5,   0,   5,   5,   5,   5,   0,  -5,
            0,   0,   5,   5,   5,   5,   0,  -5,
            -10,   5,   5,   5,   5,   5,   0, -10,
            -10,   0,   5,   0,   0,   0,   0, -10,
            -20, -10, -10,  -5,  -5, -10, -10, -20
    };
    static final int[] kingPST = {
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -20, -30, -30, -40, -40, -30, -30, -20,
            -10, -20, -20, -20, -20, -20, -20, -10,
            20,  20,   0,   0,   0,   0,  20,  20,
            20,  30,  10,   0,   0,  10,  30,  20
    };

    private static int pstIndex(int square, boolean isWhite) {
        int rank = square >> 4;
        int file = square & 7;
        return isWhite ? ((7 - rank) * 8 + file) : (rank * 8 + file);
    }

    public static int evaluatePosition() {
        return evaluatePosition(false);
    }

    public static int evaluatePosition(boolean verbose) {
        int materialScore = 0;
        int positionScore = 0;
        int safetyScore = 0;
        int mateScore = 0;

        // *** KRITISK: TJEK FOR MAT FØRST ***
        mateScore = MateDetector.evaluateMateThreats();

        // Hvis der er en mat-trussel, er andre faktorer mindre vigtige
        if (Math.abs(mateScore) >= 500_000) {
            if (verbose) {
                System.out.println("Position Evaluation:");
                System.out.println(" - Material: " + materialScore);
                System.out.println(" - Position: " + positionScore);
                System.out.println(" - Safety: " + safetyScore);
                System.out.println(" - MATE THREAT: " + mateScore);
                System.out.println(" = Total: " + mateScore);
            }
            return mateScore;
        }

        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) != 0) continue;

            int piece = Game.board[i];
            if (piece == 0) continue;

            materialScore += getPieceValue(piece);
          
            // Positionel bonus via piece-square tables
            int rank = i >> 4; // Række
            int file = i & 7;  // Linje
            // - Hvide brikker bruger PST som "bund" = rank 0 → PST[56..63], dvs. (7-rank)*8+file
            // - Sorte brikker bruger PST spejlvendt: rank 0 (bund for sort) → PST[0..7], dvs. rank*8+file
            int index = pstIndex(i, piece > 0);
          
            switch (Math.abs(piece)) {
                case MoveGenerator.PAWN -> positionScore += (piece > 0 ? pawnPST[index] : -pawnPST[index]);
                case MoveGenerator.KNIGHT -> positionScore += (piece > 0 ? knightPST[index] : -knightPST[index]);
                case MoveGenerator.BISHOP -> positionScore += (piece > 0 ? bishopPST[index] : -bishopPST[index]);
                case MoveGenerator.ROOK -> positionScore += (piece > 0 ? rookPST[index] : -rookPST[index]);
                case MoveGenerator.QUEEN -> positionScore += (piece > 0 ? queenPST[index] : -queenPST[index]);
                case MoveGenerator.KING -> positionScore += (piece > 0 ? kingPST[index] : -kingPST[index]);
            }

            boolean isAttacked = Game.isSquareAttacked(i, piece < 0);
            boolean isDefended = ThreatDetector.isCapturedPieceDefended(i, piece < 0);

            if (isAttacked && !isDefended) {
                int attacker = findLeastValuableAttacker(i, piece < 0, Game.board);
                if (attacker != -1) {
                    int see = staticExchangeEval(i, attacker);
                    if (see < 0) {
                        safetyScore += (piece > 0 ? see : -see);
                    }
                }
            }

        }

        int totalScore = materialScore + positionScore + safetyScore + mateScore;

        if (verbose) {
            System.out.println("Position Evaluation:");
            System.out.println(" - Material: " + materialScore);
            System.out.println(" - Position: " + positionScore);
            System.out.println(" - Safety: " + safetyScore);
            System.out.println(" - Mate threats: " + mateScore);
            System.out.println(" = Total: " + totalScore);
        }

        return totalScore;
    }

    static int getPieceValue(int piece) {
        return switch (piece) {
            case MoveGenerator.PAWN -> 100;
            case MoveGenerator.KNIGHT -> 320;
            case MoveGenerator.BISHOP -> 330;
            case MoveGenerator.ROOK -> 500;
            case MoveGenerator.QUEEN -> 900;
            case MoveGenerator.KING -> 20000;
            case -MoveGenerator.PAWN -> -100;
            case -MoveGenerator.KNIGHT -> -320;
            case -MoveGenerator.BISHOP -> -330;
            case -MoveGenerator.ROOK -> -500;
            case -MoveGenerator.QUEEN -> -900;
            case -MoveGenerator.KING -> -20000;
            default -> 0;
        };
    }

    public static String getPieceName(int piece) {
        return switch (Math.abs(piece)) {
            case MoveGenerator.PAWN -> "Pawn";
            case MoveGenerator.KNIGHT -> "Knight";
            case MoveGenerator.BISHOP -> "Bishop";
            case MoveGenerator.ROOK -> "Rook";
            case MoveGenerator.QUEEN -> "Queen";
            case MoveGenerator.KING -> "King";
            default -> "Unknown";
        };
    }

    /**
     * Udfører en Static Exchange Evaluation (SEE) på feltet 'square' hvor en angrebende brik står på 'attackerSquare'.
     * Den simulerer skiftende slag og modsvar med de mindst værdifulde angribere, og vurderer bytte-kædens nettoværdi.
     *
     * @param square Det felt hvor udvekslingen sker
     * @param attackerSquare Den første angriber der starter udvekslingen
     * @return Den materielle gevinst (positiv, 0 eller negativ) hvis udvekslingen gennemføres
     */
    public static int staticExchangeEval(int square, int attackerSquare) {
        int[] boardCopy = Game.board.clone();
        int[] gains = new int[32];
        int depth = 0;

        int capturedPiece = boardCopy[square];
        gains[depth++] = Math.abs(getPieceValue(capturedPiece));

        // Første slag
        boardCopy[square] = boardCopy[attackerSquare];
        boardCopy[attackerSquare] = 0;

        boolean whiteToMove = boardCopy[square] > 0;

        while (true) {
            int nextAttacker = findLeastValuableAttacker(square, whiteToMove, boardCopy);
            if (nextAttacker == -1) break;

            int pieceCaptured = boardCopy[square];
            boardCopy[square] = boardCopy[nextAttacker];
            boardCopy[nextAttacker] = 0;

            gains[depth] = Math.abs(getPieceValue(pieceCaptured)) - gains[depth - 1];
            depth++;
            whiteToMove = !whiteToMove;
        }

        // Vind/tab beregnes baglæns
        for (int i = depth - 2; i >= 0; i--) {
            gains[i] = Math.max(-gains[i + 1], gains[i]);
        }

        return gains[0];
    }

    private static int findLeastValuableAttacker(int square, boolean whiteToMove, int[] board) {
        int bestValue = Integer.MAX_VALUE;
        int bestIndex = -1;

        for (int from = 0; from < 128; from++) {
            if ((from & 0x88) != 0 || board[from] == 0) continue;
            if ((board[from] > 0) != whiteToMove) continue;

            int absPiece = Math.abs(board[from]);
            if (MoveGenerator.canPieceReachSquare(from, square, absPiece, whiteToMove, board)) {
                int val = Math.abs(getPieceValue(board[from]));
                if (val < bestValue) {
                    bestValue = val;
                    bestIndex = from;
                }
            }
        }

        return bestIndex;
    }

}
