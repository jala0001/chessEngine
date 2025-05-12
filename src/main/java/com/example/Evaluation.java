package com.example;

public class Evaluation {
    /**
     * Piece-square table for hvids bønder.
     * Giver bonusser for bønder, der er fremrykkede og i centrum.
     */
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
    /**
     * Piece-square table for hvid springer.
     * Belønner springere for at være i centrum og straffer kantplaceringer.
     */
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
    /**
     * Piece-square table for hvid løber.
     * Belønner løbere for at kontrollere lange diagonaler.
     */
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
    /**
     * Piece-square table for hvid tårn.
     * Belønner tårne for at være på åbne linjer og 7. række.
     */
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
    /**
     * Piece-square table for hvid dronning.
     * Belønner dronningen for at være centralt placeret med god mobilitet.
     */
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
    /**
     * Piece-square table for hvid konge i åbningsspillet.
     * Prioriterer kongesikkerhed bag bondelinjen og rokade.
     */
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

    /**
     * Kalder evaluatePosition med verbose=false.
     * Denne metode bruges internt af søgealgoritmer, hvor detaljeret output ikke er nødvendigt.
     *
     * @return Den samlede stillingsevaluering som en numerisk værdi
     */
    public static int evaluatePosition() {
        return evaluatePosition(false);
    }

    /**
     * Evaluerer den aktuelle stilling baseret på materiel, position og brik-sikkerhed.
     * Denne metode kombinerer flere vurderingsfaktorer til en samlet score.
     *
     * @param verbose Hvis true, udskrives detaljeret evalueringsinfo til konsollen
     * @return Den samlede stillingsevaluering (positiv = fordel til hvid, negativ = fordel til sort)
     */
    public static int evaluatePosition(boolean verbose) {
        int materialScore = 0;
        int positionScore = 0;
        int safetyScore = 0;

        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) != 0) continue; // Spring ugyldige 0x88-felter over

            int piece = Game.board[i];
            if (piece == 0) continue; // Spring tomme felter over

            // Materiel værdi
            materialScore += getPieceValue(piece);

            // Positionel bonus via piece-square tables
            int rank = i >> 4; // Række
            int file = i & 7;  // Linje
            // - Hvide brikker bruger PST som “bund” = rank 0 → PST[56..63], dvs. (7-rank)*8+file
            // - Sorte brikker bruger PST spejlvendt: rank 0 (bund for sort) → PST[0..7], dvs. rank*8+file
            int index = (piece > 0)
                    ? ((7 - rank) * 8 + file) //hvid
                    : (rank * 8 + file);      //sort

            switch (Math.abs(piece)) {
                case MoveGenerator.PAWN -> positionScore += (piece > 0 ? pawnPST[index] : -pawnPST[index]);
                case MoveGenerator.KNIGHT -> positionScore += (piece > 0 ? knightPST[index] : -knightPST[index]);
                case MoveGenerator.BISHOP -> positionScore += (piece > 0 ? bishopPST[index] : -bishopPST[index]);
                case MoveGenerator.ROOK -> positionScore += (piece > 0 ? rookPST[index] : -rookPST[index]);
                case MoveGenerator.QUEEN -> positionScore += (piece > 0 ? queenPST[index] : -queenPST[index]);
                case MoveGenerator.KING -> positionScore += (piece > 0 ? kingPST[index] : -kingPST[index]);
            }

            // Sikkerhedsstraf for ubeskyttede brikker under angreb
            boolean isAttacked = Game.isSquareAttacked(i, piece < 0);
            boolean isDefended = Game.isSquareAttacked(i, piece > 0);

            if (isAttacked && !isDefended) {
                int penalty = Math.abs(getPieceValue(piece)) / 2;
                safetyScore += (piece > 0 ? -penalty : penalty);
            }
        }

        int totalScore = materialScore + positionScore + safetyScore;

        if (verbose) {
            System.out.println("Position Evaluation:");
            System.out.println(" - Material: " + materialScore);
            System.out.println(" - Position: " + positionScore);
            System.out.println(" - Safety: " + safetyScore);
            System.out.println(" = Total: " + totalScore);
        }

        return totalScore;
    }

    /**
     * Tildeler en værdi til hver briktype.
     * Værdierne er positive for hvide brikker og negative for sorte.
     *
     * @param piece Brikværdien fra brættet (positiv = hvid, negativ = sort)
     * @return Den numeriske værdi af brikken
     */
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

    /**
     * Returnerer en læsbar streng der beskriver briktypen.
     * Bruges primært til debug-udskrifter.
     *
     * @param piece Brikværdien fra brættet
     * @return Navnet på brikken (Pawn, Knight, osv.)
     */
    static String getPieceName(int piece) {
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
}
