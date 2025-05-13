package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.example.MoveGenerator.slide;

public class ThreatDetector {
    /**
     * Finder alle brikker tilhørende den aktuelle spiller, der er under angreb.
     * Returnerer en liste sorteret efter brikværdi (mest værdifulde først).
     * Bruges til at prioritere redning af værdifulde brikker.
     *
     * @return Et array af [felt, brikværdi] par for truede brikker
     */
    public static int[][] findThreatenedPieces() {
        List<int[]> threatened = new ArrayList<>();

        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) != 0) continue;

            int piece = Game.board[i];
            if (piece == 0) continue;

            boolean isWhitePiece = piece > 0;
            if ((Game.isWhiteTurn && !isWhitePiece) || (!Game.isWhiteTurn && isWhitePiece)) continue;

            if (isSquareAttacked(i, !isWhitePiece)) {
                threatened.add(new int[]{i, Math.abs(Evaluation.getPieceValue(piece))});
            }
        }

        threatened.sort((a, b) -> Integer.compare(b[1], a[1]));
        return threatened.toArray(new int[0][0]);
    }


    /**
     * Kontrollerer om et felt er under angreb af brikker af en bestemt farve.
     * Bruges til at tjekke om en brik er i fare eller om et træk er sikkert.
     *
     * @param square        Feltet der skal tjekkes
     * @param byWhitePieces Hvis true, tjekkes angreb fra hvide brikker, ellers sorte
     * @return True hvis feltet er under angreb
     */
    public static boolean isSquareAttacked(int square, boolean byWhitePieces) {
        for (int from = 0; from < 128; from++) {
            if ((from & 0x88) != 0) continue;

            int piece = Game.board[from];
            if (piece == 0 || (piece > 0) != byWhitePieces) continue;

            int absPiece = Math.abs(piece);

            // Brug central metode for alle brikker undtagen bønder
            if (absPiece != MoveGenerator.PAWN) {
                if (MoveGenerator.canPieceReachSquare(from, square, absPiece, byWhitePieces, Game.board)) {
                    return true;
                }
            } else {
                // Specialhåndtering for bondeangreb (de angriber ikke i samme retning som de går)
                int dir = byWhitePieces ? 16 : -16;
                if ((from & 7) != 0 && from + dir - 1 == square) return true; // venstre diagonal
                if ((from & 7) != 7 && from + dir + 1 == square) return true; // højre diagonal
            }
        }
        return false;
    }

    private static boolean canReachSquare(int from, int to, int pieceType, boolean isWhite) {
        return MoveGenerator.canPieceReachSquare(from, to, pieceType, isWhite, Game.board);
    }


    /**
     * Tjekker om en brik er forsvaret af brikker af samme farve.
     * Bruges til at vurdere bytte-situationer: om en brik kan slås sikkert.
     *
     * @param square        Feltet der skal tjekkes
     * @param byWhitePieces Hvis true, tjekkes forsvar fra hvide brikker, ellers sorte
     * @return True hvis feltet er forsvaret
     */
    private static boolean isSquareDefendedBy(int square, boolean byWhitePieces) {
        StringBuilder defenders = new StringBuilder();
        boolean isDefended = false;

        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) != 0) continue;     // Spring ugyldige felter over
            if (i == square) continue;         // Skip feltet selv

            int piece = Game.board[i];
            if (piece == 0) continue;          // Skip tomme felter

            boolean isPieceWhite = piece > 0;
            if (isPieceWhite != byWhitePieces) continue;

            int absPiece = Math.abs(piece);
            boolean canDefend = false;

            // Brug fælles metode for alt undtagen bønder
            if (absPiece != MoveGenerator.PAWN) {
                canDefend = MoveGenerator.canPieceReachSquare(i, square, absPiece, isPieceWhite, Game.board);
            } else {
                // Bonde-speciallogik (diagonal forsvar)
                int dir = isPieceWhite ? 16 : -16;
                if ((i & 7) != 0 && i + dir - 1 == square) canDefend = true;
                if ((i & 7) != 7 && i + dir + 1 == square) canDefend = true;
            }

            if (canDefend) {
                if (defenders.length() > 0) defenders.append(", ");
                defenders.append(Evaluation.getPieceName(piece)).append(" at ").append(MoveGenerator.squareToCoord(i));
                isDefended = true;
            }
        }

        if (isDefended) {
            System.out.println("   ⚠️ Square " + MoveGenerator.squareToCoord(square) +
                    " is defended by: " + defenders);
        }

        return isDefended;
    }


    /**
     * Tjekker om en slag-destination er forsvaret.
     * Kritisk for at undgå dårlige bytter hvor AI'en taber materiel.
     *
     * @param captureSquare   Feltet hvor slaget sker
     * @param attackerIsWhite Om den angribende brik er hvid
     * @return True hvis den slåede brik er forsvaret
     */
    static boolean isCapturedPieceDefended(int captureSquare, boolean attackerIsWhite) {
        if (Game.board[captureSquare] != 0) {
            boolean capturedPieceIsWhite = Game.board[captureSquare] > 0;

            // Vigtigt: Vi skal tjekke om destinationsfeltet er forsvaret af brikker af SAMME farve
            // som den brik, der bliver slået (modsat farve af angriberen)
            return isSquareDefendedBy(captureSquare, capturedPieceIsWhite);
        }
        return false;
    }

    /**
     * Tjekker om et destinationsfelt vil være under angreb EFTER et træk er udført.
     * Afgørende for at undgå at flytte brikker til usikre felter.
     *
     * @param move Trækket der skal vurderes
     * @return True hvis destinationen vil være under angreb efter trækket
     */
    static boolean isDestinationAttackedAfterMove(Move move) {
        int movedPiece = Game.board[move.from];
        boolean isWhite = movedPiece > 0;

        // Midlertidigt udfør trækket for at se, hvad der sker efter
        int captured = Game.makeMove(move);
        boolean isUnderAttack = false;
        StringBuilder attackers = new StringBuilder();

        // Gennemgå alle felter på brættet og find modstanderens brikker
        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) != 0) continue; // Ignorer ugyldige felter
            if (i == move.to) continue;    // Undgå at tjekke destination mod sig selv

            int piece = Game.board[i];
            if (piece == 0) continue;

            boolean isEnemy = (piece > 0) != isWhite;
            if (!isEnemy) continue;

            int absPiece = Math.abs(piece);

            boolean canAttack;

            // Brug fælles metode til at afgøre, om brikken kan angribe det nye felt
            if (absPiece == MoveGenerator.PAWN) {
                // Special case: bønder angriber diagonalt, ikke fremad
                int dir = (piece > 0) ? 16 : -16;
                canAttack = ((i & 7) != 0 && i + dir - 1 == move.to) ||
                        ((i & 7) != 7 && i + dir + 1 == move.to);
            } else {
                canAttack = MoveGenerator.canPieceReachSquare(i, move.to, absPiece, piece > 0, Game.board);
            }

            if (canAttack) {
                if (attackers.length() > 0) attackers.append(", ");
                attackers.append(Evaluation.getPieceName(piece)).append(" at ").append(MoveGenerator.squareToCoord(i));
                isUnderAttack = true;
            }
        }

        // Gendan brættet til før trækket
        Game.undoMove(move, captured);

        if (isUnderAttack) {
            System.out.println("   🚨 After moving " + Evaluation.getPieceName(movedPiece) + " to " +
                    MoveGenerator.squareToCoord(move.to) + ", it would be attacked by: " + attackers);
        }

        return isUnderAttack;
    }
}
