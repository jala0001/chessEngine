package com.example;

public class Move {
    public int from, to;
    public boolean isEnPassant = false;
    public boolean isCastleKingside = false;
    public boolean isCastleQueenside = false;

    // Gem tidligere state så vi kan fortryde korrekt
    public boolean prevWhiteKingMoved;
    public boolean prevBlackKingMoved;
    public boolean prevWhiteKingsideRookMoved;
    public boolean prevWhiteQueensideRookMoved;
    public boolean prevBlackKingsideRookMoved;
    public boolean prevBlackQueensideRookMoved;
    public int prevEnPassantSquare;

    public Move(int from, int to) {
        this.from = from;
        this.to = to;
    }

    // Kopi-konstruktor – bruges i simulering
    public Move(Move other) {
        this.from = other.from;
        this.to = other.to;
        this.isEnPassant = other.isEnPassant;
        this.isCastleKingside = other.isCastleKingside;
        this.isCastleQueenside = other.isCastleQueenside;
    }

    @Override
    public String toString() {
        return Main.squareToCoord(from) + " -> " + Main.squareToCoord(to);
    }
}

