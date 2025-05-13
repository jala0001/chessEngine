package com.example;

@FunctionalInterface
public interface SlidingHandler {
    /**
     * @param from          Startfelt
     * @param to            Feltet der besøges
     * @param targetPiece   Brikken på feltet (0 hvis tomt)
     * @return true for at fortsætte sliding, false for at stoppe
     */
    boolean handle(int from, int to, int targetPiece);
}
