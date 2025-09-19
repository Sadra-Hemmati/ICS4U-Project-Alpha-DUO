package Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Messages.DuoCard;
import Util.Log;

public class DuoMatch {
    private final List<DuoCard> deck = new ArrayList<>();
    private final List<DuoCard> discard = new ArrayList<>();
    private final List<List<DuoCard>> hands = new ArrayList<>();
    private boolean[] hasWonRace;
    private int raceStarterPid = -1;
    private int currentPlayer = 0;
    private int direction = 1; // 1 = clockwise, -1 = counter-clockwise

    public DuoMatch(int players) {
        for (int i=0;i<players;i++) hands.add(new ArrayList<>());

        hasWonRace = new boolean[players];
        for (int i=0;i<players;i++) hasWonRace[i] = false;

        buildDeck();
        shuffle();
        dealInitial(7);
        // flip top
        DuoCard top = draw();
        // if top is action card, try to place a non-action initial top
        while (top != null && (top.value.equals("WILD4") || top.value.equals("WILD") ||
                top.value.equals("SKIP") || top.value.equals("REVERSE") || top.value.equals("DRAW2"))) {
            // put wilds back into deck and reshuffle
            deck.add(top);
            shuffle();
            top = draw();
        }
        if (top != null) discard.add(top);
    }

    private void buildDeck() {
        String[] colors = {"RED","GREEN","BLUE","YELLOW"};
        for (String c: colors) {
            // one 0
            deck.add(new DuoCard(c, "0"));
            for (int i=1;i<=9;i++) {
                deck.add(new DuoCard(c, String.valueOf(i)));
                deck.add(new DuoCard(c, String.valueOf(i)));
            }
            // skips/reverse/draw2 x2
            deck.add(new DuoCard(c, "SKIP"));
            deck.add(new DuoCard(c, "SKIP"));
            deck.add(new DuoCard(c, "REVERSE"));
            deck.add(new DuoCard(c, "REVERSE"));
            deck.add(new DuoCard(c, "DRAW2"));
            deck.add(new DuoCard(c, "DRAW2"));
        }
        // wilds
        for (int i=0;i<4;i++) deck.add(new DuoCard("WILD","WILD"));
        for (int i=0;i<4;i++) deck.add(new DuoCard("WILD","WILD4"));
    }

    private void shuffle() { Collections.shuffle(deck); }

    private void dealInitial(int count) {
        for (int p=0;p<hands.size();p++) {
            for (int i=0;i<count;i++) hands.get(p).add(draw());
        }
    }

    public DuoCard draw() {
        if (deck.isEmpty()) {
            // reshuffle discard (leave top)
            if (discard.size() <= 1) return null;
            DuoCard top = discard.remove(discard.size()-1);
            deck.addAll(discard);
            discard.clear();
            discard.add(top);
            shuffle();
        }
        return deck.remove(deck.size()-1);
    }

    public DuoCard drawAndAdvance() {
        currentPlayer = advance(currentPlayer, 1);
        return draw();
    }

    public List<DuoCard> getPlayerHand(int playerId) { return hands.get(playerId); }

    public DuoCard getTopCard() { return discard.isEmpty() ? null : discard.get(discard.size()-1); }

