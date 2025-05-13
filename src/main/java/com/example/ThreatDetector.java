package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.example.MoveGenerator.slide;

public class ThreatDetector {


    private static List<Integer> getAttackersOfSquare(int square, boolean byWhitePieces) {
        List<Integer> attackers = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) != 0) continue;
            int piece = Game.board[i];
            if (piece == 0 || (piece > 0) != byWhitePieces) continue;

            if (MoveGenerator.canPieceReachSquare(i, square, Math.abs(piece), byWhitePieces, Game.board)) {
                attackers.add(i);
            }
        }
        return attackers;
    }

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
     * Finder alle brikker der angriber et bestemt felt.
     * Dette hjælper AI'en med at vurdere om den kan slå angribere frem for at flygte.
     *
     * @param square        Feltet der blev angrebet
     * @param byWhitePieces Om angriberne er hvide brikker
     * @return Liste af felter med angribende brikker
     */
    public static List<Integer> findAttackers(int square, boolean byWhitePieces) {
        List<Integer> attackers = new ArrayList<>();

        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) != 0) continue;

            int piece = Game.board[i];
            if (piece == 0 || (piece > 0) != byWhitePieces) continue;

            if (canPieceAttackSquare(i, piece, square)) {
                attackers.add(i);
            }
        }

        return attackers;
    }


    /**
     * Tjekker om en truet brik kan beskyttes ved at slå angriberen.
     * Dette hjælper AI'en med at vælge "slå angriberen" frem for "flygt med brikken".
     *
     * @param threatenedSquare Feltet med den truede brik
     * @param attackerSquare   Feltet med angriberen
     * @return True hvis angriberen kan slås juridisk
     */
    public static boolean canCaptureAttacker(int threatenedSquare, int attackerSquare) {
        boolean playerIsWhite = Game.board[threatenedSquare] > 0;
        List<Move> legalMoves = Game.generateLegalMoves();

        // Tjek om der er et juridisk træk der slår angriberen
        for (Move move : legalMoves) {
            if (move.to == attackerSquare) {
                // Dette træk slår angriberen - tjek om det er sikkert
                // (dvs. brikken der slår bliver ikke selv slået)
                int movedPiece = Game.board[move.from];
                boolean movedPieceIsWhite = movedPiece > 0;

                if (movedPieceIsWhite == playerIsWhite) {
                    // Dette er en af vores egne brikker
                    System.out.println("   💡 " + Evaluation.getPieceName(movedPiece) + " at " +
                            MoveGenerator.squareToCoord(move.from) + " can capture the attacker " +
                            Evaluation.getPieceName(Game.board[attackerSquare]) + " at " +
                            MoveGenerator.squareToCoord(attackerSquare));
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Forbedret version af findThreatenedPieces der også tjekker om angriberen kan slås.
     * Returnerer kun brikker der er i ægte fare (ikke kan reddes ved at slå angriberen).
     */
    public static int[][] findThreatenedPiecesAdvanced() {
        List<int[]> threatened = new ArrayList<>();

        // Scan hele brættet for brikker tilhørende den aktuelle spiller
        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) != 0) continue; // Spring ugyldige felter over

            int piece = Game.board[i];
            if (piece == 0) continue; // Spring tomme felter over

            // Tjek kun brikker der tilhører den aktuelle spiller
            boolean isWhitePiece = piece > 0;
            if ((Game.isWhiteTurn && !isWhitePiece) || (!Game.isWhiteTurn && isWhitePiece)) {
                continue;
            }

            // Tjek om denne brik er under angreb af modstanderen
            boolean isAttacked = isSquareAttacked(i, !isWhitePiece);

            if (isAttacked) {
                // Find alle angribere af denne brik
                List<Integer> attackers = findAttackers(i, !isWhitePiece);
                boolean canDefendByCapture = false;

                // Tjek om vi kan slå en af angriberne
                for (int attackerSquare : attackers) {
                    if (canCaptureAttacker(i, attackerSquare)) {
                        canDefendByCapture = true;
                        System.out.println("   ✅ " + Evaluation.getPieceName(piece) + " at " +
                                MoveGenerator.squareToCoord(i) + " can be defended by capturing attacker!");
                        break;
                    }
                }

                // Hvis vi ikke kan forsvare ved at slå angriberen, er brikken i ægte fare
                if (!canDefendByCapture) {
                    int pieceValue = Math.abs(Evaluation.getPieceValue(piece));
                    threatened.add(new int[]{i, pieceValue});
                    System.out.println("⚠️ " + Evaluation.getPieceName(piece) + " at " +
                            MoveGenerator.squareToCoord(i) + " is in real danger (cannot defend by capture)!");
                }
            }
        }

        // Konverter til array og sortér efter brikværdi (højeste først)
        int[][] result = threatened.toArray(new int[0][0]);
        Arrays.sort(result, (a, b) -> Integer.compare(b[1], a[1]));

        return result;
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

            if (canPieceAttackSquare(from, piece, square)) {
                return true;
            }
        }
        return false;
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

    private static boolean canPieceAttackSquare(int from, int piece, int target) {
        int abs = Math.abs(piece);
        boolean isWhite = piece > 0;

        if (abs == MoveGenerator.PAWN) {
            int dir = isWhite ? 16 : -16;
            return ((from & 7) != 0 && from + dir - 1 == target) ||
                    ((from & 7) != 7 && from + dir + 1 == target);
        }

        return MoveGenerator.canPieceReachSquare(from, target, abs, isWhite, Game.board);
    }
}
