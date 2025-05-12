package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AI-klassen implementerer skak-motoren, som beregner og vælger træk.
 * Den indeholder evalueringsfunktioner, søgealgoritmer og sikkerhedskontroller.
 */

public class AI {

    // ===== PIECE-SQUARE TABLES =====
    // Disse tabeller giver positionelle bonusser baseret på brikkernes placering på brættet

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

    // ===== EVALUERINGS-METODER =====

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
    private static int getPieceValue(int piece) {
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
    private static String getPieceName(int piece) {
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

    // ===== TRUSSEL-DETEKTIONS METODER =====

    /**
     * Finder alle brikker tilhørende den aktuelle spiller, der er under angreb.
     * Returnerer en liste sorteret efter brikværdi (mest værdifulde først).
     * Bruges til at prioritere redning af værdifulde brikker.
     *
     * @return Et array af [felt, brikværdi] par for truede brikker
     */
    private static int[][] findThreatenedPieces() {
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
                int pieceValue = Math.abs(getPieceValue(piece));
                threatened.add(new int[]{i, pieceValue});

                System.out.println("⚠️ " + getPieceName(piece) + " at " +
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
                defenders.append(getPieceName(piece)).append(" at ").append(MoveGenerator.squareToCoord(i));
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
    private static boolean isCapturedPieceDefended(int captureSquare, boolean attackerIsWhite) {
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
    private static boolean isDestinationAttackedAfterMove(Move move) {
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
                attackers.append(getPieceName(piece)).append(" at ").append(MoveGenerator.squareToCoord(i));
                isUnderAttack = true;
            }
        }

        // Gendan den originale position
        Game.undoMove(move, captured);

        if (isUnderAttack) {
            System.out.println("   🚨 After moving " + getPieceName(movedPiece) + " to " +
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
                            " is defended by " + getPieceName(piece) + " at " +
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
                attackers.append(getPieceName(piece)).append(" at ").append(MoveGenerator.squareToCoord(i));
                squareIsSafe = false;
            }
        }

        if (!squareIsSafe) {
            System.out.println("   ⚠️ Square " + MoveGenerator.squareToCoord(destination) +
                    " is attacked by: " + attackers);
        }

        return squareIsSafe;
    }

    // ===== SØGEALGORITMER =====

    /**
     * Sorterer træk efter deres forventede værdi.
     * Prioriterer slag med positiv materielforskel højest.
     * Dette forbedrer alpha-beta beskæring markant.
     *
     * @param moves Listen af træk der skal sorteres
     * @return Sorteret liste med "mest lovende" træk først
     */
    private static void sortMoves(List<Move> moves) {
        moves.sort((a, b) -> Integer.compare(moveScore(b), moveScore(a)));
    }

    /**
     * Tildeler en "vigtighedsscore" til et træk for move-ordering.
     * Slag af høj-værdi brikker med lav-værdi brikker prioriteres højest.
     *
     * @param move Trækket der skal evalueres
     * @return En numerisk score, hvor højere tal = bedre træk for move-ordering
     */
    private static int moveScore(Move move) {
        int fromPiece = Game.board[move.from];
        int toPiece = Game.board[move.to];

        // Hvis det er et capture
        if (toPiece != 0) {
            int captureGain = Math.abs(getPieceValue(toPiece)) - Math.abs(getPieceValue(fromPiece));
            // Giv en "basis" på 1000 for at prioritere captures,
            // plus en differensafhængig bonus.
            return 1000 + captureGain;
        }

        // Alm. ikke-capture
        return 0;
    }

    /**
     * Quiescence-søgning forhindrer horisonteffekten ved at fortsætte søgningen
     * ved ustabile positioner (især slag) indtil stillingen er "rolig".
     *
     * @param alpha Alpha-værdi for pruning
     * @param beta Beta-værdi for pruning
     * @param maximizingPlayer Om den aktuelle spiller maksimerer (hvid) eller minimerer (sort)
     * @return Evaluering af stillingen når den er "rolig"
     */
    private static int quiescence(int alpha, int beta, boolean maximizingPlayer) {
        int standPat = evaluatePosition();

        if (maximizingPlayer) {
            if (standPat >= beta) return beta;
            if (alpha < standPat) alpha = standPat;
        } else {
            if (standPat <= alpha) return alpha;
            if (beta > standPat) beta = standPat;
        }

        List<Move> captures = Game.generateLegalMoves();

        // 🎯 Filtrér kun captures og sorter dem efter værdi (MVV-LVA)
        captures.removeIf(m -> Game.board[m.to] == 0);

        captures.sort((a, b) -> {
            int aValue = Math.abs(getPieceValue(Game.board[a.to])) - Math.abs(getPieceValue(Game.board[a.from]));
            int bValue = Math.abs(getPieceValue(Game.board[b.to])) - Math.abs(getPieceValue(Game.board[b.from]));
            return Integer.compare(bValue, aValue); // højeste forskel først
        });

        for (Move move : captures) {
            int captured = Game.makeMove(move);
            int score = quiescence(alpha, beta, !maximizingPlayer);
            Game.undoMove(move, captured);

            if (maximizingPlayer) {
                if (score > alpha) alpha = score;
                if (alpha >= beta) break;
            } else {
                if (score < beta) beta = score;
                if (beta <= alpha) break;
            }
        }

        return maximizingPlayer ? alpha : beta;
    }

    /**
     * Alpha-beta pruning algoritme for effektiv søgning.
     * Bruger move-ordering for at optimere beskæringer.
     *
     * @param depth Resterende søgedybde
     * @param alpha Alpha-værdi for pruning
     * @param beta Beta-værdi for pruning
     * @param maximizingPlayer Om den aktuelle spiller maksimerer (hvid) eller minimerer (sort)
     * @return Bedste score for den aktuelle spiller
     */
    public static int alphaBeta(int depth, int alpha, int beta, boolean maximizingPlayer) {
        // Basetilfælde
        if (depth == 0) {
            return quiescence(alpha, beta, maximizingPlayer);
        }

        // Generer og sortér træk (move ordering)
        List<Move> moves = Game.generateLegalMoves();
        sortMoves(moves);

        if (maximizingPlayer) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : moves) {
                int captured = Game.makeMove(move);

                int eval = alphaBeta(depth - 1, alpha, beta, false);

                Game.undoMove(move, captured);

                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);

                // Pruning
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (Move move : moves) {
                int captured = Game.makeMove(move);

                int eval = alphaBeta(depth - 1, alpha, beta, true);

                Game.undoMove(move, captured);

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);

                // Pruning
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }


    public static Move findBestMove(int maxDepth) {
        List<Move> legalMoves = Game.generateLegalMoves();

        // Sortér dem med MVV-LVA (Most Valuable Victim – Least Valuable Attacker)
        sortMoves(legalMoves);

        Move bestMove = null;
        Move bestMoveAtDepth = null;
        int bestScoreAtDepth = 0;

        // Iterativ deepening
        for (int depth = 1; depth <= maxDepth; depth++) {
            System.out.println("\n===== ITERATIVE DEEPENING - DEPTH " + depth + " =====");

            bestMoveAtDepth = findBestMoveAtDepth(depth, legalMoves);

            if (bestMoveAtDepth != null) {
                bestMove = bestMoveAtDepth;

                // Evaluer det valgte træk
                int captured = Game.makeMove(bestMove);
                boolean isMaximizingRoot = Game.isWhiteTurn;
                bestScoreAtDepth = evaluatePosition();
                Game.undoMove(bestMove, captured);

                System.out.println("✓ BEST MOVE AT DEPTH " + depth + ": " + bestMove +
                        " with score: " + bestScoreAtDepth);
            }

            // Opdater move-rækkefølgen baseret på det bedste træk
            // Dette forbedrer alpha-beta cutoffs i næste iteration
            if (bestMove != null) {
                reorderMovesBasedOnPreviousSearch(legalMoves, bestMove);
            }
        }

        return bestMove;
    }

    private static Move findBestMoveAtDepth(int depth, List<Move> moves) {
        boolean isMaximizingRoot = Game.isWhiteTurn;
        Move bestMove = null;
        int bestScore = isMaximizingRoot ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        System.out.println("Current position evaluation before AI's move:");
        evaluatePosition(true);

        // *** FIND TRUEDE BRIKKER ***
        int[][] threatenedPieces = findThreatenedPieces();
        if (threatenedPieces.length > 0) {
            System.out.println("\n⚠️⚠️⚠️ ALERT: " + threatenedPieces.length + " pieces are under attack!");
            for (int[] piece : threatenedPieces) {
                int square = piece[0];
                int value = piece[1];
                System.out.println("  - " + getPieceName(Game.board[square]) +
                        " (value: " + value + ") at " +
                        MoveGenerator.squareToCoord(square));
            }
        }

        for (Move move : moves) {
            int movedPiece = Game.board[move.from];
            int targetPiece = Game.board[move.to];
            int captureBonus = 0;
            int safetyPenalty = 0;
            int rescueBonus = 0;

            // *** TJEK OM DETTE TRÆK REDDER EN TRUET BRIK ***
            boolean savesThreatenedPiece = false;
            for (int[] threatened : threatenedPieces) {
                if (move.from == threatened[0]) {
                    savesThreatenedPiece = true;
                    boolean destSafe = !isDestinationAttackedAfterMove(move);
                    if (destSafe) {
                        rescueBonus = (int)(threatened[1] * 1.5);
                        System.out.println("💡 Move " + move + " SAVES threatened " +
                                getPieceName(movedPiece) + " (bonus: +" + rescueBonus + ")");
                    } else {
                        System.out.println("❌ Move " + move + " tries to save " +
                                getPieceName(movedPiece) + " but destination is not safe");
                    }
                    break;
                }
            }

            // *** TJEK OM DEN SLÅEDE BRIK ER FORSVARET ***
            boolean capturedPieceDefended = false;
            if (targetPiece != 0) {
                capturedPieceDefended = isCapturedPieceDefended(move.to, movedPiece > 0);
            }

            // *** VIGTIGT: TJEK OM DESTINATIONEN VIL VÆRE UNDER ANGREB EFTER TRÆKKET ***
            boolean destUnderAttack = isDestinationAttackedAfterMove(move);
            if (destUnderAttack) {
                safetyPenalty = Math.abs(getPieceValue(movedPiece));
                System.out.println("⚠️ Move " + move + " puts " +
                        getPieceName(movedPiece) + " in danger at " +
                        MoveGenerator.squareToCoord(move.to));
            }

            // Evaluer position via alphaBeta
            int captured = Game.makeMove(move);
            boolean nextIsMaximizing = !isMaximizingRoot;
            int score = alphaBeta(depth - 1,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    nextIsMaximizing);
            Game.undoMove(move, captured);

            // **2) side** (+1=hvid, −1=sort)
            int side = (movedPiece > 0) ? +1 : -1;

            // **1) rescue‐bonus**
            score += side * rescueBonus;

            // **3) safety‐penalty** (trækker for hvid, tilføjer for sort)
            score -= side * safetyPenalty;

            // **4) capture‐bonus** (tilføj/træk med korrekt fortegn)
            if (targetPiece != 0) {
                captureBonus = Math.abs(getPieceValue(targetPiece));

                if (!destUnderAttack) {
                    if (capturedPieceDefended) {
                        int exchangeValue = captureBonus - Math.abs(getPieceValue(movedPiece));
                        System.out.println("   ⚠️ Exchange evaluation: " + exchangeValue +
                                " (captures " + getPieceName(targetPiece) + " worth " + captureBonus +
                                " but risks " + getPieceName(movedPiece) + " worth " +
                                Math.abs(getPieceValue(movedPiece)) + ")");
                        if (exchangeValue >= 0) {
                            score += side * (exchangeValue + 10);
                        } else {
                            score += side * (exchangeValue * 2);
                        }
                    } else {
                        score += side * captureBonus;
                    }
                } else {
                    int exchangeValue = captureBonus - safetyPenalty;
                    System.out.println("   ⚠️ Exchange evaluation: " + exchangeValue +
                            " (captures " + getPieceName(targetPiece) + " worth " + captureBonus +
                            " but loses " + getPieceName(movedPiece) + " worth " + safetyPenalty + ")");
                    if (exchangeValue > 0) {
                        score += side * exchangeValue;
                    }
                }
            }

            // Formater og udskriv trækinformation
            System.out.printf("Move: %-8s Base score: %-6d", move, score - captureBonus - rescueBonus);
            if (targetPiece != 0) {
                if (capturedPieceDefended) {
                    System.out.printf(" DEFENDED Capture: %-6s", getPieceName(targetPiece));
                } else if (!destUnderAttack) {
                    System.out.printf(" Captures: %-6s (+%d)",
                            getPieceName(targetPiece), captureBonus);
                } else {
                    System.out.printf(" UNSAFE Capture: %-6s", getPieceName(targetPiece));
                }
            }
            if (savesThreatenedPiece && rescueBonus > 0) {
                System.out.printf(" RESCUE BONUS: +%-4d", rescueBonus);
            }
            if (safetyPenalty > 0) {
                System.out.printf(" UNSAFE (-%-4d)", safetyPenalty);
            }
            System.out.printf(" Final score: %-6d\n", score);

            // Opdater bestMove med korrekt max/min‑logik
            if (bestMove == null ||
                    (isMaximizingRoot && score >  bestScore) ||
                    (!isMaximizingRoot && score <  bestScore)) {
                bestScore = score;
                bestMove  = move;
            }
        }

        System.out.println("\n✓ SELECTED: " + bestMove + " with score: " + bestScore);

        // Vis evaluering efter det valgte træk
        if (bestMove != null) {
            int captured = Game.makeMove(bestMove);
            System.out.println("\nPosition evaluation after AI's selected move:");
            evaluatePosition(true);
            Game.undoMove(bestMove, captured);

            if (isDestinationAttackedAfterMove(bestMove)) {
                System.out.println("\n⚠️ WARNING: The selected move puts a piece in immediate danger!");
            }
        }

        return bestMove;
    }

    // Hjælpemetode til at forbedre move-ordering mellem iterationer
    private static void reorderMovesBasedOnPreviousSearch(List<Move> moves, Move bestMove) {
        if (bestMove == null) return;

        // Flyt det bedste træk først i listen
        moves.remove(bestMove);
        moves.add(0, bestMove);
    }
}