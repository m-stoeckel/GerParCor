package org.texttechnologylab.parliament.duui.mdd.data;

public class SentenceDataPoint {

    public int rootDistance = -1;
    public int numberOfSyntacticLinks = 0;
    private int dependencyDistanceSum = 0;
    private int sentenceLength = 0;

    public void add(int distance) {
        this.dependencyDistanceSum += distance;
        this.sentenceLength++;
    }

    public double mdd() {
        return ((double) this.getDependencyDistanceSum() / (double) this.getSentenceLength());
    }

    public int getRootDistance() {
        return rootDistance;
    }

    public int getNumberOfSyntacticLinks() {
        return numberOfSyntacticLinks;
    }

    public int getDependencyDistanceSum() {
        return dependencyDistanceSum;
    }

    public int getSentenceLength() {
        return sentenceLength;
    }
}
