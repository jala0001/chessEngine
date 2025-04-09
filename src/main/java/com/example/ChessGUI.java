// ChessGUI.java
package com.example;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

class ChessGUI extends JFrame {
    static final int TILE_SIZE = 64;
    int selectedSquare = -1;
    JPanel boardPanel = new JPanel(new GridLayout(8, 8));

    ChessGUI() {
        setTitle("Skak!");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(8 * TILE_SIZE, 8 * TILE_SIZE);
        setLocationRelativeTo(null);

        drawBoard();
        add(boardPanel);
        setVisible(true);
    }

    void handleClick(int square) {
        System.out.println("🔍 Klik på: " + square + " = " + MoveGenerator.squareToCoord(square));
        if ((Game.isWhiteTurn && Game.board[square] < 0) ||
                (!Game.isWhiteTurn && Game.board[square] > 0)) {
            System.out.println("❌ Det er ikke din tur – ignorer klik.");
            return;
        }
        int piece = Game.board[square];

        if (selectedSquare == -1) {
            if (piece != 0 && ((Game.isWhiteTurn && piece > 0) || (!Game.isWhiteTurn && piece < 0))) {
                selectedSquare = square;
                System.out.println("🖱️ Valgte: " + MoveGenerator.squareToCoord(square));
            }
        } else {
            List<Move> allMoves = Game.generateLegalMoves();

            System.out.println("📋 Genererede lovlige træk:");
            for (Move m : allMoves) {
                System.out.println(" - " + m);
            }

            List<Move> legalMovesFromSelected = new ArrayList<>();
            for (Move move : allMoves) {
                if (move.from == selectedSquare) {
                    legalMovesFromSelected.add(move);
                }
            }

            System.out.println("🎯 Træk fra valgte felt (" + MoveGenerator.squareToCoord(selectedSquare) + "):");
            for (Move m : legalMovesFromSelected) {
                System.out.println(" → " + m);
            }

            for (Move move : legalMovesFromSelected) {
                if (move.to == square) {
                    System.out.println("⬅️ " + (Game.isWhiteTurn ? "Hvid" : "Sort") + " udfører træk: " + move);

                    if (move.isEnPassant) {
                        System.out.println("🔥 En passant bliver udført!");
                    }
                    if (move.isCastleKingside) {
                        System.out.println("🏰 Kongeside rokade!");
                    }
                    if (move.isCastleQueenside) {
                        System.out.println("🏰 Dronningeside rokade!");
                    }

                    int captured = Game.makeMove(move);
                    Game.isWhiteTurn = !Game.isWhiteTurn;
                    System.out.println("➡️ Nu er det " + (Game.isWhiteTurn ? "hvid" : "sort") + "s tur");

                    // Promotion
                    int movedPiece = Game.board[move.to];
                    if (Math.abs(movedPiece) == MoveGenerator.PAWN) {
                        int rank = move.to >> 4;
                        if ((rank == 7 && movedPiece > 0) || (rank == 0 && movedPiece < 0)) {
                            String[] options = { "Dronning", "Tårn", "Løber", "Springer" };
                            int choice = JOptionPane.showOptionDialog(this, "Vælg forvandling",
                                    "Bonde forvandles", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                                    null, options, options[0]);

                            int newPiece = switch (choice) {
                                case 1 -> MoveGenerator.ROOK;
                                case 2 -> MoveGenerator.BISHOP;
                                case 3 -> MoveGenerator.KNIGHT;
                                default -> MoveGenerator.QUEEN;
                            };
                            Game.board[move.to] = movedPiece > 0 ? newPiece : -newPiece;
                            System.out.println("♛ Promotion til: " + options[choice]);
                        }
                    }

                    // Skakmat/patt
                    List<Move> nextMoves = Game.generateLegalMoves();
                    if (nextMoves.isEmpty()) {
                        if (Game.isInCheck()) {
                            JOptionPane.showMessageDialog(this, (Game.isWhiteTurn ? "Hvid" : "Sort") + " er mat!");
                        } else {
                            JOptionPane.showMessageDialog(this, "Patt! Uafgjort.");
                        }
                    }

                    break;
                }
            }

            selectedSquare = -1;
            drawBoard();
        }
    }




    void drawBoard() {
        boardPanel.removeAll();

        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                int square = rank * 16 + file;
                JButton tile = new JButton(getPieceSymbol(Game.board[square]));
                tile.setFont(new Font("Arial", Font.PLAIN, 32));
                tile.setBackground((rank + file) % 2 == 0 ? Color.WHITE : Color.GRAY);

                final int clickedSquare = square;
                tile.addActionListener(e -> handleClick(clickedSquare));

                boardPanel.add(tile);
            }
        }

        boardPanel.revalidate();
        boardPanel.repaint();
    }

    String getPieceSymbol(int piece) {
        return switch (piece) {
            case MoveGenerator.PAWN -> "♙";
            case MoveGenerator.KNIGHT -> "♘";
            case MoveGenerator.BISHOP -> "♗";
            case MoveGenerator.ROOK -> "♖";
            case MoveGenerator.QUEEN -> "♕";
            case MoveGenerator.KING -> "♔";
            case -MoveGenerator.PAWN -> "♟";
            case -MoveGenerator.KNIGHT -> "♞";
            case -MoveGenerator.BISHOP -> "♝";
            case -MoveGenerator.ROOK -> "♜";
            case -MoveGenerator.QUEEN -> "♛";
            case -MoveGenerator.KING -> "♚";
            default -> "";
        };
    }
}
