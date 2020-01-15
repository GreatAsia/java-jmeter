package com.okay;

import org.apache.jmeter.samplers.SampleResult;

import java.text.DecimalFormat;

/**
 * @author zhou
 * @date 2019/12/26
 */
public class SummariserRunningSample {

    private final DecimalFormat errorFormatter = new DecimalFormat("#0.00%");
    private final String label;
    private long counter;
    private long runningSum;
    private long max;
    private long min;
    private long errorCount;
    private long startTime;
    private long endTime;

    public SummariserRunningSample(String label) {
        this.label = label;
        this.init();
    }

    public SummariserRunningSample(SummariserRunningSample src) {
        this.label = src.label;
        this.counter = src.counter;
        this.errorCount = src.errorCount;
        this.startTime = src.startTime;
        this.endTime = src.endTime;
        this.max = src.max;
        this.min = src.min;
        this.runningSum = src.runningSum;
    }

    private void init() {
        this.counter = 0L;
        this.runningSum = 0L;
        this.max = -9223372036854775808L;
        this.min = 9223372036854775807L;
        this.errorCount = 0L;
        this.startTime = System.currentTimeMillis();
        this.endTime = this.startTime;
    }

    public void clear() {
        this.init();
    }

    public void addSample(SummariserRunningSample rs) {
        this.counter += rs.counter;
        this.errorCount += rs.errorCount;
        this.runningSum += rs.runningSum;
        if (this.max < rs.max) {
            this.max = rs.max;
        }

        if (this.min > rs.min) {
            this.min = rs.min;
        }

        this.endTime = System.currentTimeMillis();
    }

    public void addSample(SampleResult res) {
        this.counter += (long) res.getSampleCount();
        this.errorCount += (long) res.getErrorCount();
        long aTimeInMillis = res.getTime();
        this.runningSum += aTimeInMillis;
        if (aTimeInMillis > this.max) {
            this.max = aTimeInMillis;
        }

        if (aTimeInMillis < this.min) {
            this.min = aTimeInMillis;
        }

        this.endTime = System.currentTimeMillis();
    }

    public long getNumSamples() {
        return this.counter;
    }

    public long getElapsed() {
        return this.counter == 0L ? 0L : this.endTime - this.startTime;
    }

    public double getRate() {
        if (this.counter == 0L) {
            return 0.0D;
        } else {
            long howLongRunning = this.endTime - this.startTime;
            return howLongRunning == 0L ? 1.7976931348623157E308D : (double) this.counter / (double) howLongRunning * 1000.0D;
        }
    }

    public long getAverage() {
        return this.counter == 0L ? 0L : this.runningSum / this.counter;
    }

    public long getErrorCount() {
        return this.errorCount;
    }

    public String getErrorPercentageString() {
        return this.errorFormatter.format(this.getErrorPercentage());
    }

    public double getErrorPercentage() {
        if (this.counter == 0L) {
            return 0.0D;
        } else {
            double rval = (double) this.errorCount / (double) this.counter;
            return rval;
        }
    }

    public long getMax() {
        return this.max;
    }

    public long getMin() {
        return this.min;
    }

    public void setEndTime() {
        this.endTime = System.currentTimeMillis();
    }
}


