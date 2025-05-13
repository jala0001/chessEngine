package com.example;

import java.util.List;

public class AI {


    public static Move findBestMove(int maxDepth) {
        List<Move> legalMoves = Game.generateLegalMoves();

        // Sortér dem med MVV-LVA (Most Valuable Victim – Least Valuable Attacker)
        Search.sortMoves(legalMoves);

        Move bestMove = null;
        Move bestMoveAtDepth = null;
        int bestScoreAtDepth = 0;

        // Start tidsmåling (5 sekunder max)
        long startTime = System.currentTimeMillis();

        long timeLimit = 5000; // 5 sekunder i millisekunder

        // *** KRITISK: TJEK FOR SKAKMAT FØRST ***
        // Hvis vi kan give mat, gør det øjeblikkeligt
        List<Move> mateMoves = MateDetector.findMateInOneMoves();
        if (!mateMoves.isEmpty()) {
            System.out.println("🏆🏆🏆 MATE IN 1 FOUND! Playing " + mateMoves.get(0));
            return mateMoves.get(0);
        }

        // Hvis vi er truet af mat, find forsvar øjeblikkeligt
        if (MateDetector.isMateInOne()) {
            Move defense = MateDetector.findMateDefense();
            if (defense != null) {
                System.out.println("🛡️🛡️🛡️ DEFENDING AGAINST MATE with " + defense);
                return defense;
            } else {
                System.out.println("💀💀💀 NO DEFENSE AGAINST MATE - choosing random move");
                // Hvis ingen forsvar findes, spil det første lovlige træk
                return legalMoves.isEmpty() ? null : legalMoves.get(0);
            }
        }

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

        // *** EVALUER MAT-TRUSLER ***
        int mateScore = MateDetector.evaluateMateThreats();
        System.out.println("💀 Mate threat evaluation: " + mateScore);

        // *** FIND TRUEDE BRIKKER (FORBEDRET) ***
        // Brug den avancerede metode der tjekker om angribere kan slås
        int[][] threatenedPieces = ThreatDetector.findThreatenedPiecesAdvanced();
        if (threatenedPieces.length > 0) {
            System.out.println("\n⚠️⚠️⚠️ ALERT: " + threatenedPieces.length + " pieces are in REAL danger!");
            for (int[] piece : threatenedPieces) {
                int square = piece[0];
                int value = piece[1];
                System.out.println("  - " + Evaluation.getPieceName(Game.board[square]) +
                        " (value: " + value + ") at " +
                        MoveGenerator.squareToCoord(square));
            }
        }

        for (Move move : moves) {
            if (System.currentTimeMillis() - startTime > timeLimit) {
                break;
            }

            int movedPiece = Game.board[move.from];
            int targetPiece = Game.board[move.to];
            int rescueBonus = 0;
            int mateBonus = 0;

            // *** TJEK OM DETTE TRÆK GIVER SKAKMAT ***
            int captured = Game.makeMove(move);
            if (Game.isCheckmate()) {
                mateBonus = 1_000_000; // Maksimal bonus for mat
                System.out.println("🏆 Move " + move + " gives CHECKMATE! Bonus: +" + mateBonus);
            }
            Game.undoMove(move, captured);

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

            boolean nextIsMaximizing = !isMaximizingRoot;
            int score = Search.alphaBeta(depth - 1,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    nextIsMaximizing,
                    startTime,
                    timeLimit);
            Game.undoMove(move, captured);

            int side = (movedPiece > 0) ? +1 : -1;

            // **1) mate-bonus** (HØJESTE PRIORITET)
            score += side * mateBonus;

            // **2) rescue‐bonus**
            score += side * rescueBonus;
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
            System.out.printf("Move: %-8s Base score: %-6d", move, score - captureBonus - rescueBonus - mateBonus);
            if (mateBonus > 0) {
                System.out.printf(" CHECKMATE! (+%d)", mateBonus);
            }
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