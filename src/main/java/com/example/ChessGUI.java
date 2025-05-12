// ChessGUI.java
package com.example;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.border.LineBorder;


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
        int piece = Game.board[square];

        if (selectedSquare == -1) {
            // Kun tjek tur når spiller vælger brik
            if ((Game.isWhiteTurn && piece > 0 && Game.playerIsWhite) ||
                    (!Game.isWhiteTurn && piece < 0 && !Game.playerIsWhite)) {
                selectedSquare = square;
                drawBoard(); // Opdater GUI for at vise markering
            }
        } else {
            List<Move> allMoves = Game.generateLegalMoves();

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
                    String playerName = Game.playerIsWhite ? "Hvid" : "Sort";
                    String playerType = Game.isWhiteTurn == Game.playerIsWhite ? "mennesket" : "AI'en";
                    System.out.println("⬅️ " + playerName + " (" + playerType + ") udfører træk: " + move);

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
                    System.out.println("➡️ Nu er det " + (Game.isWhiteTurn ? "hvid" : "sort") + "'s tur");

                    // Promotion - kun for spillerens bønder
                    int movedPiece = Game.board[move.to];
                    if (Math.abs(movedPiece) == MoveGenerator.PAWN) {
                        int rank = move.to >> 4;
                        boolean shouldPromote = (Game.playerIsWhite && movedPiece > 0 && rank == 7) ||
                                (!Game.playerIsWhite && movedPiece < 0 && rank == 0);

                        if (shouldPromote) {
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

                            // Sæt korrekt farve på det promoverede stykke
                            Game.board[move.to] = Game.playerIsWhite ? newPiece : -newPiece;
                            System.out.println("♛ Promotion til: " + options[choice]);
                        }
                    }

                    // Skakmat/patt tjek
                    List<Move> nextMoves = Game.generateLegalMoves();
                    if (nextMoves.isEmpty()) {
                        if (Game.isCurrentPlayerInCheck()) {
                            String winner = Game.isWhiteTurn ? "Sort" : "Hvid";
                            String loser = Game.isWhiteTurn ? "Hvid" : "Sort";
                            JOptionPane.showMessageDialog(this, winner + " vinder – " + loser + " er mat!");
                        } else if (Game.isDrawByStalemate()) {
                            JOptionPane.showMessageDialog(this, "Patt! Uafgjort.");
                        }
                    }

                    // 👾 AI trækker når det er AI's tur
                    if ((Game.isWhiteTurn && !Game.playerIsWhite) || (!Game.isWhiteTurn && Game.playerIsWhite)) {
                        Move aiMove = AI.findBestMove(4); // dybde 5
                        if (aiMove != null) {
                            String aiColor = Game.isWhiteTurn ? "Hvid" : "Sort";
                            System.out.println("🤖 " + aiColor + " (AI) trækker: " + aiMove);
                            Game.makeMove(aiMove);

                            // Tjek om spilleren nu er mat eller i patt
                            List<Move> playerMoves = Game.generateLegalMoves();
                            if (playerMoves.isEmpty()) {
                                if (Game.isCurrentPlayerInCheck()) {
                                    String winner = Game.isWhiteTurn ? "Sort" : "Hvid";
                                    String loser = Game.isWhiteTurn ? "Hvid" : "Sort";
                                    JOptionPane.showMessageDialog(this, winner + " vinder – " + loser + " er mat!");
                                } else if (Game.isDrawByStalemate()) {
                                    JOptionPane.showMessageDialog(this, "Patt! Uafgjort.");
                                }
                            }
                        }
                    }

                    drawBoard(); // opdater GUI efter træk
                    break;
                }
            }

            selectedSquare = -1;
            drawBoard();
        }
    }






    void drawBoard() {
        boardPanel.removeAll();

        // Skakbræt-farver
        Color lightSquare = new Color(240, 217, 181);   // beige
        Color darkSquare = new Color(181, 136, 99);     // brun
        Color selectedColor = new Color(255, 255, 153); // gul markering

        Font chessFont = new Font("Ariel", Font.PLAIN, 32);

        // Find ud af hvilken retning vi skal tegne brættet
        boolean reverseBoard = !Game.playerIsWhite;

        for (int rankIndex = 0; rankIndex < 8; rankIndex++) {
            for (int fileIndex = 0; fileIndex < 8; fileIndex++) {
                // Find den rigtige række og linje afhængigt af spillerens perspektiv
                int rank = reverseBoard ? rankIndex : (7 - rankIndex);
                int file = reverseBoard ? (7 - fileIndex) : fileIndex;

                int square = rank * 16 + file;
                int piece = Game.board[square];

                JButton tile = new JButton(getPieceSymbol(piece));
                tile.setFont(chessFont);
                tile.setFocusPainted(false);
                tile.setBorderPainted(false);
                tile.setOpaque(true);
                tile.setMargin(new Insets(2, 2, 2, 2));
                tile.setContentAreaFilled(true);

                // 🎨 Sæt tekstfarve
                if (piece > 0) {
                    tile.setForeground(Color.WHITE);
                } else if (piece < 0) {
                    tile.setForeground(Color.BLACK);
                } else {
                    tile.setForeground(Color.DARK_GRAY);
                }

                // RETTELSE: Feltfarven skal beregnes fra den faktiske skakbrætsposition
                boolean isDark = (rank + file) % 2 == 0;
                Color baseColor = isDark ? darkSquare : lightSquare;
                tile.setBackground(baseColor);

                // ✨ Marker valgt felt
                if (square == selectedSquare) {
                    tile.setBackground(selectedColor);
                    tile.setBorder(new LineBorder(Color.YELLOW, 3));
                } else {
                    tile.setBorder(null);
                }

                final int clickedSquare = square;

                // 🖱️ Klik
                tile.addActionListener(e -> handleClick(clickedSquare));

                // 🌈 Hover-effekt
                tile.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent evt) {
                        tile.setBackground(tile.getBackground().brighter());
                    }

                    public void mouseExited(java.awt.event.MouseEvent evt) {
                        if (clickedSquare == selectedSquare) {
                            tile.setBackground(selectedColor);
                        } else {
                            tile.setBackground(baseColor);
                        }
                    }
                });

                boardPanel.add(tile);
            }
        }

        boardPanel.revalidate();
        boardPanel.repaint();
    }

    // Hjælpemetode til at oprette en brikkefelt
    private JButton createTile(int piece, int square, int rank, int file, Color selectedColor, Font chessFont, Color lightSquare, Color darkSquare) {
        JButton tile = new JButton(getPieceSymbol(piece));
        tile.setFont(chessFont);
        tile.setFocusPainted(false);
        tile.setBorderPainted(false);
        tile.setOpaque(true);
        tile.setMargin(new Insets(2, 2, 2, 2));
        tile.setContentAreaFilled(true);

        // 🎨 Sæt tekstfarve
        if (piece > 0) {
            tile.setForeground(Color.WHITE);
        } else if (piece < 0) {
            tile.setForeground(Color.BLACK);
        } else {
            tile.setForeground(Color.DARK_GRAY);
        }

        boolean isDark = (rank + file) % 2 == 0;
        Color baseColor = isDark ? darkSquare : lightSquare;
        tile.setBackground(baseColor);

        // ✨ Marker valgt felt
        if (square == selectedSquare) {
            tile.setBackground(selectedColor);
            tile.setBorder(new LineBorder(Color.YELLOW, 3));
        } else {
            tile.setBorder(null);
        }

        final int clickedSquare = square;

        // 🖱️ Klik
        tile.addActionListener(e -> handleClick(clickedSquare));

        // 🌈 Hover-effekt
        tile.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                tile.setBackground(tile.getBackground().brighter());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (clickedSquare == selectedSquare) {
                    tile.setBackground(selectedColor);
                } else {
                    tile.setBackground(baseColor);
                }
            }
        });

        return tile;
    }

    public static boolean showColorSelectionDialog() {
        String[] options = {"Spil som Hvid", "Spil som Sort"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "Vælg din farve",
                "Farvevalg",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        return choice == 0; // True hvis hvid vælges
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
