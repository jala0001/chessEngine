package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                int pieceValue = Math.abs(Evaluation.getPieceValue(piece));
                threatened.add(new int[]{i, pieceValue});

                System.out.println("⚠️ " + Evaluation.getPieceName(piece) + " at " +
                        MoveGenerator.squareToCoord(i) + " is currently under attack!");
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
     * @param square Feltet der skal tjekkes
     * @param byWhitePieces Hvis true, tjekkes angreb fra hvide brikker, ellers sorte
     * @return True hvis feltet er under angreb
     */
    private static boolean isSquareAttacked(int square, boolean byWhitePieces) {
        // Tjek alle mulige angribere
        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) != 0) continue; // Spring ugyldige felter over

            int piece = Game.board[i];
            if (piece == 0) continue; // Spring tomme felter over

            // Tjek kun brikker af den angivne farve
            boolean isPieceWhite = piece > 0;
            if (isPieceWhite != byWhitePieces) continue;

            // Tjek om denne brik kan angribe feltet
            boolean canAttack = false;

            switch (Math.abs(piece)) {
                case MoveGenerator.PAWN:
                    // Bønder angriber diagonalt fremad
                    int dir = isPieceWhite ? 16 : -16; // Retning bønder bevæger sig
                    if ((i & 7) != 0 && i + dir - 1 == square) canAttack = true; // Angreb venstre
                    if ((i & 7) != 7 && i + dir + 1 == square) canAttack = true; // Angreb højre
                    break;

                case MoveGenerator.KNIGHT:
                    // Springere bevæger sig i L-form
                    for (int offset : MoveGenerator.knightOffsets) {
                        if (i + offset == square) {
                            canAttack = true;
                            break;
                        }
                    }
                    break;

                case MoveGenerator.BISHOP:
                    // Løbere bevæger sig diagonalt
                    for (int offset : MoveGenerator.bishopDirections) {
                        int sq = i;
                        while (true) {
                            sq += offset;
                            if ((sq & 0x88) != 0) break; // Uden for brættet
                            if (sq == square) {
                                canAttack = true;
                                break;
                            }
                            if (Game.board[sq] != 0) break; // Blokeret
                        }
                        if (canAttack) break;
                    }
                    break;

                case MoveGenerator.ROOK:
                    // Tårne bevæger sig horisontalt og vertikalt
                    for (int offset : MoveGenerator.rookDirections) {
                        int sq = i;
                        while (true) {
                            sq += offset;
                            if ((sq & 0x88) != 0) break; // Uden for brættet
                            if (sq == square) {
                                canAttack = true;
                                break;
                            }
                            if (Game.board[sq] != 0) break; // Blokeret
                        }
                        if (canAttack) break;
                    }
                    break;

                case MoveGenerator.QUEEN:
                    // Dronninger bevæger sig som løbere og tårne kombineret
                    // Tjek løber-lignende træk
                    for (int offset : MoveGenerator.bishopDirections) {
                        int sq = i;
                        while (true) {
                            sq += offset;
                            if ((sq & 0x88) != 0) break; // Uden for brættet
                            if (sq == square) {
                                canAttack = true;
                                break;
                            }
                            if (Game.board[sq] != 0) break; // Blokeret
                        }
                        if (canAttack) break;
                    }

                    // Hvis ikke fundet, tjek tårn-lignende træk
                    if (!canAttack) {
                        for (int offset : MoveGenerator.rookDirections) {
                            int sq = i;
                            while (true) {
                                sq += offset;
                                if ((sq & 0x88) != 0) break; // Uden for brættet
                                if (sq == square) {
                                    canAttack = true;
                                    break;
                                }
                                if (Game.board[sq] != 0) break; // Blokeret
                            }
                            if (canAttack) break;
                        }
                    }
                    break;

                case MoveGenerator.KING:
                    // Konger bevæger sig ét felt i alle retninger
                    for (int offset : Game.kingOffsets) {
                        if (i + offset == square) {
                            canAttack = true;
                            break;
                        }
                    }
                    break;
            }

            if (canAttack) return true;
        }

        return false;
    }

    /**
     * Tjekker om en brik er forsvaret af brikker af samme farve.
     * Bruges til at vurdere bytte-situationer: om en brik kan slås sikkert.
     *
     * @param square Feltet der skal tjekkes
     * @param byWhitePieces Hvis true, tjekkes forsvar fra hvide brikker, ellers sorte
     * @return True hvis feltet er forsvaret
     */
    private static boolean isSquareDefendedBy(int square, boolean byWhitePieces) {
        StringBuilder defenders = new StringBuilder();
        boolean isDefended = false;

        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) != 0) continue; // Spring ugyldige felter over
            if (i == square) continue; // Spring feltet selv over

            int piece = Game.board[i];
            if (piece == 0) continue; // Spring tomme felter over

            // Tjek kun brikker af den angivne farve
            boolean isPieceWhite = piece > 0;
            if (isPieceWhite != byWhitePieces) continue;

            // Tjek om denne brik forsvarer feltet
            boolean canDefend = false;

            switch (Math.abs(piece)) {
                case MoveGenerator.PAWN:
                    // Bønder forsvarer diagonalt fremad
                    int forwardDir = isPieceWhite ? 16 : -16;

                    // Tjek om bonden er korrekt placeret til at forsvare diagonalt
                    if ((i & 7) != 0 && i + forwardDir - 1 == square) canDefend = true; // Forsvar venstre diagonal
                    if ((i & 7) != 7 && i + forwardDir + 1 == square) canDefend = true; // Forsvar højre diagonal
                    break;

                case MoveGenerator.KNIGHT:
                    for (int offset : MoveGenerator.knightOffsets) {
                        if (i + offset == square) {
                            canDefend = true;
                            break;
                        }
                    }
                    break;

                case MoveGenerator.BISHOP:
                    for (int offset : MoveGenerator.bishopDirections) {
                        int sq = i;
                        while (true) {
                            sq += offset;
                            if ((sq & 0x88) != 0) break; // Uden for brættet
                            if (sq == square) {
                                canDefend = true;
                                break;
                            }
                            if (Game.board[sq] != 0) break; // Blokeret
                        }
                        if (canDefend) break;
                    }
                    break;

                case MoveGenerator.ROOK:
                    for (int offset : MoveGenerator.rookDirections) {
                        int sq = i;
                        while (true) {
                            sq += offset;
                            if ((sq & 0x88) != 0) break; // Uden for brættet
                            if (sq == square) {
                                canDefend = true;
                                break;
                            }
                            if (Game.board[sq] != 0) break; // Blokeret
                        }
                        if (canDefend) break;
                    }
                    break;

                case MoveGenerator.QUEEN:
                    // Tjek løber-lignende træk
                    for (int offset : MoveGenerator.bishopDirections) {
                        int sq = i;
                        while (true) {
                            sq += offset;
                            if ((sq & 0x88) != 0) break; // Uden for brættet
                            if (sq == square) {
                                canDefend = true;
                                break;
                            }
                            if (Game.board[sq] != 0) break; // Blokeret
                        }
                        if (canDefend) break;
                    }

                    // Hvis ikke fundet, tjek tårn-lignende træk
                    if (!canDefend) {
                        for (int offset : MoveGenerator.rookDirections) {
                            int sq = i;
                            while (true) {
                                sq += offset;
                                if ((sq & 0x88) != 0) break; // Uden for brættet
                                if (sq == square) {
                                    canDefend = true;
                                    break;
                                }
                                if (Game.board[sq] != 0) break; // Blokeret
                            }
                            if (canDefend) break;
                        }
                    }
                    break;

                case MoveGenerator.KING:
                    for (int offset : Game.kingOffsets) {
                        if (i + offset == square) {
                            canDefend = true;
                            break;
                        }
                    }
                    break;
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
     * @param captureSquare Feltet hvor slaget sker
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
        boolean isPieceWhite = movedPiece > 0;

        // Udfør trækket midlertidigt
        int captured = Game.makeMove(move);

        // Detaljeret scanning efter alle mulige angribere i den nye position
        StringBuilder attackers = new StringBuilder();
        boolean isUnderAttack = false;

        // Tjek specifikt for bondeangreb (oftest overset)
        int pawnAttackColor = isPieceWhite ? -1 : 1; // Modstander bondefarve
        int[] pawnAttackOffsets;

        // Hvide brikker angribes af sorte bønder der bevæger sig NV og NØ
        // Sorte brikker angribes af hvide bønder der bevæger sig SV og SØ
        if (isPieceWhite) {
            pawnAttackOffsets = new int[]{-17, -15}; // Sorte bønder angriber diagonalt fremad (NV, NØ)
        } else {
            pawnAttackOffsets = new int[]{15, 17};   // Hvide bønder angriber diagonalt fremad (SV, SØ)
        }

        // Tjek hver mulig bondeangrebsretning
        for (int offset : pawnAttackOffsets) {
            int attackerSquare = move.to - offset; // Hvor en angribende bonde ville være

            if ((attackerSquare & 0x88) == 0) { // Hvis det er et gyldigt felt
                int piece = Game.board[attackerSquare];

                // Tjek om der er en modstanderbonde der kan angribe
                if (piece == pawnAttackColor * MoveGenerator.PAWN) {
                    if (attackers.length() > 0) attackers.append(", ");
                    attackers.append("Pawn at ").append(MoveGenerator.squareToCoord(attackerSquare));
                    isUnderAttack = true;
                }
            }
        }

        // Tjek alle andre briktyper der kunne angribe
        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) != 0) continue; // Spring ugyldige felter over
            if (i == move.to) continue;    // Spring destinationsfeltet selv over

            int piece = Game.board[i];
            if (piece == 0) continue; // Spring tomme felter over

            // Tjek kun modstanderbrikker
            boolean isPieceAttackerWhite = piece > 0;
            if (isPieceAttackerWhite == isPieceWhite) continue;

            // Tjek om denne brik kan angribe destinationen
            boolean canAttack = false;

            switch (Math.abs(piece)) {
                case MoveGenerator.PAWN:
                    // Allerede håndteret ovenfor for mere præcision
                    break;

                case MoveGenerator.KNIGHT:
                    for (int offset : MoveGenerator.knightOffsets) {
                        if (i + offset == move.to) {
                            canAttack = true;
                            break;
                        }
                    }
                    break;

                case MoveGenerator.BISHOP:
                    for (int offset : MoveGenerator.bishopDirections) {
                        int sq = i;
                        while (true) {
                            sq += offset;
                            if ((sq & 0x88) != 0) break; // Uden for brættet
                            if (sq == move.to) {
                                canAttack = true;
                                break;
                            }
                            if (Game.board[sq] != 0) break; // Blokeret
                        }
                        if (canAttack) break;
                    }
                    break;

                case MoveGenerator.ROOK:
                    for (int offset : MoveGenerator.rookDirections) {
                        int sq = i;
                        while (true) {
                            sq += offset;
                            if ((sq & 0x88) != 0) break; // Uden for brættet
                            if (sq == move.to) {
                                canAttack = true;
                                break;
                            }
                            if (Game.board[sq] != 0) break; // Blokeret
                        }
                        if (canAttack) break;
                    }
                    break;

                case MoveGenerator.QUEEN:
                    // Tjek løber-lignende træk
                    for (int offset : MoveGenerator.bishopDirections) {
                        int sq = i;
                        while (true) {
                            sq += offset;
                            if ((sq & 0x88) != 0) break; // Uden for brættet
                            if (sq == move.to) {
                                canAttack = true;
                                break;
                            }
                            if (Game.board[sq] != 0) break; // Blokeret
                        }
                        if (canAttack) break;
                    }

                    // Hvis ikke fundet, tjek tårn-lignende træk
                    if (!canAttack) {
                        for (int offset : MoveGenerator.rookDirections) {
                            int sq = i;
                            while (true) {
                                sq += offset;
                                if ((sq & 0x88) != 0) break; // Uden for brættet
                                if (sq == move.to) {
                                    canAttack = true;
                                    break;
                                }
                                if (Game.board[sq] != 0) break; // Blokeret
                            }
                            if (canAttack) break;
                        }
                    }
                    break;

                case MoveGenerator.KING:
                    for (int offset : Game.kingOffsets) {
                        if (i + offset == move.to) {
                            canAttack = true;
                            break;
                        }
                    }
                    break;
            }

            if (canAttack) {
                if (attackers.length() > 0) attackers.append(", ");
                attackers.append(Evaluation.getPieceName(piece)).append(" at ").append(MoveGenerator.squareToCoord(i));
                isUnderAttack = true;
            }
        }

        // Gendan den originale position
        Game.undoMove(move, captured);

        if (isUnderAttack) {
            System.out.println("   🚨 After moving " + Evaluation.getPieceName(movedPiece) + " to " +
                    MoveGenerator.squareToCoord(move.to) + ", it would be attacked by: " + attackers);
        }

        return isUnderAttack;
    }

    /**
     * Kontrollerer om et felt er sikkert at flytte til.
     * En alternativ implementering af angrebs-detektion, bruges som backup.
     *
     * @param square Feltet der skal tjekkes
     * @param isWhitePiece Om brikken der skal flyttes er hvid
     * @return True hvis feltet er sikkert
     */
    private static boolean isSquareSafeToMoveTo(int square, boolean isWhitePiece) {
        boolean isUnderAttack = Game.isSquareAttacked(square, isWhitePiece);

        if (isUnderAttack) {
            // Tjek bondeangreb specifikt
            int pawnDirection = isWhitePiece ? 16 : -16; // Retning bønder bevæger sig
            int[] pawnAttackOffsets = {pawnDirection - 1, pawnDirection + 1}; // Diagonale slag

            for (int offset : pawnAttackOffsets) {
                int attackerSquare = square - offset; // Omvendt retning for at finde angriber
                if ((attackerSquare & 0x88) != 0) continue; // Spring ugyldige felter over

                int piece = Game.board[attackerSquare];
                if ((isWhitePiece && piece == -MoveGenerator.PAWN) ||
                        (!isWhitePiece && piece == MoveGenerator.PAWN)) {
                    System.out.println("   ⚠️ Square " + MoveGenerator.squareToCoord(square) +
                            " is defended by pawn at " + MoveGenerator.squareToCoord(attackerSquare));
                }
            }

            // Tjek andre brikkers angreb
            for (int i = 0; i < 128; i++) {
                if ((i & 0x88) != 0) continue; // Spring ugyldige felter over

                int piece = Game.board[i];
                if (piece == 0) continue; // Spring tomme felter over

                // Tjek kun modstanderbrikker
                if ((isWhitePiece && piece > 0) || (!isWhitePiece && piece < 0)) continue;

                // Tjek om denne brik kan angribe feltet
                boolean canAttack = false;

                switch (Math.abs(piece)) {
                    case MoveGenerator.PAWN:
                        // Allerede tjekket bønder ovenfor
                        break;

                    case MoveGenerator.KNIGHT:
                        for (int offset : MoveGenerator.knightOffsets) {
                            if (i + offset == square) {
                                canAttack = true;
                                break;
                            }
                        }
                        break;

                    case MoveGenerator.BISHOP:
                    case MoveGenerator.QUEEN:
                        // Tjek diagonale angreb
                        for (int dir : MoveGenerator.bishopDirections) {
                            int sq = i;
                            while (true) {
                                sq += dir;
                                if ((sq & 0x88) != 0) break; // Uden for brættet
                                if (sq == square) {
                                    canAttack = true;
                                    break;
                                }
                                if (Game.board[sq] != 0) break; // Blokeret
                            }
                            if (canAttack) break;
                        }
                        if (Math.abs(piece) == MoveGenerator.BISHOP) break;
                        // Fortsæt for dronning til også at tjekke tårn-retninger

                    case MoveGenerator.ROOK:
                        // Tjek horisontale/vertikale angreb
                        for (int dir : MoveGenerator.rookDirections) {
                            int sq = i;
                            while (true) {
                                sq += dir;
                                if ((sq & 0x88) != 0) break; // Uden for brættet
                                if (sq == square) {
                                    canAttack = true;
                                    break;
                                }
                                if (Game.board[sq] != 0) break; // Blokeret
                            }
                            if (canAttack) break;
                        }
                        break;

                    case MoveGenerator.KING:
                        for (int offset : Game.kingOffsets) {
                            if (i + offset == square) {
                                canAttack = true;
                                break;
                            }
                        }
                        break;
                }

                if (canAttack) {
                    System.out.println("   ⚠️ Square " + MoveGenerator.squareToCoord(square) +
                            " is defended by " + Evaluation.getPieceName(piece) + " at " +
                            MoveGenerator.squareToCoord(i));
                }
            }
        }

        return !isUnderAttack;
    }

    /**
     * Grundigt tjekker om et destinationsfelt er sikkert fra angreb
     * ved at undersøge alle potentielle angribere på brættet.
     *
     * @param destination Destinationsfeltet
     * @param forWhitePiece Om brikken der flytter er hvid
     * @return True hvis feltet er sikkert
     */
    private static boolean isDestinationSafe(int destination, boolean forWhitePiece) {
        boolean squareIsSafe = true;
        StringBuilder attackers = new StringBuilder();

        // Undersøg hvert felt på brættet for potentielle angribere
        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) != 0) continue; // Spring ugyldige felter over

            int piece = Game.board[i];
            if (piece == 0) continue; // Spring tomme felter over

            // Tjek kun modstanderbrikker
            boolean isPieceWhite = piece > 0;
            if ((forWhitePiece && isPieceWhite) || (!forWhitePiece && !isPieceWhite)) {
                continue;
            }

            // Tjek om denne brik kan angribe destinationen
            boolean canAttack = false;

            switch (Math.abs(piece)) {
                case MoveGenerator.PAWN:
                    // Bønder angriber diagonalt fremad
                    int dir = isPieceWhite ? 16 : -16; // Retning bønder bevæger sig
                    if ((i & 7) != 0 && i + dir - 1 == destination) canAttack = true; // Angreb venstre
                    if ((i & 7) != 7 && i + dir + 1 == destination) canAttack = true; // Angreb højre
                    break;

                case MoveGenerator.KNIGHT:
                    // Springere bevæger sig i L-form
                    for (int offset : MoveGenerator.knightOffsets) {
                        if (i + offset == destination) {
                            canAttack = true;
                            break;
                        }
                    }
                    break;

                case MoveGenerator.BISHOP:
                    // Løbere bevæger sig diagonalt
                    for (int offset : MoveGenerator.bishopDirections) {
                        int sq = i;
                        while (true) {
                            sq += offset;
                            if ((sq & 0x88) != 0) break; // Uden for brættet
                            if (sq == destination) {
                                canAttack = true;
                                break;
                            }
                            if (Game.board[sq] != 0) break; // Blokeret
                        }
                        if (canAttack) break;
                    }
                    break;

                case MoveGenerator.ROOK:
                    // Tårne bevæger sig horisontalt og vertikalt
                    for (int offset : MoveGenerator.rookDirections) {
                        int sq = i;
                        while (true) {
                            sq += offset;
                            if ((sq & 0x88) != 0) break; // Uden for brættet
                            if (sq == destination) {
                                canAttack = true;
                                break;
                            }
                            if (Game.board[sq] != 0) break; // Blokeret
                        }
                        if (canAttack) break;
                    }
                    break;

                case MoveGenerator.QUEEN:
                    // Dronninger bevæger sig som løbere og tårne kombineret
                    // Tjek løber-lignende træk
                    for (int offset : MoveGenerator.bishopDirections) {
                        int sq = i;
                        while (true) {
                            sq += offset;
                            if ((sq & 0x88) != 0) break; // Uden for brættet
                            if (sq == destination) {
                                canAttack = true;
                                break;
                            }
                            if (Game.board[sq] != 0) break; // Blokeret
                        }
                        if (canAttack) break;
                    }

                    // Hvis ikke fundet, tjek tårn-lignende træk
                    if (!canAttack) {
                        for (int offset : MoveGenerator.rookDirections) {
                            int sq = i;
                            while (true) {
                                sq += offset;
                                if ((sq & 0x88) != 0) break; // Uden for brættet
                                if (sq == destination) {
                                    canAttack = true;
                                    break;
                                }
                                if (Game.board[sq] != 0) break; // Blokeret
                            }
                            if (canAttack) break;
                        }
                    }
                    break;

                case MoveGenerator.KING:
                    // Konger bevæger sig ét felt i alle retninger
                    for (int offset : Game.kingOffsets) {
                        if (i + offset == destination) {
                            canAttack = true;
                            break;
                        }
                    }
                    break;
            }

            if (canAttack) {
                if (attackers.length() > 0) attackers.append(", ");
                attackers.append(Evaluation.getPieceName(piece)).append(" at ").append(MoveGenerator.squareToCoord(i));
                squareIsSafe = false;
            }
        }

        if (!squareIsSafe) {
            System.out.println("   ⚠️ Square " + MoveGenerator.squareToCoord(destination) +
                    " is attacked by: " + attackers);
        }

        return squareIsSafe;
    }
}
