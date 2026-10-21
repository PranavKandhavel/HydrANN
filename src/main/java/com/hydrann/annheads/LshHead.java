package com.hydrann.annheads;

import com.hydrann.similarity.CosineSimilarity;
import com.hydrann.vectorstore.VectorStore;

import java.util.*;

public class LshHead implements ANNHead {
    private final VectorStore store;
    private final int dim, numTables, bitsPerTable;
    private final boolean multiProbe;
    private final float[][][] planes;                                   // [table][bit][dim]
    private final List<Map<Integer, List<Integer>>> tables = new ArrayList<>();
    private int indexedCount = 0;

    public LshHead(VectorStore store, int dim, int numTables,
                   int bitsPerTable, boolean multiProbe, long seed) {
        if (bitsPerTable < 1 || bitsPerTable > 30)
            throw new IllegalArgumentException("bitsPerTable must be 1..30");
        this.store = store;
        this.dim = dim;
        this.numTables = numTables;
        this.bitsPerTable = bitsPerTable;
        this.multiProbe = multiProbe;

        Random rnd = new Random(seed);             // keep the seed: needed to reload the index later
        planes = new float[numTables][bitsPerTable][dim];
        for (int t = 0; t < numTables; t++) {
            for (int b = 0; b < bitsPerTable; b++)
                for (int d = 0; d < dim; d++)
                    planes[t][b][d] = (float) rnd.nextGaussian();
            tables.add(new HashMap<>());
        }
    }

    @Override public String name() { return "lsh"; }

    private int hash(int table, float[] v) {
        int code = 0;
        for (int b = 0; b < bitsPerTable; b++)
            if (CosineSimilarity.dot(planes[table][b], v) >= 0) code |= (1 << b);
        return code;
    }

    @Override
    public void build() {
        for (Map<Integer, List<Integer>> m : tables) m.clear();
        indexedCount = 0;
        for (int id = 0; id < store.idLimit(); id++)
            if (store.isActive(id)) insert(id, store.vector(id));
    }

    @Override
    public void insert(int id, float[] v) {
        for (int t = 0; t < numTables; t++)
            tables.get(t).computeIfAbsent(hash(t, v), c -> new ArrayList<>()).add(id);
        indexedCount++;
    }

    @Override
    public List<Neighbor> search(float[] q, int k) {
        Set<Integer> candidates = new HashSet<>();
        for (int t = 0; t < numTables; t++) {
            int code = hash(t, q);
            addBucket(t, code, candidates);
            if (multiProbe)
                for (int b = 0; b < bitsPerTable; b++)
                    addBucket(t, code ^ (1 << b), candidates);
        }
        TopK top = new TopK(k);
        for (int id : candidates) {
            if (!store.isActive(id)) continue;         // skip ghosts of deactivated documents
            top.offer(id, CosineSimilarity.dot(q, store.vector(id)));
        }
        return top.toSortedList();
    }

    private void addBucket(int table, int code, Set<Integer> out) {
        List<Integer> bucket = tables.get(table).get(code);
        if (bucket != null) out.addAll(bucket);
    }

    @Override
    public long memoryBytes() {   // rough estimate: hyperplanes + ~20 bytes per boxed bucket entry
        return (long) numTables * bitsPerTable * dim * 4 + (long) indexedCount * numTables * 20;
    }
}