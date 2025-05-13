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
        long timeLimit = 5000; // 15 sekunder i millisekunder

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

        int[][] threatenedPieces = ThreatDetector.findThreatenedPieces();

        for (Move move : moves) {
            if (System.currentTimeMillis() - startTime > timeLimit) {
                break;
            }

            int movedPiece = Game.board[move.from];
            int targetPiece = Game.board[move.to];
            int rescueBonus = 0;
            int safetyPenalty = 0;

            for (int[] threatened : threatenedPieces) {
                if (move.from == threatened[0]) {
                    boolean destSafe = !ThreatDetector.isDestinationAttackedAfterMove(move);
                    if (destSafe) {
                        rescueBonus = (int)(threatened[1] * 0.15);
                    }
                    break;
                }
                if (move.to == threatened[0] && move.from != threatened[0]) {
                    int defendBonus = (int)(threatened[1] * 0.1);
                    rescueBonus += defendBonus;
                }
            }

            boolean destUnderAttack = ThreatDetector.isDestinationAttackedAfterMove(move);
            if (destUnderAttack) {
                safetyPenalty = Math.abs(Evaluation.getPieceValue(movedPiece));
            }

            int captured = Game.makeMove(move);
            boolean nextIsMaximizing = !isMaximizingRoot;
            int score = Search.alphaBeta(depth - 1,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    nextIsMaximizing,
                    startTime,
                    timeLimit);
            Game.undoMove(move, captured);

            int side = (movedPiece > 0) ? +1 : -1;
            score += side * rescueBonus;
            score -= side * safetyPenalty;

            if (targetPiece != 0) {
                int seeScore = Evaluation.staticExchangeEval(move.to, move.from);
                score += side * seeScore;
            }

            if (bestMove == null ||
                    (isMaximizingRoot && score > bestScore) ||
                    (!isMaximizingRoot && score < bestScore)) {
                bestScore = score;
                bestMove = move;
            }
        }

        return bestMove;
    }


}