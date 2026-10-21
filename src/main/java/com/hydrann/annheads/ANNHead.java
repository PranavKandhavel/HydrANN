package com.hydrann.annheads;

import java.util.List;

public interface ANNHead {
    String name();
    void build();                                // index every active vector currently in the store
    void insert(int id, float[] vector);         // incremental add
    List<Neighbor> search(float[] query, int k); // best first
    long memoryBytes();                          // estimate, used by the fitness function
}