package com.hydrann.controller;

import com.hydrann.annheads.ANNHead;
import com.hydrann.annheads.Neighbor;
import com.hydrann.vectorstore.SearchResult;
import com.hydrann.vectorstore.VectorStore;

import java.util.ArrayList;
import java.util.List;

public class IndexManager {
    private final VectorStore store;
    private volatile ANNHead active;
    private int watermark = -1;   // highest store id the active head has seen

    public IndexManager(VectorStore store, ANNHead initial) {
        this.store = store;
        this.active = initial;
        initial.build();
        this.watermark = store.idLimit() - 1;
    }

    /** Call after every ingestion batch: feeds new vectors to the active head. */
    public synchronized void catchUp() {
        ANNHead head = active;
        for (int id = watermark + 1; id < store.idLimit(); id++) {
            if (store.isActive(id)) head.insert(id, store.vector(id));
        }
        watermark = store.idLimit() - 1;
    }

    public List<SearchResult> search(float[] query, int k) {
        ANNHead head = active;
        int fetch = k;
        while (true) {
            List<Neighbor> hits = head.search(query, fetch);
            List<SearchResult> out = new ArrayList<>();
            for (Neighbor n : hits) {
                if (store.isActive(n.id())) {              // drop soft-deleted chunks
                    out.add(new SearchResult(store.get(n.id()), n.score()));
                    if (out.size() == k) return out;
                }
            }
            if (hits.size() < fetch || fetch >= store.idLimit()) return out; // nothing more to find
            fetch *= 2;                                    // deleted ids ate into the top-k; widen
        }
    }

    public ANNHead activeHead() { return active; }
}