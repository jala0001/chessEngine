package com.example;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static boolean isWhiteTurn = true;


    // 0x88 board (128 fields)
        static int[] board = new int[128];

        // Brikker: positive = hvid, negative = sort
        static final int PAWN = 1, KNIGHT = 2, BISHOP = 3, ROOK = 4, QUEEN = 5, KING = 6;

        static final int[] knightOffsets = { -33, -31, -18, -14, 14, 18, 31, 33 };
        static final int[] rookDirections = { -16, 16, -1, 1 };
        static final int[] bishopDirections = { -17, -15, 15, 17 };
        static final int[] kingOffsets = { -17, -16, -15, -1, 1, 15, 16, 17 };

        static int enPassantSquare = -1;

    static boolean whiteKingMoved = false;
    static boolean whiteKingsideRookMoved = false;
    static boolean whiteQueensideRookMoved = false;
    static boolean blackKingMoved = false;
    static boolean blackKingsideRookMoved = false;
    static boolean blackQueensideRookMoved = false;



    public static void main(String[] args) {
        loadFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        System.out.println("📍 board[4] (e1): " + board[4]);  // Hvid konge
        System.out.println("📍 board[63] (h1): " + board[63]);  // Hvid tårn
        System.out.println("📍 board[116] (e8): " + board[116]); // Sort konge
        System.out.println("📍 board[119] (h8): " + board[119]);
        System.out.println("🏁 Ved start: whiteKingMoved = " + whiteKingMoved);
        printBoard();
        System.out.println("🏁 EFTER PRINTBOARD: whiteKingMoved = " + whiteKingMoved);
        new ChessGUI();
        System.out.println("🏁 EFTER CHESSGUI: whiteKingMoved = " + whiteKingMoved);
        System.out.println("🏁 Slut: whiteKingMoved = " + whiteKingMoved);
    }

    static void printBoard() {
        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                int sq = rank * 16 + file; // ← korrekt for 0x88
                int piece = board[sq];
                System.out.printf("%2d ", piece);
            }
            System.out.println();
        }
        System.out.println();
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


    static void generateKnightMoves(int from, List<Move> moves, boolean whiteToMove) {
        for (int offset : knightOffsets) {
            int to = from + offset;

            // Brug 0x88-tricket til at tjekke om 'to' er gyldigt
            if ((to & 0x88) != 0) continue;

            int targetPiece = board[to];

            // Tjek om vi prøver at slå vores egen brik
            if (isWhiteTurn && targetPiece > 0) continue;
            if (!isWhiteTurn && targetPiece < 0) continue;

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

                int target = board[to];

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
        if ((oneForward & 0x88) == 0 && board[oneForward] == 0) {
            moves.add(new Move(from, oneForward));

            int twoForward = from + 2 * direction;
            if ((from >> 4) == startRank && board[twoForward] == 0) {
                moves.add(new Move(from, twoForward));
            }
        }

        int[] captureOffsets = { direction - 1, direction + 1 };
        for (int offset : captureOffsets) {
            int target = from + offset;
            if ((target & 0x88) != 0) continue;

            int targetPiece = board[target];
            if ((whiteToMove && targetPiece < 0) || (!whiteToMove && targetPiece > 0)) {
                moves.add(new Move(from, target));
            }
        }

        int epRank = whiteToMove ? 4 : 3;
        if ((from >> 4) == epRank && Main.enPassantSquare != -1) {
            int[] epOffsets = { -1, 1 };
            for (int sideOffset : epOffsets) {
                int sideSquare = from + sideOffset;
                if ((sideSquare & 0x88) != 0) continue;

                int sidePiece = board[sideSquare];
                if ((whiteToMove && sidePiece != -PAWN) || (!whiteToMove && sidePiece != PAWN)) {
                    continue;
                }

                int epTarget = from + direction + sideOffset;
                if (epTarget == Main.enPassantSquare) {
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

                int target = board[to];

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
        for (int offset : kingOffsets) {
            int to = from + offset;

            if ((to & 0x88) != 0) continue;

            int target = board[to];
            if ((whiteToMove && target > 0) || (!whiteToMove && target < 0)) continue;

            moves.add(new Move(from, to));
        }

        if (forCheckOnly) return;

        // 🏰 HVID ROKADE
        if (whiteToMove && !whiteKingMoved) {
            boolean rookOK = board[7] == ROOK;
            boolean kingOK = board[4] == KING;

            if (!whiteKingsideRookMoved &&
                    board[5] == 0 &&
                    board[6] == 0 &&
                    kingOK && rookOK &&
                    !Rules.isSquareAttacked(4, false) &&
                    !Rules.isSquareAttacked(5, false) &&
                    !Rules.isSquareAttacked(6, false)) {

                Move castle = new Move(from, 6); // g1
                castle.isCastleKingside = true;
                moves.add(castle);
                System.out.println("✅ Tilføjer hvid rokade: e1 -> g1 (O-O)");
            } else {
                System.out.println("❌ Rokade betingelser IKKE opfyldt");
            }
        }

        // 🏰 SORT ROKADE
        if (!whiteToMove && !blackKingMoved) {
            System.out.println("👑 Tjekker sort rokade...");
            System.out.println("  blackKingsideRookMoved: " + blackKingsideRookMoved);
            System.out.println("  board[116] (e8): " + board[116]);
            System.out.println("  board[117] (f8): " + board[117]);
            System.out.println("  board[118] (g8): " + board[118]);
            System.out.println("  board[119] (h8): " + board[119]);
            System.out.println("  e8 angrebet? " + Rules.isSquareAttacked(116, true));
            System.out.println("  f8 angrebet? " + Rules.isSquareAttacked(117, true));
            System.out.println("  g8 angrebet? " + Rules.isSquareAttacked(118, true));

            boolean rookOK = board[119] == -ROOK;
            boolean kingOK = board[116] == -KING;

            if (!blackKingsideRookMoved &&
                    board[117] == 0 &&
                    board[118] == 0 &&
                    kingOK && rookOK &&
                    !Rules.isSquareAttacked(116, true) &&
                    !Rules.isSquareAttacked(117, true) &&
                    !Rules.isSquareAttacked(118, true)) {

                Move castle = new Move(from, 118);
                castle.isCastleKingside = true;
                moves.add(castle);
                System.out.println("✅ Tilføjer sort rokade: e8 -> g8 (O-O)");
            } else {
                System.out.println("❌ Sort rokade betingelser IKKE opfyldt");
            }
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
        if (Math.abs(movedPiece) == PAWN && Math.abs(move.to - move.from) == 32) {
            enPassantSquare = (move.from + move.to) / 2;
        } else {
            enPassantSquare = -1;
        }

        // Opdater konge-flytning
        if (Math.abs(movedPiece) == KING) {
            if (movedPiece > 0) whiteKingMoved = true;
            else blackKingMoved = true;
        }

        // Rokade – flyt tårn
        if (move.isCastleKingside) {
            if (move.from == 4 && move.to == 6) { // hvid
                board[7] = 0;
                board[5] = ROOK;
            } else if (move.from == 116 && move.to == 118) { // sort
                board[119] = 0;
                board[117] = -ROOK;
            }
        }
        if (move.isCastleQueenside) {
            if (move.from == 4 && move.to == 2) { // hvid
                board[0] = 0;
                board[3] = ROOK;
            } else if (move.from == 116 && move.to == 114) { // sort
                board[112] = 0;
                board[115] = -ROOK;
            }
        }

        // Opdater tårn-flyt flags
        if (movedPiece == ROOK) {
            if (move.from == 0) whiteQueensideRookMoved = true;
            if (move.from == 7) whiteKingsideRookMoved = true;
        }
        if (movedPiece == -ROOK) {
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



    static String squareToCoord(int square) {
        if (square < 0 || (square & 0x88) != 0) return "-";
        int file = square & 7;
        int rank = square >> 4;
        return "" + (char)('a' + file) + (rank + 1);
    }

    static List<Move> generateAllMoves() {
        return generateAllMoves(isWhiteTurn, false);
    }


    static List<Move> generateAllMoves(boolean whiteToMove, boolean forCheckOnly) {
        List<Move> moves = new ArrayList<>();

        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) != 0) continue; // skip off-board
            int piece = board[i];
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