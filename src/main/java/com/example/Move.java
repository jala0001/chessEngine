package com.example;

/**
 * Represents a chess move with optional metadata like castling or en passant.
 * Also stores previous state to support undo functionality.
 */
public class Move {
    public final int from;
    public final int to;

    public boolean isEnPassant = false;
    public boolean isCastleKingside = false;
    public boolean isCastleQueenside = false;

    // Previous state for undoing moves
    public boolean prevWhiteKingMoved;
    public boolean prevBlackKingMoved;
    public boolean prevWhiteKingsideRookMoved;
    public boolean prevWhiteQueensideRookMoved;
    public boolean prevBlackKingsideRookMoved;
    public boolean prevBlackQueensideRookMoved;
    public int prevEnPassantSquare;

    public int promotionPiece = 0;


    /**
     * Constructs a move from one square to another.
     */
    public Move(int from, int to, int promotionPiece) {
        this.from = from;
        this.to = to;
        this.promotionPiece = promotionPiece;
    }

    public Move(int from, int to) {
        this.from = from;
        this.to = to;
    }

    /**
     * Copy constructor - used for move simulation (e.g., legal move filtering).
     */
    public Move(Move other) {
        this.from = other.from;
        this.to = other.to;
        this.isEnPassant = other.isEnPassant;
        this.isCastleKingside = other.isCastleKingside;
        this.isCastleQueenside = other.isCastleQueenside;

        this.prevWhiteKingMoved = other.prevWhiteKingMoved;
        this.prevBlackKingMoved = other.prevBlackKingMoved;
        this.prevWhiteKingsideRookMoved = other.prevWhiteKingsideRookMoved;
        this.prevWhiteQueensideRookMoved = other.prevWhiteQueensideRookMoved;
        this.prevBlackKingsideRookMoved = other.prevBlackKingsideRookMoved;
        this.prevBlackQueensideRookMoved = other.prevBlackQueensideRookMoved;
        this.prevEnPassantSquare = other.prevEnPassantSquare;
    }

    @Override
    public String toString() {
        return MoveGenerator.squareToCoord(from) + " -> " + MoveGenerator.squareToCoord(to);
    }
}