    /**
     * Play a card. If the card is a wild (WILD or WILD4) and declaredColor is non-null,
     * that color will become the active color. Returns true if play was legal.
     */
    public boolean playCard(int playerId, DuoCard card) {
        DuoCard top = getTopCard();
        if (top == null) {
            Log.d("Duo Match", "Player " + playerId + " attempted to play " + card.toString() + " on null top card");
            return false;
        }
        List<DuoCard> hand = hands.get(playerId);
        if (!hand.remove(DuoCard.clearWildColor(card))){
            Log.d("Duo Match", "Player " + playerId + " attempted to play a card not in their hand: " + card.toString());
            return false;
        }

        String color = top.color.equals("WILD")? top.wildColor.toUpperCase() : top.color.toUpperCase();
        // check legality
        boolean legal = false;
        if (card.color.equals("WILD")) {
            legal = true;
        } else if (card.color.equals(color) || card.value.equals(top.value)) {
            legal = true;
        }

        if (!legal) {
            // illegal play, return card
            hand.add(card);
            Log.d("Duo Match", "Player " + playerId + " attempted illegal play of " + card.toString() + " on top " + top.toString());
            return false;
        }

        // perform card effects
        discard.add(card);
        Log.d("Duo Match", "Player " + playerId + " played " + card.toString() + " on top " + top.toString());

        // resolve special values
        switch (card.value) {
            case "SKIP":
                // advance by 2 positions (skip next player)
                currentPlayer = advance(currentPlayer, 2);
                break;
            case "REVERSE":
                if (hands.size() == 2) {
                    // reverse acts like skip in 2-player game
                    currentPlayer = advance(currentPlayer, 2);
                } else {
                    direction *= -1;
                    currentPlayer = advance(currentPlayer, 1);
                }
                break;
            case "DRAW2":
                // next player draws 2
                int next = advanceIndex(currentPlayer, 1);
                for (int i=0;i<2;i++) {
                    DuoCard c = draw(); if (c != null) hands.get(next).add(c);
                }
                currentPlayer = advance(currentPlayer, 1);
                break;
            case "WILD4":
                // // next player draws 4; declaredColor must be provided
                // if (declaredColor != null) {
                //     // set a virtual color by placing a marker card
                //     discard.add(new DuoCard(declaredColor, "WILD_COLOR"));
                // }
                int next4 = advanceIndex(currentPlayer, 1);
                for (int i=0;i<4;i++) { DuoCard c = draw(); if (c != null) hands.get(next4).add(c); }
                currentPlayer = advance(currentPlayer, 1);
                break;
            default:
                // number card or other
                currentPlayer = advance(currentPlayer, 1);
                break;
        }

        return true;
    }

    /**
     * Advance the current player index by 'steps' players considering direction.
     */
    private int advance(int from, int steps) {
        int idx = from;
        for (int i=0;i<steps;i++) idx = advanceIndex(idx, 1);
        return idx;
    }

    private int advanceIndex(int from, int delta) {
        int n = hands.size();
        int step = (delta * direction) % n;
        int r = (from + step) % n;
        if (r < 0) r += n;
        return r;
    }

    public void advanceTurn() {
        currentPlayer = advance(currentPlayer, 1);
    }

    public int getCurrentPlayer() { return currentPlayer; }

    public List<Integer> getHandSizes() {
        List<Integer> ret = new ArrayList<>();
        for (List<DuoCard> h: hands) ret.add(h.size());
        return ret;
    }

    public List<List<DuoCard>> getHands() {
        return hands;
    }

    public boolean isDuoRace() {
        for (int pid = 0; pid < hands.size(); pid++) {
            if (hasWonRace[pid] && hands.get(pid).size() > 2) hasWonRace[pid] = false;

            if (hasWonRace[pid]) continue;

            List<DuoCard> h = hands.get(pid);
            if (h.size() == 2){
                raceStarterPid = pid;
                return true;
            }
        }
        return false;
    }

    public void winDuoRace(int playerId) {
        if (raceStarterPid == playerId) {
            hasWonRace[playerId] = true;
        }
        else {
            Log.d("Duo Match", "Player " + raceStarterPid + " lost the Duo Race");
            for (int i = 0; i < 2; i++) {
                hands.get(raceStarterPid).add(draw());
            }
        }
        Log.d("Duo Match", "Player " + playerId + " won the Duo Race");
        raceStarterPid = -1;
    }

    public int getWinnerId() {
        for (int pid = 0; pid < hands.size(); pid++) {
            List<DuoCard> h = hands.get(pid);
            if (h.size() == 0) return pid;
        }
        return -1;
    }
}
