package com.example;

import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {
    // Brikker: positive = hvid, negative = sort
        static final int PAWN = 1;
    static final int KNIGHT = 2;
    static final int BISHOP = 3;
    static final int ROOK = 4;
    static final int QUEEN = 5;
    static final int KING = 6;
    static final int[] knightOffsets = { -33, -31, -18, -14, 14, 18, 31, 33 };
    static final int[] rookDirections = { -16, 16, -1, 1 };
    static final int[] bishopDirections = { -17, -15, 15, 17 };

    static void generateKnightMoves(int from, List<Move> moves, boolean whiteToMove) {
        for (int offset : knightOffsets) {
            int to = from + offset;

            // Brug 0x88-tricket til at tjekke om 'to' er gyldigt
            if ((to & 0x88) != 0) continue;

            int targetPiece = Game.board[to];

            // Tjek om vi prøver at slå vores egen brik
            if (Game.isWhiteTurn && targetPiece > 0) continue;
            if (!Game.isWhiteTurn && targetPiece < 0) continue;

            // OK, gyldigt træk – tilføj det
            moves.add(new Move(from, to));
        }
    }

    static void generateRookMoves(int from, List<Move> moves, boolean whiteToMove) {
        for (int direction : rookDirections) {
            int to = from;

            while (true) {
                to += direction;
                if ((to & 0x88) != 0) break;

                int target = Game.board[to];

                if (target == 0) {
                    moves.add(new Move(from, to));
                } else {
                    if ((whiteToMove && target < 0) || (!whiteToMove && target > 0)) {
                        moves.add(new Move(from, to));
                    }
                    break;
                }
            }
        }
    }

    static void generatePawnMoves(int from, List<Move> moves, boolean whiteToMove) {
        int direction = whiteToMove ? 16 : -16;
        int startRank = whiteToMove ? 1 : 6;

        int oneForward = from + direction;
        if ((oneForward & 0x88) == 0 && Game.board[oneForward] == 0) {
            moves.add(new Move(from, oneForward));

            int twoForward = from + 2 * direction;
            if ((from >> 4) == startRank && Game.board[twoForward] == 0) {
                moves.add(new Move(from, twoForward));
            }
        }

        int[] captureOffsets = { direction - 1, direction + 1 };
        for (int offset : captureOffsets) {
            int target = from + offset;
            if ((target & 0x88) != 0) continue;

            int targetPiece = Game.board[target];
            if ((whiteToMove && targetPiece < 0) || (!whiteToMove && targetPiece > 0)) {
                moves.add(new Move(from, target));
            }
        }

        int epRank = whiteToMove ? 4 : 3;
        if ((from >> 4) == epRank && Game.enPassantSquare != -1) {
            int[] epOffsets = { -1, 1 };
            for (int sideOffset : epOffsets) {
                int sideSquare = from + sideOffset;
                if ((sideSquare & 0x88) != 0) continue;

                int sidePiece = Game.board[sideSquare];
                if ((whiteToMove && sidePiece != -PAWN) || (!whiteToMove && sidePiece != PAWN)) {
                    continue;
                }

                int epTarget = from + direction + sideOffset;
                if (epTarget == Game.enPassantSquare) {
                    Move epMove = new Move(from, epTarget);
                    epMove.isEnPassant = true;
                    moves.add(epMove);
                }
            }
        }
    }

    static void generateBishopMoves(int from, List<Move> moves, boolean whiteToMove) {
        for (int direction : bishopDirections) {
            int to = from;

            while (true) {
                to += direction;
                if ((to & 0x88) != 0) break;

                int target = Game.board[to];

                if (target == 0) {
                    moves.add(new Move(from, to));
                } else {
                    if ((whiteToMove && target < 0) || (!whiteToMove && target > 0)) {
                        moves.add(new Move(from, to));
                    }
                    break;
                }
            }
        }
    }

    static void generateQueenMoves(int from, List<Move> moves, boolean whiteToMove) {
        generateRookMoves(from, moves, whiteToMove);
        generateBishopMoves(from, moves, whiteToMove);
    }

    static void generateKingMoves(int from, List<Move> moves, boolean whiteToMove, boolean forCheckOnly) {
        for (int offset : Game.kingOffsets) {
            int to = from + offset;

            if ((to & 0x88) != 0) continue;

            int target = Game.board[to];
            if ((whiteToMove && target > 0) || (!whiteToMove && target < 0)) continue;

            moves.add(new Move(from, to));
        }

        if (forCheckOnly) return;

        // 🏰 HVID ROKADE
        if (whiteToMove && !Game.whiteKingMoved) {
            boolean rookOK = Game.board[7] == ROOK;
            boolean kingOK = Game.board[4] == KING;

            if (!Game.whiteKingsideRookMoved &&
                    Game.board[5] == 0 &&
                    Game.board[6] == 0 &&
                    kingOK && rookOK &&
                    !Game.isSquareAttacked(4, false) &&
                    !Game.isSquareAttacked(5, false) &&
                    !Game.isSquareAttacked(6, false)) {

                Move castle = new Move(from, 6); // g1
                castle.isCastleKingside = true;
                moves.add(castle);
                System.out.println("✅ Tilføjer hvid rokade: e1 -> g1 (O-O)");
            } else {
                System.out.println("❌ Rokade betingelser IKKE opfyldt");
            }
        }

        // 🏰 SORT ROKADE
        if (!whiteToMove && !Game.blackKingMoved) {
            System.out.println("👑 Tjekker sort rokade...");
            System.out.println("  blackKingsideRookMoved: " + Game.blackKingsideRookMoved);
            System.out.println("  board[116] (e8): " + Game.board[116]);
            System.out.println("  board[117] (f8): " + Game.board[117]);
            System.out.println("  board[118] (g8): " + Game.board[118]);
            System.out.println("  board[119] (h8): " + Game.board[119]);
            System.out.println("  e8 angrebet? " + Game.isSquareAttacked(116, true));
            System.out.println("  f8 angrebet? " + Game.isSquareAttacked(117, true));
            System.out.println("  g8 angrebet? " + Game.isSquareAttacked(118, true));

            boolean rookOK = Game.board[119] == -ROOK;
            boolean kingOK = Game.board[116] == -KING;

            if (!Game.blackKingsideRookMoved &&
                    Game.board[117] == 0 &&
                    Game.board[118] == 0 &&
                    kingOK && rookOK &&
                    !Game.isSquareAttacked(116, true) &&
                    !Game.isSquareAttacked(117, true) &&
                    !Game.isSquareAttacked(118, true)) {

                Move castle = new Move(from, 118);
                castle.isCastleKingside = true;
                moves.add(castle);
                System.out.println("✅ Tilføjer sort rokade: e8 -> g8 (O-O)");
            } else {
                System.out.println("❌ Sort rokade betingelser IKKE opfyldt");
            }
        }

    }

    static String squareToCoord(int square) {
        if (square < 0 || (square & 0x88) != 0) return "-";
        int file = square & 7;
        int rank = square >> 4;
        return "" + (char)('a' + file) + (rank + 1);
    }

    static List<Move> generateAllMoves() {
        return generateAllMoves(Game.isWhiteTurn, false);
    }

    static List<Move> generateAllMoves(boolean whiteToMove, boolean forCheckOnly) {
        List<Move> moves = new ArrayList<>();

        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) != 0) continue; // skip off-board
            int piece = Game.board[i];
            if (piece == 0) continue;

            if (whiteToMove && piece > 0) {
                switch (piece) {
                    case PAWN -> generatePawnMoves(i, moves, true);
                    case KNIGHT -> generateKnightMoves(i, moves, true);
                    case BISHOP -> generateBishopMoves(i, moves, true);
                    case ROOK -> generateRookMoves(i, moves, true);
                    case QUEEN -> generateQueenMoves(i, moves, true);
                    case KING -> {
                        int before = moves.size();
                        generateKingMoves(i, moves, true, forCheckOnly);
                        int after = moves.size();
                        if (!forCheckOnly && after > before) {
                            for (int j = before; j < after; j++) {
                                Move m = moves.get(j);
                                if (m.isCastleKingside || m.isCastleQueenside) {
                                    System.out.println("🏰 Rokade genereret for hvid: " + m);
                                }
                            }
                        }
                    }
                }
            } else if (!whiteToMove && piece < 0) {
                switch (Math.abs(piece)) {
                    case PAWN -> generatePawnMoves(i, moves, false);
                    case KNIGHT -> generateKnightMoves(i, moves, false);
                    case BISHOP -> generateBishopMoves(i, moves, false);
                    case ROOK -> generateRookMoves(i, moves, false);
                    case QUEEN -> generateQueenMoves(i, moves, false);
                    case KING -> {
                        int before = moves.size();
                        generateKingMoves(i, moves, false, forCheckOnly);
                        int after = moves.size();
                        if (!forCheckOnly && after > before) {
                            for (int j = before; j < after; j++) {
                                Move m = moves.get(j);
                                if (m.isCastleKingside || m.isCastleQueenside) {
                                    System.out.println("🏰 Rokade genereret for sort: " + m);
                                }
                            }
                        }
                    }
                }
            }
        }

        return moves;
    }
}
