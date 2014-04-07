package qwaker00;

import java.io.Serializable;

class GFHolder implements Serializable {
    double[] hits = new double[31];
    final double HISTORY = 50.;

    public int length() {
        return hits.length;
    }

    public void Hit(int gfIndex) {
        hits[gfIndex] = (hits[gfIndex] * HISTORY + 1.) / (HISTORY + 1);
        for (int i = 0; i < 31; ++i) if (i != gfIndex) {
            hits[i] = hits[i] * HISTORY / (HISTORY + 1);
        }
    }

    public int Predict() {
        int gfMax = hits.length / 2;

        double maxCost = 0;
        for (int i = 0; i < hits.length; ++i) {
            double cost = 0, d = Math.min(30 - i, Math.min(i, 0));

            if (d == 0) cost = hits[i];else
                for (int j = 0; j < hits.length; ++j) {
                    cost += hits[j] * 1. / (d * 2 * Math.PI) * Math.exp(-(j - i)*(j - i) / (2 * d * d));
                }

            if (cost > maxCost) {
                gfMax = i;
                maxCost = cost;
            }
        }

        return gfMax;
    }
}
