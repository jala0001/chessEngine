package com.example;

import java.util.List;

/**
 * AI-klassen implementerer skak-motoren, som beregner og vælger træk.
 * Den indeholder evalueringsfunktioner, søgealgoritmer og sikkerhedskontroller.
 */

public class AI {

    // ===== PIECE-SQUARE TABLES =====
    // Disse tabeller giver positionelle bonusser baseret på brikkernes placering på brættet

    // ===== EVALUERINGS-METODER =====

    // ===== TRUSSEL-DETEKTIONS METODER =====

    // ===== SØGEALGORITMER =====


    public static Move findBestMove(int maxDepth) {
        List<Move> legalMoves = Game.generateLegalMoves();

        // Sortér dem med MVV-LVA (Most Valuable Victim – Least Valuable Attacker)
        Search.sortMoves(legalMoves);

        Move bestMove = null;
        Move bestMoveAtDepth = null;
        int bestScoreAtDepth = 0;

        // Start tidsmåling (15 sekunder max)
        long startTime = System.currentTimeMillis();
        long timeLimit = 15000; // 15 sekunder i millisekunder

        // Iterativ deepening med tidsbegrænsning
        for (int depth = 1; depth <= maxDepth; depth++) {
            // Tjek om tid er løbet ud
            if (System.currentTimeMillis() - startTime > timeLimit) {
                System.out.println("\n⏰ Time limit reached! Stopping search at depth " + (depth - 1));
                break;
            }

            System.out.println("\n===== ITERATIVE DEEPENING - DEPTH " + depth + " =====");

            bestMoveAtDepth = findBestMoveAtDepth(depth, legalMoves, startTime, timeLimit);

            if (bestMoveAtDepth != null) {
                bestMove = bestMoveAtDepth;

                // Evaluer det valgte træk
                int captured = Game.makeMove(bestMove);
                boolean isMaximizingRoot = Game.isWhiteTurn;
                bestScoreAtDepth = Evaluation.evaluatePosition();
                Game.undoMove(bestMove, captured);

                System.out.println("✓ BEST MOVE AT DEPTH " + depth + ": " + bestMove +
                        " with score: " + bestScoreAtDepth);
            }

            // Opdater move-rækkefølgen baseret på det bedste træk
            // Dette forbedrer alpha-beta cutoffs i næste iteration
            if (bestMove != null) {
                Search.reorderMovesBasedOnPreviousSearch(legalMoves, bestMove);
            }

            // Ekstra tidstjek før næste iteration starter
            if (System.currentTimeMillis() - startTime > timeLimit * 0.8) {
                System.out.println("\n⏰ Approaching time limit, wrapping up search...");
                break;
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("\n⏱️ Search completed in " + elapsedTime + "ms");

        return bestMove;
    }

    private static Move findBestMoveAtDepth(int depth, List<Move> moves, long startTime, long timeLimit) {
        boolean isMaximizingRoot = Game.isWhiteTurn;
        Move bestMove = null;
        int bestScore = isMaximizingRoot ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        System.out.println("Current position evaluation before AI's move:");
        Evaluation.evaluatePosition(true);

        // *** FIND TRUEDE BRIKKER ***
        int[][] threatenedPieces = ThreatDetector.findThreatenedPieces();
        if (threatenedPieces.length > 0) {
            System.out.println("\n⚠️⚠️⚠️ ALERT: " + threatenedPieces.length + " pieces are under attack!");
            for (int[] piece : threatenedPieces) {
                int square = piece[0];
                int value = piece[1];
                System.out.println("  - " + Evaluation.getPieceName(Game.board[square]) +
                        " (value: " + value + ") at " +
                        MoveGenerator.squareToCoord(square));
            }
        }

        for (Move move : moves) {
            // Tidstjek mellem træk
            if (System.currentTimeMillis() - startTime > timeLimit) {
                System.out.println("\n⏰ Time limit reached during move evaluation!");
                break;
            }

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
                    boolean destSafe = !ThreatDetector.isDestinationAttackedAfterMove(move);
                    if (destSafe) {
                        rescueBonus = (int)(threatened[1] * 1.5);
                        System.out.println("💡 Move " + move + " SAVES threatened " +
                                Evaluation.getPieceName(movedPiece) + " (bonus: +" + rescueBonus + ")");
                    } else {
                        System.out.println("❌ Move " + move + " tries to save " +
                                Evaluation.getPieceName(movedPiece) + " but destination is not safe");
                    }
                    break;
                }
            }

            // *** TJEK OM DEN SLÅEDE BRIK ER FORSVARET ***
            boolean capturedPieceDefended = false;
            if (targetPiece != 0) {
                capturedPieceDefended = ThreatDetector.isCapturedPieceDefended(move.to, movedPiece > 0);
            }

            // *** VIGTIGT: TJEK OM DESTINATIONEN VIL VÆRE UNDER ANGREB EFTER TRÆKKET ***
            boolean destUnderAttack = ThreatDetector.isDestinationAttackedAfterMove(move);
            if (destUnderAttack) {
                safetyPenalty = Math.abs(Evaluation.getPieceValue(movedPiece));
                System.out.println("⚠️ Move " + move + " puts " +
                        Evaluation.getPieceName(movedPiece) + " in danger at " +
                        MoveGenerator.squareToCoord(move.to));
            }

            // Evaluer position via alphaBeta med tidsbegrænsning
            int captured = Game.makeMove(move);
            boolean nextIsMaximizing = !isMaximizingRoot;
            int score = Search.alphaBeta(depth - 1,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    nextIsMaximizing,
                    startTime,
                    timeLimit);
            Game.undoMove(move, captured);

            // **2) side** (+1=hvid, −1=sort)
            int side = (movedPiece > 0) ? +1 : -1;

            // **1) rescue‐bonus**
            score += side * rescueBonus;

            // **3) safety‐penalty** (trækker for hvid, tilføjer for sort)
            score -= side * safetyPenalty;

            // **4) capture‐bonus** (tilføj/træk med korrekt fortegn)
            if (targetPiece != 0) {
                captureBonus = Math.abs(Evaluation.getPieceValue(targetPiece));

                if (!destUnderAttack) {
                    if (capturedPieceDefended) {
                        int exchangeValue = captureBonus - Math.abs(Evaluation.getPieceValue(movedPiece));
                        System.out.println("   ⚠️ Exchange evaluation: " + exchangeValue +
                                " (captures " + Evaluation.getPieceName(targetPiece) + " worth " + captureBonus +
                                " but risks " + Evaluation.getPieceName(movedPiece) + " worth " +
                                Math.abs(Evaluation.getPieceValue(movedPiece)) + ")");
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
                            " (captures " + Evaluation.getPieceName(targetPiece) + " worth " + captureBonus +
                            " but loses " + Evaluation.getPieceName(movedPiece) + " worth " + safetyPenalty + ")");
                    if (exchangeValue > 0) {
                        score += side * exchangeValue;
                    }
                }
            }

            // Formater og udskriv trækinformation
            System.out.printf("Move: %-8s Base score: %-6d", move, score - captureBonus - rescueBonus);
            if (targetPiece != 0) {
                if (capturedPieceDefended) {
                    System.out.printf(" DEFENDED Capture: %-6s", Evaluation.getPieceName(targetPiece));
                } else if (!destUnderAttack) {
                    System.out.printf(" Captures: %-6s (+%d)",
                            Evaluation.getPieceName(targetPiece), captureBonus);
                } else {
                    System.out.printf(" UNSAFE Capture: %-6s", Evaluation.getPieceName(targetPiece));
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
            Evaluation.evaluatePosition(true);
            Game.undoMove(bestMove, captured);

            if (ThreatDetector.isDestinationAttackedAfterMove(bestMove)) {
                System.out.println("\n⚠️ WARNING: The selected move puts a piece in immediate danger!");
            }
        }

        return bestMove;
    }

}