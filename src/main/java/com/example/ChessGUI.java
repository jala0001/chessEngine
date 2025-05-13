package com.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ChessGUI extends JFrame {
    static final int TILE_SIZE = 64;

    JPanel mainPanel = new JPanel(new BorderLayout());
    JPanel boardPanel = new JPanel(new GridLayout(8, 8));
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
    JLabel titleLabel = new JLabel(" SKAK // BOT", SwingConstants.CENTER);

    int selectedSquare = -1;
    int lastMoveTo = -1;
    boolean playerIsWhite = true;
    boolean flippedBoard = false;
    boolean darkTheme = false;

    List<Integer> threatenedSquares = new ArrayList<>();

    Color lightSquare, darkSquare, selectedColor, threatColor, bgColor, buttonColor, boardBackground;

    public ChessGUI() {
        setTitle("Skak – Niveau 2000");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(8 * TILE_SIZE + 40, 8 * TILE_SIZE + 180);
        setLocationRelativeTo(null);

        setupTheme();
        setupTitle();
        setupButtons();
        drawBoard();

        mainPanel.setBackground(bgColor);
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(boardPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel);

        setVisible(true);
    }

    void setupTheme() {
        if (darkTheme) {
            lightSquare = new Color(100, 100, 100);
            darkSquare = new Color(60, 60, 60);
            bgColor = new Color(35, 25, 30);
            buttonColor = new Color(90, 20, 30);
            boardBackground = new Color(45, 35, 45);
        } else {
            lightSquare = new Color(240, 217, 181);
            darkSquare = new Color(181, 136, 99);
            bgColor = new Color(255, 255, 255);
            buttonColor = new Color(180, 50, 60);
            boardBackground = new Color(230, 230, 230);
        }

        selectedColor = new Color(255, 255, 153);
        threatColor = new Color(255, 102, 102);

        boardPanel.setBackground(boardBackground);
        buttonPanel.setBackground(bgColor);
        getContentPane().setBackground(bgColor);
    }

    void setupTitle() {
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLabel.setForeground(darkTheme ? new Color(230, 230, 230) : new Color(40, 40, 40));
        titleLabel.setBorder(new EmptyBorder(20, 0, 10, 0));
    }

    void setupButtons() {
        buttonPanel.removeAll();
        Font buttonFont = new Font("SansSerif", Font.PLAIN, 14);

        buttonPanel.add(makeButton("Tema", () -> {
            darkTheme = !darkTheme;
            setupTheme();
            drawBoard();
            setupButtons();
        }, buttonFont));

        buttonPanel.add(makeButton("Start forfra", () -> {
            Game.reset();
            selectedSquare = -1;
            lastMoveTo = -1;
            threatenedSquares.clear();
            drawBoard();

            if (Game.isWhiteTurn != playerIsWhite) {
                Move aiMove = AI.findBestMove(6);
                if (aiMove != null) {
                    Game.makeMove(aiMove);
                    lastMoveTo = aiMove.to;
                    drawBoard();
                }
            }
        }, buttonFont));

        buttonPanel.add(makeButton("Skift farve", () -> {
            playerIsWhite = !playerIsWhite;
            flippedBoard = !flippedBoard;
            Game.reset();
            selectedSquare = -1;
            lastMoveTo = -1;
            threatenedSquares.clear();

            if (Game.isWhiteTurn != playerIsWhite) {
                Move aiMove = AI.findBestMove(6);
                if (aiMove != null) {
                    Game.makeMove(aiMove);
                    lastMoveTo = aiMove.to;
                }
            }
            drawBoard();
        }, buttonFont));

        buttonPanel.add(makeButton("Truede", () -> {
            threatenedSquares.clear();
            int[][] threats = ThreatDetector.findThreatenedPieces();
            for (int[] t : threats) threatenedSquares.add(t[0]);
            drawBoard();
        }, buttonFont));

        buttonPanel.add(makeButton("Evaluering", () -> {
            int score = Evaluation.evaluatePosition(true);
            JOptionPane.showMessageDialog(this, "Evaluering: " + score);
        }, buttonFont));
    }

    JButton makeButton(String text, Runnable action, Font font) {
        JButton btn = new JButton(text);
        btn.setFont(font);
        btn.setForeground(Color.WHITE);
        btn.setBackground(buttonColor);
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(btn.getBackground().brighter());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(buttonColor);
            }
        });

        btn.addActionListener(e -> action.run());
        return btn;
    }

    void drawBoard() {
        boardPanel.removeAll();
        Font font = new Font("SansSerif", Font.PLAIN, 32);

        for (int r = 0; r < 8; r++) {
            int rank = flippedBoard ? r : 7 - r;
            for (int f = 0; f < 8; f++) {
                int file = flippedBoard ? 7 - f : f;
                int square = rank * 16 + file;
                int piece = Game.board[square];

                JButton tile = new JButton(getPieceSymbol(piece));
                tile.setFont(font);
                tile.setFocusPainted(false);
                tile.setBorderPainted(false);
                tile.setOpaque(true);
                tile.setMargin(new Insets(2, 2, 2, 2));
                tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                tile.setForeground(piece > 0 ? Color.WHITE : (piece < 0 ? Color.BLACK : Color.GRAY));

                Color baseColor = ((rank + file) % 2 == 0) ? darkSquare : lightSquare;

                if (square == selectedSquare) {
                    tile.setBackground(selectedColor);
                    tile.setBorder(new LineBorder(Color.YELLOW, 3));
                } else if (square == lastMoveTo) {
                    tile.setBackground(new Color(200, 200, 200, 150));
                } else if (threatenedSquares.contains(square)) {
                    tile.setBackground(threatColor);
                } else {
                    tile.setBackground(baseColor);
                }

                final int clicked = square;
                tile.addActionListener(e -> handleClick(clicked));

                tile.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent evt) {
                        tile.setBackground(tile.getBackground().brighter());
                    }

                    public void mouseExited(java.awt.event.MouseEvent evt) {
                        if (clicked == selectedSquare) {
                            tile.setBackground(selectedColor);
                        } else if (clicked == lastMoveTo) {
                            tile.setBackground(new Color(200, 200, 200, 150));
                        } else if (threatenedSquares.contains(clicked)) {
                            tile.setBackground(threatColor);
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

    void handleClick(int square) {
        int piece = Game.board[square];

        if (selectedSquare == -1) {
            if ((Game.isWhiteTurn && playerIsWhite && piece > 0) ||
                    (!Game.isWhiteTurn && !playerIsWhite && piece < 0)) {
                selectedSquare = square;
            }
        } else {
            List<Move> legalMoves = Game.generateLegalMoves();
            for (Move move : legalMoves) {
                if (move.from == selectedSquare && move.to == square) {
                    Game.makeMove(move);
                    handlePromotionIfNeeded(move);
                    lastMoveTo = move.to;
                    selectedSquare = -1;
                    threatenedSquares.clear();
                    drawBoard();
                    checkEnd();

                    if (Game.isWhiteTurn != playerIsWhite) {
                        new Thread(() -> {
                            try {
                                Thread.sleep(100); // kort forsinkelse for visning
                            } catch (InterruptedException ignored) {
                            }

                            Move aiMove = AI.findBestMove(6);
                            if (aiMove != null) {
                                Game.makeMove(aiMove);
                                handlePromotionIfNeeded(aiMove);
                                lastMoveTo = aiMove.to;

                                SwingUtilities.invokeLater(() -> {
                                    drawBoard();
                                    checkEnd();
                                });
                            }
                        }).start();
                    }
                    return;
                }
            }
            selectedSquare = -1;
            threatenedSquares.clear();
        }

        drawBoard();
    }

    void handlePromotionIfNeeded(Move move) {
        if (move.promotionPiece != 0) return;

        int piece = Game.board[move.to];
        boolean isPawn = Math.abs(piece) == MoveGenerator.PAWN;
        int rank = move.to / 16;

        if (isPawn && (rank == 0 || rank == 7)) {
            String[] options = {"Dronning", "Tårn", "Løber", "Springer"};
            int choice = JOptionPane.showOptionDialog(this, "Vælg forfremmelse:", "Bondeforfremmelse",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

            int newPiece = switch (choice) {
                case 1 -> MoveGenerator.ROOK;
                case 2 -> MoveGenerator.BISHOP;
                case 3 -> MoveGenerator.KNIGHT;
                default -> MoveGenerator.QUEEN;
            };

            Game.board[move.to] = (piece > 0 ? newPiece : -newPiece);
        }
    }

    void checkEnd() {
        List<Move> moves = Game.generateLegalMoves();
        if (moves.isEmpty()) {
            String winner = Game.isWhiteTurn ? "Sort" : "Hvid";
            if (Game.isCurrentPlayerInCheck()) {
                JOptionPane.showMessageDialog(this, winner + " vinder! Mat.");
            } else if (Game.isDrawByStalemate()) {
                JOptionPane.showMessageDialog(this, "Patt! Uafgjort.");
            }
        }
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
