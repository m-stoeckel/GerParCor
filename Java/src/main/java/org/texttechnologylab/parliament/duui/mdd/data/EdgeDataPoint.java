package org.texttechnologylab.parliament.duui.mdd.data;

import java.util.ArrayList;
import java.util.List;

public class EdgeDataPoint extends SentenceDataPoint {

    protected final ArrayList<Integer> dependencyDistances;

    public EdgeDataPoint() {
        this.dependencyDistances = new ArrayList<>();
    }

    public void add(int distance) {
        this.dependencyDistances.add(distance);
    }

    public int getDependencyDistanceSum() {
        return this.dependencyDistances.stream().reduce(0, (a, b) -> a + b);
    }

    public int getSentenceLength() {
        return this.dependencyDistances.size();
    }

    public final List<Integer> getDependencyDistances() {
        return this.dependencyDistances;
    }
}
