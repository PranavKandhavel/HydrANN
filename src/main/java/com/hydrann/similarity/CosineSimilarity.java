package com.hydrann.similarity;

public final class CosineSimilarity {
    private CosineSimilarity() {}

    /** Valid as cosine similarity because MiniLM vectors are L2-normalized. */
    public static double dot(float[] a, float[] b) {
    double sum = 0.0;
    for (int i = 0; i < a.length; i++) sum += (double) a[i] * b[i];
    return sum;
}

}