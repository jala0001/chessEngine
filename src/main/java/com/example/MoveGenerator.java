package com.example;

import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {
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
            if ((to & 0x88) != 0) continue;

            int targetPiece = Game.board[to];
            if ((whiteToMove && targetPiece > 0) || (!whiteToMove && targetPiece < 0)) continue;

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

        if (whiteToMove && !Game.whiteKingMoved) {
            if (!Game.whiteKingsideRookMoved &&
                    Game.board[5] == 0 && Game.board[6] == 0 &&
                    Game.board[4] == KING && Game.board[7] == ROOK &&
                    !Game.isSquareAttacked(4, false) &&
                    !Game.isSquareAttacked(5, false) &&
                    !Game.isSquareAttacked(6, false)) {

                Move castle = new Move(from, 6);
                castle.isCastleKingside = true;
                moves.add(castle);
            }

            if (!Game.whiteQueensideRookMoved &&
                    Game.board[1] == 0 && Game.board[2] == 0 && Game.board[3] == 0 &&
                    Game.board[0] == ROOK && Game.board[4] == KING &&
                    !Game.isSquareAttacked(2, false) &&
                    !Game.isSquareAttacked(3, false) &&
                    !Game.isSquareAttacked(4, false)) {

                Move castle = new Move(from, 2);
                castle.isCastleQueenside = true;
                moves.add(castle);
            }
        }

        if (!whiteToMove && !Game.blackKingMoved) {
            if (!Game.blackKingsideRookMoved &&
                    Game.board[117] == 0 && Game.board[118] == 0 &&
                    Game.board[116] == -KING && Game.board[119] == -ROOK &&
                    !Game.isSquareAttacked(116, true) &&
                    !Game.isSquareAttacked(117, true) &&
                    !Game.isSquareAttacked(118, true)) {

                Move castle = new Move(from, 118);
                castle.isCastleKingside = true;
                moves.add(castle);
            }

            if (!Game.blackQueensideRookMoved &&
                    Game.board[113] == 0 && Game.board[114] == 0 && Game.board[115] == 0 &&
                    Game.board[112] == -ROOK && Game.board[116] == -KING &&
                    !Game.isSquareAttacked(114, true) &&
                    !Game.isSquareAttacked(115, true) &&
                    !Game.isSquareAttacked(116, true)) {

                Move castle = new Move(from, 114);
                castle.isCastleQueenside = true;
                moves.add(castle);
            }
        }
    }

    static void generatePawnMoves(int from, List<Move> moves, boolean whiteToMove, boolean forCheckOnly) {
        int direction = whiteToMove ? 16 : -16;
        int startRank = whiteToMove ? 1 : 6;

        if (forCheckOnly) {
            int[] captureOffsets = { direction - 1, direction + 1 };
            for (int offset : captureOffsets) {
                int target = from + offset;
                if ((target & 0x88) != 0) continue;
                int targetPiece = Game.board[target];
                if ((whiteToMove && targetPiece < 0) || (!whiteToMove && targetPiece > 0)) {
                    moves.add(new Move(from, target));
                }
            }
            return;
        }

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
                if ((whiteToMove && sidePiece != -PAWN) || (!whiteToMove && sidePiece != PAWN)) continue;

                int epTarget = from + direction + sideOffset;
                if (epTarget == Game.enPassantSquare) {
                    Move epMove = new Move(from, epTarget);
                    epMove.isEnPassant = true;
                    moves.add(epMove);
                }
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
            if ((i & 0x88) != 0) continue;
            int piece = Game.board[i];
            if (piece == 0) continue;

            if (whiteToMove && piece > 0) {
                switch (piece) {
                    case PAWN -> generatePawnMoves(i, moves, true, forCheckOnly);
                    case KNIGHT -> generateKnightMoves(i, moves, true);
                    case BISHOP -> generateBishopMoves(i, moves, true);
                    case ROOK -> generateRookMoves(i, moves, true);
                    case QUEEN -> generateQueenMoves(i, moves, true);
                    case KING -> generateKingMoves(i, moves, true, forCheckOnly);
                }
            } else if (!whiteToMove && piece < 0) {
                switch (Math.abs(piece)) {
                    case PAWN -> generatePawnMoves(i, moves, false, forCheckOnly);
                    case KNIGHT -> generateKnightMoves(i, moves, false);
                    case BISHOP -> generateBishopMoves(i, moves, false);
                    case ROOK -> generateRookMoves(i, moves, false);
                    case QUEEN -> generateQueenMoves(i, moves, false);
                    case KING -> generateKingMoves(i, moves, false, forCheckOnly);
                }
            }
        }

        return moves;
    }
}

