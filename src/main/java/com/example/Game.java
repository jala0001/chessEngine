package com.example;

import java.util.ArrayList;
import java.util.List;

public class Game {


    static final int[] kingOffsets = { -17, -16, -15, -1, 1, 15, 16, 17 };
    public static boolean isWhiteTurn = true;
    // 0x88 board (128 fields)
        static int[] board = new int[128];
    static int enPassantSquare = -1;
    static boolean whiteKingMoved = false;
    static boolean whiteKingsideRookMoved = false;
    static boolean whiteQueensideRookMoved = false;
    static boolean blackKingMoved = false;
    static boolean blackKingsideRookMoved = false;
    static boolean blackQueensideRookMoved = false;

    public static boolean isInCheck() {
        boolean whiteToMove = isWhiteTurn;
        int kingSquare = -1;
        int kingValue = whiteToMove ? MoveGenerator.KING : -MoveGenerator.KING;

        // Find kongens position
        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) != 0) continue;
            if (board[i] == kingValue) {
                kingSquare = i;
                break;
            }
        }

        // Hvis kongen ikke findes, antag skakmat
        if (kingSquare == -1) return true;

        // Tjek om nogen modstandertræk går til kongens felt
        List<Move> enemyMoves = MoveGenerator.generateAllMoves(!whiteToMove, true);
        for (Move move : enemyMoves) {
            if (move.to == kingSquare) return true;
        }

        return false;
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
        return legalMoves;
    }


    /**
     * Tjekker om et felt er truet af den givne farve.
     *
     * @param square feltet vi vil tjekke (0x88)
     * @param byWhite true hvis vi vil tjekke om hvid truer feltet
     * @return true hvis feltet er truet
     */
    public static boolean isSquareAttacked(int square, boolean byWhite) {
        List<Move> moves = MoveGenerator.generateAllMoves(byWhite, true);
        for (Move move : moves) {
            if (move.to == square) return true;
        }
        return false;
    }

    static void loadFEN(String fen) {
        System.out.println("📦 FEN modtaget: " + fen);
        String[] parts = fen.split(" ");
        String boardPart = parts[0];

        // Nulstil brættet
        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) == 0) board[i] = 0;  // Udelader de "usynlige" felter
        }

        int i = 0; // index i FEN-stringen
        for (int rank = 7; rank >= 0; rank--) {
            int file = 0;
            while (file < 8) {
                char c = boardPart.charAt(i++);
                if (c == '/') continue;

                int sq = rank * 16 + file; // Mapper til 0x88-indekset

                if (Character.isDigit(c)) {
                    file += c - '0'; // Håndter tomme felter
                } else {
                    board[sq] = pieceFromChar(c); // Placer brikken
                    file++;
                }
            }
        }

        isWhiteTurn = parts[1].equals("w");
        System.out.println("🔄 Tur efter FEN: " + (isWhiteTurn ? "hvid" : "sort"));

        // Rokade-rettigheder
        String castlingRights = parts[2];
        whiteKingsideRookMoved = !castlingRights.contains("K");
        whiteQueensideRookMoved = !castlingRights.contains("Q");
        blackKingsideRookMoved = !castlingRights.contains("k");
        blackQueensideRookMoved = !castlingRights.contains("q");
        whiteKingMoved = whiteKingsideRookMoved && whiteQueensideRookMoved;
        blackKingMoved = blackKingsideRookMoved && blackQueensideRookMoved;

        System.out.println("♔ Rokade rettigheder:");
        System.out.println("  whiteKingMoved: " + whiteKingMoved);
        System.out.println("  whiteKingsideRookMoved: " + whiteKingsideRookMoved);
        System.out.println("  whiteQueensideRookMoved: " + whiteQueensideRookMoved);
        System.out.println("  blackKingMoved: " + blackKingMoved);
        System.out.println("  blackKingsideRookMoved: " + blackKingsideRookMoved);
        System.out.println("  blackQueensideRookMoved: " + blackQueensideRookMoved);

        // En passant
        if (parts.length > 3 && !parts[3].equals("-")) {
            String ep = parts[3];
            int file = ep.charAt(0) - 'a';
            int rank = Character.getNumericValue(ep.charAt(1)) - 1;
            enPassantSquare = rank * 16 + file;
            System.out.println("🎯 En passant-felt: " + ep);
        } else {
            enPassantSquare = -1;
            System.out.println("🎯 En passant-felt: -");
        }
    }

    // Map FEN-char til vores integer-brikrepræsentation
    static int pieceFromChar(char c) {
        switch (c) {
            case 'K': return 6;
            case 'Q': return 5;
            case 'R': return 4;
            case 'B': return 3;
            case 'N': return 2;
            case 'P': return 1;
            case 'k': return -6;
            case 'q': return -5;
            case 'r': return -4;
            case 'b': return -3;
            case 'n': return -2;
            case 'p': return -1;
            default:
                System.out.println("🚨 Ukendt tegn i FEN: " + c);
                return 0;
        }
    }

    static int makeMove(Move move) {
        int movedPiece = board[move.from];
        int captured;

        // Gem tidligere flags
        move.prevWhiteKingMoved = whiteKingMoved;
        move.prevBlackKingMoved = blackKingMoved;
        move.prevWhiteKingsideRookMoved = whiteKingsideRookMoved;
        move.prevWhiteQueensideRookMoved = whiteQueensideRookMoved;
        move.prevBlackKingsideRookMoved = blackKingsideRookMoved;
        move.prevBlackQueensideRookMoved = blackQueensideRookMoved;
        move.prevEnPassantSquare = enPassantSquare;

        // Håndter en passant
        if (move.isEnPassant) {
            int epPawnSquare = move.to + (movedPiece > 0 ? -16 : 16);
            captured = board[epPawnSquare];
            board[epPawnSquare] = 0;
        } else {
            captured = board[move.to];
        }

        // Flyt brikken
        board[move.to] = movedPiece;
        board[move.from] = 0;

        // Hvis bonde laver dobbelt spring
        if (Math.abs(movedPiece) == MoveGenerator.PAWN && Math.abs(move.to - move.from) == 32) {
            enPassantSquare = (move.from + move.to) / 2;
        } else {
            enPassantSquare = -1;
        }

        // Opdater konge-flytning
        if (Math.abs(movedPiece) == MoveGenerator.KING) {
            if (movedPiece > 0) whiteKingMoved = true;
            else blackKingMoved = true;
        }

        // Rokade – flyt tårn
        if (move.isCastleKingside) {
            if (move.from == 4 && move.to == 6) { // hvid
                board[7] = 0;
                board[5] = MoveGenerator.ROOK;
            } else if (move.from == 116 && move.to == 118) { // sort
                board[119] = 0;
                board[117] = -MoveGenerator.ROOK;
            }
        }
        if (move.isCastleQueenside) {
            if (move.from == 4 && move.to == 2) { // hvid
                board[0] = 0;
                board[3] = MoveGenerator.ROOK;
            } else if (move.from == 116 && move.to == 114) { // sort
                board[112] = 0;
                board[115] = -MoveGenerator.ROOK;
            }
        }

        // Opdater tårn-flyt flags
        if (movedPiece == MoveGenerator.ROOK) {
            if (move.from == 0) whiteQueensideRookMoved = true;
            if (move.from == 7) whiteKingsideRookMoved = true;
        }
        if (movedPiece == -MoveGenerator.ROOK) {
            if (move.from == 112) blackQueensideRookMoved = true;
            if (move.from == 119) blackKingsideRookMoved = true;
        }

        return captured;
    }

    static void undoMove(Move move, int captured) {
        board[move.from] = board[move.to];
        board[move.to] = 0;

        // Gendan en passant-offer
        if (move.isEnPassant) {
            int epPawnSquare = move.to + (board[move.from] > 0 ? -16 : 16);
            board[epPawnSquare] = captured;
        } else {
            board[move.to] = captured;
        }

        // Gendan rokade
        if (move.isCastleKingside) {
            if (move.to == 6) { // hvid
                board[7] = board[5];
                board[5] = 0;
            } else if (move.to == 118) { // sort
                board[119] = board[117];
                board[117] = 0;
            }
        }
        if (move.isCastleQueenside) {
            if (move.to == 2) { // hvid
                board[0] = board[3];
                board[3] = 0;
            } else if (move.to == 114) { // sort
                board[112] = board[115];
                board[115] = 0;
            }
        }

        // Gendan flags
        whiteKingMoved = move.prevWhiteKingMoved;
        blackKingMoved = move.prevBlackKingMoved;
        whiteKingsideRookMoved = move.prevWhiteKingsideRookMoved;
        whiteQueensideRookMoved = move.prevWhiteQueensideRookMoved;
        blackKingsideRookMoved = move.prevBlackKingsideRookMoved;
        blackQueensideRookMoved = move.prevBlackQueensideRookMoved;
        enPassantSquare = move.prevEnPassantSquare;
    }

}

