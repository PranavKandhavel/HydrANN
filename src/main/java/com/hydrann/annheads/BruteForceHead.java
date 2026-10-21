package com.hydrann.annheads;

import com.hydrann.similarity.CosineSimilarity;
import com.hydrann.vectorstore.VectorStore;

import java.util.*;

public class BruteForceHead implements ANNHead {
    private final VectorStore store;

    public BruteForceHead(VectorStore store) { this.store = store; }

    @Override public String name() { return "brute-force"; }
    @Override public void build() { /* nothing to build */ }
    @Override public void insert(int id, float[] vector) { /* store is the index */ }
    @Override public long memoryBytes() { return 0; }

    @Override
    public List<Neighbor> search(float[] query, int k) {
        PriorityQueue<Neighbor> heap =
                new PriorityQueue<>(Comparator.comparingDouble(Neighbor::score)); // min-heap

        for (int id = 0; id < store.idLimit(); id++) {
            if (!store.isActive(id)) continue;
            double s = CosineSimilarity.dot(query, store.vector(id));
            if (heap.size() < k) {
                heap.add(new Neighbor(id, s));
            } else if (s > heap.peek().score()) {
                heap.poll();
                heap.add(new Neighbor(id, s));
            }
        }

        List<Neighbor> out = new ArrayList<>(heap);
        out.sort(Comparator.comparingDouble(Neighbor::score).reversed());
        return out;
    }
}