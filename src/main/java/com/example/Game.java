package com.example;

import java.util.ArrayList;
import java.util.List;

public class Game {

    public static final int[] kingOffsets = { -17, -16, -15, -1, 1, 15, 16, 17 };
    public static boolean isWhiteTurn = true;

    public static int[] board = new int[128];
    public static int enPassantSquare = -1;
    public static boolean whiteKingMoved = false;
    public static boolean whiteKingsideRookMoved = false;
    public static boolean whiteQueensideRookMoved = false;
    public static boolean blackKingMoved = false;
    public static boolean blackKingsideRookMoved = false;
    public static boolean blackQueensideRookMoved = false;

    static boolean isInCheck() {
        boolean whiteToCheck = !isWhiteTurn;  // <- den spiller der lige HAR flyttet
        int kingSquare = -1;
        int kingValue = whiteToCheck ? MoveGenerator.KING : -MoveGenerator.KING;

        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) != 0) continue;
            if (Game.board[i] == kingValue) {
                kingSquare = i;
                break;
            }
        }

        if (kingSquare == -1) return true;

        return isSquareAttacked(kingSquare, !whiteToCheck);
    }



    public static List<Move> generateLegalMoves() {
        boolean whiteToMove = isWhiteTurn;
        List<Move> legalMoves = new ArrayList<>();
        List<Move> pseudoMoves = MoveGenerator.generateAllMoves(whiteToMove, false);

        for (Move move : pseudoMoves) {
            Move testMove = new Move(move);
            int originalEnPassant = enPassantSquare;

            int captured = makeMove(testMove);
            boolean stillLegal = !isInCheck();
            undoMove(testMove, captured);

            enPassantSquare = originalEnPassant;

            if (stillLegal) {
                legalMoves.add(move);
            }
        }

        int kingSquare = whiteToMove ? 4 : 116;
        if (canCastleKingside(whiteToMove)) {
            Move m = new Move(kingSquare, whiteToMove ? 6 : 118);
            m.isCastleKingside = true;
            legalMoves.add(m);
        }
        if (canCastleQueenside(whiteToMove)) {
            Move m = new Move(kingSquare, whiteToMove ? 2 : 114);
            m.isCastleQueenside = true;
            legalMoves.add(m);
        }

        return legalMoves;
    }

    private static boolean canCastleKingside(boolean white) {
        if (white) {
            return !whiteKingMoved && !whiteKingsideRookMoved &&
                    board[5] == 0 && board[6] == 0 &&
                    board[4] == MoveGenerator.KING && board[7] == MoveGenerator.ROOK &&
                    !isSquareAttacked(4, false) &&
                    !isSquareAttacked(5, false) &&
                    !isSquareAttacked(6, false);
        } else {
            return !blackKingMoved && !blackKingsideRookMoved &&
                    board[117] == 0 && board[118] == 0 &&
                    board[116] == -MoveGenerator.KING && board[119] == -MoveGenerator.ROOK &&
                    !isSquareAttacked(116, true) &&
                    !isSquareAttacked(117, true) &&
                    !isSquareAttacked(118, true);
        }
    }

    private static boolean canCastleQueenside(boolean white) {
        if (white) {
            return !whiteKingMoved && !whiteQueensideRookMoved &&
                    board[1] == 0 && board[2] == 0 && board[3] == 0 &&
                    board[4] == MoveGenerator.KING && board[0] == MoveGenerator.ROOK &&
                    !isSquareAttacked(2, false) &&
                    !isSquareAttacked(3, false) &&
                    !isSquareAttacked(4, false);
        } else {
            return !blackKingMoved && !blackQueensideRookMoved &&
                    board[113] == 0 && board[114] == 0 && board[115] == 0 &&
                    board[116] == -MoveGenerator.KING && board[112] == -MoveGenerator.ROOK &&
                    !isSquareAttacked(114, true) &&
                    !isSquareAttacked(115, true) &&
                    !isSquareAttacked(116, true);
        }
    }

    public static boolean isSquareAttacked(int square, boolean byWhite) {
        int enemyPawn = byWhite ? MoveGenerator.PAWN : -MoveGenerator.PAWN;
        int enemyKnight = byWhite ? MoveGenerator.KNIGHT : -MoveGenerator.KNIGHT;
        int enemyBishop = byWhite ? MoveGenerator.BISHOP : -MoveGenerator.BISHOP;
        int enemyRook = byWhite ? MoveGenerator.ROOK : -MoveGenerator.ROOK;
        int enemyQueen = byWhite ? MoveGenerator.QUEEN : -MoveGenerator.QUEEN;
        int enemyKing = byWhite ? MoveGenerator.KING : -MoveGenerator.KING;

        // 🟨 1. Bondeangreb
        int direction = byWhite ? -16 : 16;
        int[] pawnCaptures = { direction - 1, direction + 1 };
        for (int offset : pawnCaptures) {
            int target = square + offset;
            if ((target & 0x88) == 0 && Game.board[target] == enemyPawn) {
                return true;
            }
        }

        // ♞ 2. Springer
        for (int offset : MoveGenerator.knightOffsets) {
            int target = square + offset;
            if ((target & 0x88) == 0 && Game.board[target] == enemyKnight) {
                return true;
            }
        }

        // ♝♛ 3. Diagonal løber/dronning
        for (int offset : MoveGenerator.bishopDirections) {
            int target = square;
            while (true) {
                target += offset;
                if ((target & 0x88) != 0) break;
                int piece = Game.board[target];
                if (piece == 0) continue;
                if (piece == enemyBishop || piece == enemyQueen) return true;
                break;
            }
        }

        // ♜♛ 4. Lige tårn/dronning
        for (int offset : MoveGenerator.rookDirections) {
            int target = square;
            while (true) {
                target += offset;
                if ((target & 0x88) != 0) break;
                int piece = Game.board[target];
                if (piece == 0) continue;
                if (piece == enemyRook || piece == enemyQueen) return true;
                break;
            }
        }

        // 👑 5. Konge – 1 felt omkring
        for (int offset : Game.kingOffsets) {
            int target = square + offset;
            if ((target & 0x88) == 0 && Game.board[target] == enemyKing) {
                return true;
            }
        }

        return false;
    }


    public static void loadFEN(String fen) {
        String[] parts = fen.split(" ");
        String boardPart = parts[0];

        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) == 0) board[i] = 0;
        }

        int i = 0;
        for (int rank = 7; rank >= 0; rank--) {
            int file = 0;
            while (file < 8) {
                char c = boardPart.charAt(i++);
                if (c == '/') continue;

                int sq = rank * 16 + file;

                if (Character.isDigit(c)) {
                    file += c - '0';
                } else {
                    board[sq] = pieceFromChar(c);
                    file++;
                }
            }
        }

        isWhiteTurn = parts[1].equals("w");

        String castlingRights = parts[2];
        whiteKingsideRookMoved = !castlingRights.contains("K");
        whiteQueensideRookMoved = !castlingRights.contains("Q");
        blackKingsideRookMoved = !castlingRights.contains("k");
        blackQueensideRookMoved = !castlingRights.contains("q");
        whiteKingMoved = whiteKingsideRookMoved && whiteQueensideRookMoved;
        blackKingMoved = blackKingsideRookMoved && blackQueensideRookMoved;

        if (parts.length > 3 && !parts[3].equals("-")) {
            String ep = parts[3];
            int file = ep.charAt(0) - 'a';
            int rank = Character.getNumericValue(ep.charAt(1)) - 1;
            enPassantSquare = rank * 16 + file;
        } else {
            enPassantSquare = -1;
        }
    }

    private static int pieceFromChar(char c) {
        return switch (c) {
            case 'K' -> 6;
            case 'Q' -> 5;
            case 'R' -> 4;
            case 'B' -> 3;
            case 'N' -> 2;
            case 'P' -> 1;
            case 'k' -> -6;
            case 'q' -> -5;
            case 'r' -> -4;
            case 'b' -> -3;
            case 'n' -> -2;
            case 'p' -> -1;
            default -> 0;
        };
    }

    public static int makeMove(Move move) {
        int movedPiece = board[move.from];
        int captured;

        move.prevWhiteKingMoved = whiteKingMoved;
        move.prevBlackKingMoved = blackKingMoved;
        move.prevWhiteKingsideRookMoved = whiteKingsideRookMoved;
        move.prevWhiteQueensideRookMoved = whiteQueensideRookMoved;
        move.prevBlackKingsideRookMoved = blackKingsideRookMoved;
        move.prevBlackQueensideRookMoved = blackQueensideRookMoved;
        move.prevEnPassantSquare = enPassantSquare;

        if (move.isEnPassant) {
            int epPawnSquare = move.to + (movedPiece > 0 ? -16 : 16);
            captured = board[epPawnSquare];
          //  System.out.println("🔥 En passant! Fjerner fjendtlig bonde på: " + MoveGenerator.squareToCoord(epPawnSquare));
          //  System.out.println("   Bonden flyttes fra " + MoveGenerator.squareToCoord(move.from) + " til " + MoveGenerator.squareToCoord(move.to));
            board[epPawnSquare] = 0;
        } else {
            captured = board[move.to];
        }

        board[move.to] = movedPiece;
        board[move.from] = 0;

        if (Math.abs(movedPiece) == MoveGenerator.PAWN && Math.abs(move.to - move.from) == 32) {
            enPassantSquare = (move.from + move.to) / 2;
        } else {
            enPassantSquare = -1;
        }

        if (Math.abs(movedPiece) == MoveGenerator.KING) {
            if (movedPiece > 0) whiteKingMoved = true;
            else blackKingMoved = true;
        }

        if (move.isCastleKingside) {
            if (move.from == 4 && move.to == 6) {
                board[7] = 0;
                board[5] = MoveGenerator.ROOK;
            } else if (move.from == 116 && move.to == 118) {
                board[119] = 0;
                board[117] = -MoveGenerator.ROOK;
            }
        }
        if (move.isCastleQueenside) {
            if (move.from == 4 && move.to == 2) {
                board[0] = 0;
                board[3] = MoveGenerator.ROOK;
            } else if (move.from == 116 && move.to == 114) {
                board[112] = 0;
                board[115] = -MoveGenerator.ROOK;
            }
        }

        if (movedPiece == MoveGenerator.ROOK) {
            if (move.from == 0) whiteQueensideRookMoved = true;
            if (move.from == 7) whiteKingsideRookMoved = true;
        }
        if (movedPiece == -MoveGenerator.ROOK) {
            if (move.from == 112) blackQueensideRookMoved = true;
            if (move.from == 119) blackKingsideRookMoved = true;
        }

        isWhiteTurn = !isWhiteTurn;


        return captured;
    }

    public static void undoMove(Move move, int captured) {
        board[move.from] = board[move.to];
        board[move.to] = 0;

        if (move.isEnPassant) {
            int epPawnSquare = move.to + (board[move.from] > 0 ? -16 : 16);
            board[epPawnSquare] = captured;
        } else {
            board[move.to] = captured;
        }

        if (move.isCastleKingside) {
            if (move.to == 6) {
                board[7] = board[5];
                board[5] = 0;
            } else if (move.to == 118) {
                board[119] = board[117];
                board[117] = 0;
            }
        }
        if (move.isCastleQueenside) {
            if (move.to == 2) {
                board[0] = board[3];
                board[3] = 0;
            } else if (move.to == 114) {
                board[112] = board[115];
                board[115] = 0;
            }
        }

        whiteKingMoved = move.prevWhiteKingMoved;
        blackKingMoved = move.prevBlackKingMoved;
        whiteKingsideRookMoved = move.prevWhiteKingsideRookMoved;
        whiteQueensideRookMoved = move.prevWhiteQueensideRookMoved;
        blackKingsideRookMoved = move.prevBlackKingsideRookMoved;
        blackQueensideRookMoved = move.prevBlackQueensideRookMoved;
        enPassantSquare = move.prevEnPassantSquare;
        isWhiteTurn = !isWhiteTurn;
    }

    public static boolean isStalemate() {
        return generateLegalMoves().isEmpty() && !isInCheck();
    }

    public static boolean isCheckmate() {
        return generateLegalMoves().isEmpty() && isInCheck();
    }

    public static boolean isDrawByStalemate() {
        return generateLegalMoves().isEmpty() && !isInCheck();
    }


}