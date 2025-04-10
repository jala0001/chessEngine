package com.example;

import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {
    // Piece constants
    static final int PAWN = 1;
    static final int KNIGHT = 2;
    static final int BISHOP = 3;
    static final int ROOK = 4;
    static final int QUEEN = 5;
    static final int KING = 6;

    // Direction offsets for movement
    static final int[] knightOffsets = { -33, -31, -18, -14, 14, 18, 31, 33 };
    static final int[] rookDirections = { -16, 16, -1, 1 };
    static final int[] bishopDirections = { -17, -15, 15, 17 };

    static boolean isOnBoard(int square) {
        return (square & 0x88) == 0;
    }

    static boolean isOpponentPiece(int piece, boolean whiteToMove) {
        return (whiteToMove && piece < 0) || (!whiteToMove && piece > 0);
    }

    static boolean isFriendlyPiece(int piece, boolean whiteToMove) {
        return (whiteToMove && piece > 0) || (!whiteToMove && piece < 0);
    }

    // Generate knight moves (L-shaped in 8 directions)
    static void generateKnightMoves(int from, List<Move> moves, boolean whiteToMove) {
        for (int offset : knightOffsets) {
            int to = from + offset;
            if (!isOnBoard(to)) continue;

            int target = Game.board[to];
            if (isFriendlyPiece(target, whiteToMove)) continue;

            moves.add(new Move(from, to));
        }
    }

    // Generate moves for sliding pieces (rooks, bishops, queens)
    static void generateSlidingMoves(int from, List<Move> moves, boolean whiteToMove, int[] directions) {
        for (int direction : directions) {
            int to = from;
            while (true) {
                to += direction;
                if (!isOnBoard(to)) break;

                int target = Game.board[to];
                if (target == 0) {
                    moves.add(new Move(from, to));
                } else {
                    if (isOpponentPiece(target, whiteToMove)) {
                        moves.add(new Move(from, to));
                    }
                    break;
                }
            }
        }
    }

    static void generateRookMoves(int from, List<Move> moves, boolean whiteToMove) {
        generateSlidingMoves(from, moves, whiteToMove, rookDirections);
    }

    static void generateBishopMoves(int from, List<Move> moves, boolean whiteToMove) {
        generateSlidingMoves(from, moves, whiteToMove, bishopDirections);
    }

    static void generateQueenMoves(int from, List<Move> moves, boolean whiteToMove) {
        generateSlidingMoves(from, moves, whiteToMove, rookDirections);
        generateSlidingMoves(from, moves, whiteToMove, bishopDirections);
    }

    // Helper for adding castling moves if conditions are met
    static void tryAddCastleMove(List<Move> moves, int from, int to, boolean white, boolean kingside) {
        int[] path = white ? (kingside ? new int[]{5, 6} : new int[]{1, 2, 3})
                : (kingside ? new int[]{117, 118} : new int[]{113, 114, 115});
        int rookSquare = white ? (kingside ? 7 : 0) : (kingside ? 119 : 112);
        int kingSquare = white ? 4 : 116;

        if (Game.board[rookSquare] != (white ? ROOK : -ROOK)) return;
        if (Game.board[kingSquare] != (white ? KING : -KING)) return;

        for (int sq : path) {
            if (Game.board[sq] != 0 || Game.isSquareAttacked(sq, !white)) return;
        }

        Move castle = new Move(from, to);
        if (kingside) castle.isCastleKingside = true;
        else castle.isCastleQueenside = true;
        moves.add(castle);
    }

    // Generate king moves including castling
    static void generateKingMoves(int from, List<Move> moves, boolean whiteToMove, boolean forCheckOnly) {
        for (int offset : Game.kingOffsets) {
            int to = from + offset;
            if (!isOnBoard(to)) continue;

            int target = Game.board[to];
            if (isFriendlyPiece(target, whiteToMove)) continue;

            moves.add(new Move(from, to));
        }

        if (forCheckOnly) return;

        if (whiteToMove && !Game.whiteKingMoved) {
            if (!Game.whiteKingsideRookMoved) tryAddCastleMove(moves, from, 6, true, true);
            if (!Game.whiteQueensideRookMoved) tryAddCastleMove(moves, from, 2, true, false);
        }
        if (!whiteToMove && !Game.blackKingMoved) {
            if (!Game.blackKingsideRookMoved) tryAddCastleMove(moves, from, 118, false, true);
            if (!Game.blackQueensideRookMoved) tryAddCastleMove(moves, from, 114, false, false);
        }
    }

    // Generate pawn moves (forward, captures, double move, en passant)
    static void generatePawnMoves(int from, List<Move> moves, boolean whiteToMove, boolean forCheckOnly) {
        int direction = whiteToMove ? 16 : -16;
        int startRank = whiteToMove ? 1 : 6;
        int epRank = whiteToMove ? 4 : 3;
        int[] captureOffsets = { direction - 1, direction + 1 };

        if (forCheckOnly) {
            for (int offset : captureOffsets) {
                int target = from + offset;
                if (!isOnBoard(target)) continue;

                int targetPiece = Game.board[target];
                if (isOpponentPiece(targetPiece, whiteToMove)) {
                    moves.add(new Move(from, target));
                }
            }
            return;
        }

        // Move one step forward
        int oneForward = from + direction;
        if (isOnBoard(oneForward) && Game.board[oneForward] == 0) {
            moves.add(new Move(from, oneForward));

            // Move two steps forward from starting rank
            int twoForward = from + 2 * direction;
            if ((from >> 4) == startRank && Game.board[twoForward] == 0) {
                moves.add(new Move(from, twoForward));
            }
        }

        // Normal captures
        for (int offset : captureOffsets) {
            int target = from + offset;
            if (!isOnBoard(target)) continue;

            int targetPiece = Game.board[target];
            if (isOpponentPiece(targetPiece, whiteToMove)) {
                moves.add(new Move(from, target));
            }
        }

        // En passant capture
        if ((from >> 4) == epRank && Game.enPassantSquare != -1) {
            for (int sideOffset : new int[]{-1, 1}) {
                int sideSquare = from + sideOffset;
                if (!isOnBoard(sideSquare)) continue;

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

    // Convert board index to coordinate string like "e2"
    static String squareToCoord(int square) {
        if (!isOnBoard(square)) return "-";
        int file = square & 7;
        int rank = square >> 4;
        return "" + (char)('a' + file) + (rank + 1);
    }

    // Main entry: generate all legal moves
    static List<Move> generateAllMoves() {
        return generateAllMoves(Game.isWhiteTurn, false);
    }

    // Generate all moves for current side
    static List<Move> generateAllMoves(boolean whiteToMove, boolean forCheckOnly) {
        List<Move> moves = new ArrayList<>();

        for (int i = 0; i < 128; i++) {
            if (!isOnBoard(i)) continue;
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
