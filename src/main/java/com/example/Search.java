package com.example;

import java.util.List;

public class Search {
    /**
     * Sorterer træk efter deres forventede værdi.
     * Prioriterer slag med positiv materielforskel højest.
     * Dette forbedrer alpha-beta beskæring markant.
     *
     * @param moves Listen af træk der skal sorteres
     * @return Sorteret liste med "mest lovende" træk først
     */
    public static void sortMoves(List<Move> moves) {
        moves.sort((a, b) -> Integer.compare(moveScore(b), moveScore(a)));
    }

    /**
     * Tildeler en "vigtighedsscore" til et træk for move-ordering.
     * Slag af høj-værdi brikker med lav-værdi brikker prioriteres højest.
     *
     * @param move Trækket der skal evalueres
     * @return En numerisk score, hvor højere tal = bedre træk for move-ordering
     */
    private static int moveScore(Move move) {
        int fromPiece = Game.board[move.from];
        int toPiece = Game.board[move.to];

        // Hvis det er et capture
        if (toPiece != 0) {
            int captureGain = Math.abs(Evaluation.getPieceValue(toPiece)) - Math.abs(Evaluation.getPieceValue(fromPiece));
            // Giv en "basis" på 1000 for at prioritere captures,
            // plus en differensafhængig bonus.
            return 1000 + captureGain;
        }

        // Alm. ikke-capture
        return 0;
    }

    /**
     * Quiescence-søgning forhindrer horisonteffekten ved at fortsætte søgningen
     * ved ustabile positioner (især slag) indtil stillingen er "rolig".
     *
     * @param alpha Alpha-værdi for pruning
     * @param beta Beta-værdi for pruning
     * @param maximizingPlayer Om den aktuelle spiller maksimerer (hvid) eller minimerer (sort)
     * @return Evaluering af stillingen når den er "rolig"
     */
    public static int quiescence(int alpha, int beta, boolean maximizingPlayer, long startTime, long timeLimit) {
        // Tidstjek i quiescence
        if (System.currentTimeMillis() - startTime > timeLimit) {
            return Evaluation.evaluatePosition();
        }

        int standPat = Evaluation.evaluatePosition();

        if (maximizingPlayer) {
            if (standPat >= beta) return beta;
            if (alpha < standPat) alpha = standPat;
        } else {
            if (standPat <= alpha) return alpha;
            if (beta > standPat) beta = standPat;
        }

        List<Move> captures = Game.generateLegalMoves();

        // 🎯 Filtrér kun captures og sorter dem efter værdi (MVV-LVA)
        captures.removeIf(m -> Game.board[m.to] == 0);

        captures.sort((a, b) -> {
            int aValue = Math.abs(Evaluation.getPieceValue(Game.board[a.to])) - Math.abs(Evaluation.getPieceValue(Game.board[a.from]));
            int bValue = Math.abs(Evaluation.getPieceValue(Game.board[b.to])) - Math.abs(Evaluation.getPieceValue(Game.board[b.from]));
            return Integer.compare(bValue, aValue); // højeste forskel først
        });

        for (Move move : captures) {
            // Tidstjek mellem captures
            if (System.currentTimeMillis() - startTime > timeLimit) {
                break;
            }

            int captured = Game.makeMove(move);
            int score = quiescence(alpha, beta, !maximizingPlayer, startTime, timeLimit);
            Game.undoMove(move, captured);

            if (maximizingPlayer) {
                if (score > alpha) alpha = score;
                if (alpha >= beta) break;
            } else {
                if (score < beta) beta = score;
                if (beta <= alpha) break;
            }
        }

        return maximizingPlayer ? alpha : beta;
    }

    /**
     * Alpha-beta pruning algoritme for effektiv søgning.
     * Bruger move-ordering for at optimere beskæringer.
     *
     * @param depth Resterende søgedybde
     * @param alpha Alpha-værdi for pruning
     * @param beta Beta-værdi for pruning
     * @param maximizingPlayer Om den aktuelle spiller maksimerer (hvid) eller minimerer (sort)
     * @return Bedste score for den aktuelle spiller
     */
    public static int alphaBeta(int depth, int alpha, int beta, boolean maximizingPlayer, long startTime, long timeLimit) {
        // Tidstjek
        if (System.currentTimeMillis() - startTime > timeLimit) {
            return Evaluation.evaluatePosition();
        }

        // Basetilfælde
        if (depth == 0) {
            return quiescence(alpha, beta, maximizingPlayer, startTime, timeLimit);
        }

        // Generer og sortér træk (move ordering)
        List<Move> moves = Game.generateLegalMoves();
        sortMoves(moves);

        if (maximizingPlayer) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : moves) {
                // Tidstjek mellem træk
                if (System.currentTimeMillis() - startTime > timeLimit) {
                    break;
                }

                int captured = Game.makeMove(move);

                int eval = alphaBeta(depth - 1, alpha, beta, false, startTime, timeLimit);

                Game.undoMove(move, captured);

                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);

                // Pruning
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (Move move : moves) {
                // Tidstjek mellem træk
                if (System.currentTimeMillis() - startTime > timeLimit) {
                    break;
                }

                int captured = Game.makeMove(move);

                int eval = alphaBeta(depth - 1, alpha, beta, true, startTime, timeLimit);

                Game.undoMove(move, captured);

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);

                // Pruning
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    // Hjælpemetode til at forbedre move-ordering mellem iterationer
    static void reorderMovesBasedOnPreviousSearch(List<Move> moves, Move bestMove) {
        if (bestMove == null) return;

        // Flyt det bedste træk først i listen
        moves.remove(bestMove);
        moves.add(0, bestMove);
    }
}
