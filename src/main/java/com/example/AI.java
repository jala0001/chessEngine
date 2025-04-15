package com.example;

import java.util.List;

public class AI {

    // Piece-square table for hvids bønder
    static final int[] pawnPST = {
            0,  5,  5, -10, -10,  5,  5,  0,
            0, 10, 10,   0,   0, 10, 10,  0,
            0, 10, 20,  20,  20, 20, 10,  0,
            5, 15, 15,  25,  25, 15, 15,  5,
            10, 20, 20,  30,  30, 20, 20, 10,
            5, 10, 10,  20,  20, 10, 10,  5,
            0,  0,  0,   0,   0,  0,  0,  0,
            0,  0,  0,   0,   0,  0,  0,  0
    };

    // Piece-square table for hvid springer
    static final int[] knightPST = {
            -50, -40, -30, -30, -30, -30, -40, -50,
            -40, -20,   0,   0,   0,   0, -20, -40,
            -30,   0,  10,  15,  15,  10,   0, -30,
            -30,   5,  15,  20,  20,  15,   5, -30,
            -30,   0,  15,  20,  20,  15,   0, -30,
            -30,   5,  10,  15,  15,  10,   5, -30,
            -40, -20,   0,   5,   5,   0, -20, -40,
            -50, -40, -30, -30, -30, -30, -40, -50
    };

    // Piece-square table for hvid løber
    static final int[] bishopPST = {
            -20, -10, -10, -10, -10, -10, -10, -20,
            -10,   5,   0,   0,   0,   0,   5, -10,
            -10,  10,  10,  10,  10,  10,  10, -10,
            -10,   0,  10,  10,  10,  10,   0, -10,
            -10,   5,   5,  10,  10,   5,   5, -10,
            -10,   0,   5,  10,  10,   5,   0, -10,
            -10,   0,   0,   0,   0,   0,   0, -10,
            -20, -10, -10, -10, -10, -10, -10, -20
    };

    // Tårn – bedst på åbne linjer og 7. række
    static final int[] rookPST = {
            0,   0,   0,   5,   5,   0,   0,   0,
            -5,   0,   0,   0,   0,   0,   0,  -5,
            -5,   0,   0,   0,   0,   0,   0,  -5,
            -5,   0,   0,   0,   0,   0,   0,  -5,
            -5,   0,   0,   0,   0,   0,   0,  -5,
            -5,   0,   0,   0,   0,   0,   0,  -5,
            5,  10,  10,  10,  10,  10,  10,   5,
            0,   0,   0,   0,   0,   0,   0,   0
    };

    // Dronning – fleksibel og stærk i centrum
    static final int[] queenPST = {
            -20, -10, -10,  -5,  -5, -10, -10, -20,
            -10,   0,   0,   0,   0,   0,   0, -10,
            -10,   0,   5,   5,   5,   5,   0, -10,
            -5,   0,   5,   5,   5,   5,   0,  -5,
            0,   0,   5,   5,   5,   5,   0,  -5,
            -10,   5,   5,   5,   5,   5,   0, -10,
            -10,   0,   5,   0,   0,   0,   0, -10,
            -20, -10, -10,  -5,  -5, -10, -10, -20
    };

    // Konge – åbningsposition: sikkerhed bag bønder
    static final int[] kingPST = {
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -20, -30, -30, -40, -40, -30, -30, -20,
            -10, -20, -20, -20, -20, -20, -20, -10,
            20,  20,   0,   0,   0,   0,  20,  20,
            20,  30,  10,   0,   0,  10,  30,  20
    };


    /**
     * Grundlæggende evaluering af stillingen:
     *  - Materialeværdi
     *  - PST (positionel bonus)
     *  - Straf for brikker i slag uden dækning
     */
    public static int evaluatePosition() {
        int score = 0;

        for (int i = 0; i < 128; i++) {
            if ((i & 0x88) != 0) continue;

            int piece = Game.board[i];
            if (piece == 0) continue;

            // Grundværdi
            score += getPieceValue(piece);

            // Positionel bonus via piece-square tables
            int rank = i >> 4;
            int file = i & 7;
            int index = (piece > 0) ? (rank * 8 + file) : ((7 - rank) * 8 + file); // flip for sort

            switch (Math.abs(piece)) {
                case MoveGenerator.PAWN -> score += (piece > 0 ? pawnPST[index] : -pawnPST[index]);
                case MoveGenerator.KNIGHT -> score += (piece > 0 ? knightPST[index] : -knightPST[index]);
                case MoveGenerator.BISHOP -> score += (piece > 0 ? bishopPST[index] : -bishopPST[index]);
                case MoveGenerator.ROOK -> score += (piece > 0 ? rookPST[index] : -rookPST[index]);
                case MoveGenerator.QUEEN -> score += (piece > 0 ? queenPST[index] : -queenPST[index]);
                case MoveGenerator.KING -> score += (piece > 0 ? kingPST[index] : -kingPST[index]);
            }

            // Straf hvis brikken er under trussel og ikke dækket
            boolean isAttacked = Game.isSquareAttacked(i, piece < 0);
            boolean isDefended = Game.isSquareAttacked(i, piece > 0);

            if (isAttacked && !isDefended) {
                int penalty = Math.abs(getPieceValue(piece)) / 2;
                score += (piece > 0 ? -penalty : penalty);
            }
        }

        return score;
    }

