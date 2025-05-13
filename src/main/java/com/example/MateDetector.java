package com.example;

import java.util.List;

/**
 * MateDetector indeholder metoder til at opdage skakmat-trusler og -muligheder.
 * Dette er kritisk for en chess engine da mat har højeste prioritet.
 */
public class MateDetector {

    /**
     * Tjekker om den aktuelle spiller kan gives skakmat i næste træk.
     * Dette er den vigtigste sikkerhedsfunktion for AI'en.
     *
     * @return True hvis der er en mat-trussel (AI skal reagere øjeblikkeligt)
     */
    public static boolean isMateInOne() {
        boolean currentPlayerIsWhite = Game.isWhiteTurn;

        // Skift til modstanders perspektiv for at se om de kan give mat
        Game.changeTurn();

        List<Move> opponentMoves = Game.generateLegalMoves();
        boolean mateFound = false;

        for (Move move : opponentMoves) {
            int captured = Game.makeMove(move);

            // Tjek om dette træk resulterer i skakmat
            if (Game.isCheckmate()) {
                System.out.println("🚨🚨🚨 CRITICAL: Opponent has mate in 1 with " + move + "!");
                mateFound = true;
                Game.undoMove(move, captured);
                break;
            }

            Game.undoMove(move, captured);
        }

        // Skift tilbage til original spiller
        Game.changeTurn();

        return mateFound;
    }

    /**
     * Finder alle træk der giver skakmat i et træk.
     * Dette bruges til at prioritere mat-træk over alt andet.
     *
     * @return Liste af træk der giver øjeblikkelig skakmat
     */
    public static List<Move> findMateInOneMoves() {
        List<Move> mateMoves = new java.util.ArrayList<>();
        List<Move> legalMoves = Game.generateLegalMoves();

        for (Move move : legalMoves) {
            int captured = Game.makeMove(move);

            // Tjek om dette træk resulterer i skakmat for modstanderen
            if (Game.isCheckmate()) {
                System.out.println("✅ Found mate in 1: " + move);
                mateMoves.add(move);
            }

            Game.undoMove(move, captured);
        }

        return mateMoves;
    }

    /**
     * Tjekker om der er træk der forhindrer skakmat.
     * Bruges når AI'en er truet af mat for at finde redningen.
     *
     * @return Et træk der forhindrer skakmat, eller null hvis ingen findes
     */
    public static Move findMateDefense() {
        if (!isMateInOne()) {
            return null; // Ingen mat-trussel
        }

        List<Move> legalMoves = Game.generateLegalMoves();

        for (Move move : legalMoves) {
            int captured = Game.makeMove(move);

            // Tjek om dette træk forhindrer mat
            boolean stillMateAfterMove = isMateInOne();

            if (!stillMateAfterMove) {
                System.out.println("🛡️ Defense found: " + move + " prevents mate!");
                Game.undoMove(move, captured);
                return move;
            }

            Game.undoMove(move, captured);
        }

        System.out.println("💀 No defense against mate found!");
        return null;
    }

    /**
     * Tjekker for skakmat i 2 træk.
     * Mere avanceret, men vigtig for at undgå at gå ind i mat-fælder.
     *
     * @return True hvis modstanderen har mat i 2 træk
     */
    public static boolean isMateInTwo() {
        // Skift til modstanders perspektiv
        Game.changeTurn();

        List<Move> opponentFirstMoves = Game.generateLegalMoves();
        boolean mateInTwoFound = false;

        for (Move firstMove : opponentFirstMoves) {
            int captured1 = Game.makeMove(firstMove);

            // Efter modstanderens første træk, kan vi reagere?
            Game.changeTurn(); // Skift til vores tur
            List<Move> ourResponses = Game.generateLegalMoves();

            boolean allResponsesLeadToMate = true;

            for (Move ourResponse : ourResponses) {
                int captured2 = Game.makeMove(ourResponse);

                // Kan modstanderen så give mat?
                Game.changeTurn(); // Tilbage til modstander
                if (!isMateInOne()) {
                    allResponsesLeadToMate = false;
                    Game.changeTurn(); // Tilbage til os
                    Game.undoMove(ourResponse, captured2);
                    break;
                }
                Game.changeTurn(); // Tilbage til os
                Game.undoMove(ourResponse, captured2);
            }

            Game.changeTurn(); // Tilbage til modstander
            Game.undoMove(firstMove, captured1);

            if (allResponsesLeadToMate && ourResponses.size() > 0) {
                System.out.println("🚨 Warning: Opponent has mate in 2 starting with " + firstMove);
                mateInTwoFound = true;
                break;
            }
        }

        // Skift tilbage til original spiller
        Game.changeTurn();

        return mateInTwoFound;
    }

    /**
     * Evaluerer mat-trusler og sætter meget høj score på mat-situationer.
     * Denne funktion integreres i hovedevalueringen.
     *
     * @return Tillægscore baseret på mat-trusler (-/+ 1,000,000 for mat)
     */
    public static int evaluateMateThreats() {
        // Hvis vi kan give skakmat, er det den bedste score muligt
        List<Move> mateMoves = findMateInOneMoves();
        if (!mateMoves.isEmpty()) {
            System.out.println("🏆 We have mate in 1! Score: +1,000,000");
            return 1_000_000;
        }

        // Hvis vi er truet af skakmat, er det den værste score muligt
        if (isMateInOne()) {
            System.out.println("💀 We are threatened with mate in 1! Score: -1,000,000");
            return -1_000_000;
        }

        // Mindre alvorlige trusler får lavere straffe/bonusser
        if (isMateInTwo()) {
            System.out.println("⚠️ Mate in 2 threat detected. Score: -500,000");
            return -500_000;
        }

        return 0; // Ingen mat-trusler
    }
}