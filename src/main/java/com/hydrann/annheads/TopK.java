package com.hydrann.annheads;

import java.util.*;

final class TopK {
    private final int k;
    private final PriorityQueue<Neighbor> heap =
            new PriorityQueue<>(Comparator.comparingDouble(Neighbor::score)); // min-heap

    TopK(int k) { this.k = k; }

    void offer(int id, double score) {
        if (heap.size() < k) heap.add(new Neighbor(id, score));
        else if (score > heap.peek().score()) { heap.poll(); heap.add(new Neighbor(id, score)); }
    }

    List<Neighbor> toSortedList() {
        List<Neighbor> out = new ArrayList<>(heap);
        out.sort(Comparator.comparingDouble(Neighbor::score).reversed());
        return out;
    }
}