    /**
     * Tildeler en "grundværdi" til hver brik.
     * Positive værdier = hvid, negative = sort.
     */
    private static int getPieceValue(int piece) {
        return switch (piece) {
            case MoveGenerator.PAWN -> 100;
            case MoveGenerator.KNIGHT -> 320;
            case MoveGenerator.BISHOP -> 330;
            case MoveGenerator.ROOK -> 500;
            case MoveGenerator.QUEEN -> 900;
            case MoveGenerator.KING -> 20000;

            case -MoveGenerator.PAWN -> -100;
            case -MoveGenerator.KNIGHT -> -320;
            case -MoveGenerator.BISHOP -> -330;
            case -MoveGenerator.ROOK -> -500;
            case -MoveGenerator.QUEEN -> -900;
            case -MoveGenerator.KING -> -20000;
            default -> 0;
        };
    }

    /**
     * Move ordering - vi sorterer træk efter "mest lovende" først.
     *  1) Captures med positiv materielforskel
     *  2) Captures generelt
     *  3) Andre træk
     */
    private static List<Move> sortMoves(List<Move> moves) {
        moves.sort((a, b) -> Integer.compare(moveScore(b), moveScore(a)));
        return moves;
    }

    /**
     * Tildeler en "vigtighedsscore" til et træk.
     *  - Jo større stykke man slår, jo større score
     *  - Hvis man slår med en lille brik, er det ekstra godt
     *  - Non-captures får score 0
     */
    private static int moveScore(Move move) {
        int fromPiece = Game.board[move.from];
        int toPiece = Game.board[move.to];

        // Hvis det er et capture
        if (toPiece != 0) {
            int captureGain = Math.abs(getPieceValue(toPiece)) - Math.abs(getPieceValue(fromPiece));
            // Giv en "basis" på 1000 for at prioritere captures,
            // plus en differensafhængig bonus.
            return 1000 + captureGain;
        }

        // Alm. ikke-capture
        return 0;
    }

    private static int quiescence(int alpha, int beta, boolean maximizingPlayer) {
        int standPat = evaluatePosition();

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
            int aValue = Math.abs(getPieceValue(Game.board[a.to])) - Math.abs(getPieceValue(Game.board[a.from]));
            int bValue = Math.abs(getPieceValue(Game.board[b.to])) - Math.abs(getPieceValue(Game.board[b.from]));
            return Integer.compare(bValue, aValue); // højeste forskel først
        });

        for (Move move : captures) {
            int captured = Game.makeMove(move);
            int score = quiescence(alpha, beta, !maximizingPlayer);
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
     * Alpha-beta med move ordering for mere effektiv pruning
     */
    public static int alphaBeta(int depth, int alpha, int beta, boolean maximizingPlayer) {
        // Basetilfælde
        if (depth == 0) {
            return quiescence(alpha, beta, maximizingPlayer);
        }

        // Generer og sortér træk (move ordering)
        List<Move> moves = Game.generateLegalMoves();
        moves = sortMoves(moves);

        if (maximizingPlayer) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : moves) {
                int captured = Game.makeMove(move);

                int eval = alphaBeta(depth - 1, alpha, beta, false);

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
                int captured = Game.makeMove(move);

                int eval = alphaBeta(depth - 1, alpha, beta, true);

                Game.undoMove(move, captured);

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);

                // Pruning
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    /**
     * Find det bedste træk for den side, der har tur.
     * Inkl. captures-bonus og "unsafe move"-straf.
     */
    public static Move findBestMove(int depth) {
        List<Move> legalMoves = Game.generateLegalMoves();
        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (Move move : legalMoves) {
            int movedPiece = Game.board[move.from];
            int targetPiece = Game.board[move.to];

            int captured = Game.makeMove(move);
            int score = alphaBeta(depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);

            // 🎯 Bonus: slå en dyrere brik
            if (targetPiece != 0) {
                int gain = Math.abs(getPieceValue(targetPiece)) - Math.abs(getPieceValue(movedPiece));
                if (gain > 0) score += gain;  // ikke bare halv gevinst
            }

            // ⚔️ Straf hvis vi straks kan blive slået uden beskyttelse
            boolean underThreat = Game.isSquareAttacked(move.to, movedPiece < 0);
            boolean isDefended = Game.isSquareAttacked(move.to, movedPiece > 0);
            if (underThreat && !isDefended) {
                int penalty = Math.abs(getPieceValue(movedPiece)) / 2;
                score -= penalty;
            }

            Game.undoMove(move, captured);

            if (score > bestScore || bestMove == null) {
                bestScore = score;
                bestMove = move;
            }
        }

        System.out.println("AI vælger: " + bestMove + " med score: " + bestScore);
        return bestMove;
    }


}
