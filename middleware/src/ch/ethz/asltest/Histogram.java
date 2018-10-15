package ch.ethz.asltest;


/**
 * Histogram in 0.1 ms (100 us) steps. The overall range is 5 seconds by default.
 *
 * before aggregate():
 * [0] 0.0 ms <= x < 0.1 ms
 * [1] 0.1 ms <= x < 0.2 ms
 * ...
 * [9999] 999.9 ms <= x < 1000.0 ms
 * [10000] 1000.0 ms <= x < 1000.1 ms
 * ...
 * [49999] 4999.9 ms <= x < 5000.0 ms
 * [50000] 5000.0 ms <= x
 *
 * after aggregate():
 * [0] x < 0.1 ms
 * [1] x < 0.2 ms
 * [2] x < 0.3 ms
 * ...
 * [9999] x < 1000.0 ms
 * [10000] x < 1000.1 ms
 * ...
 * [49999] x < 5000.0 ms
 * [50000] total
 */
class Histogram {
    // frequencies
    private int[] freq;

    // max value
    private int maxVal = -1;

    // the lower limit of the last bin
    private static final int maxThreshold = 50000;

    Histogram() {
        freq = new int[maxThreshold + 1];
    }


    /**
     * put a new record into the histogram
     * don't call this method any more after isAggregated turns true!
     * @param val time in us
     */
    void add(long val) {
        int index = (int) (val / 100);
        if (index >= maxThreshold) {
            ++freq[maxThreshold];
        } else {
            ++freq[index];
        }

        if (index > maxVal) {
            maxVal = index;
        }
    }


    /**
     * Assuming the two Histograms have the same maxThreshold, which is always true in our configuration.
     * @param other another Histogram instance
     */
    void merge(Histogram other) {
        for (int i = 0; i <= maxThreshold; ++i) {
            freq[i] += other.freq[i];
        }

        if (other.maxVal > maxVal) {
            maxVal = other.maxVal;
        }
    }


    String finalString() {
        for (int i = 1; i <= maxThreshold; ++i) {
            freq[i] += freq[i - 1];
        }

        StringBuilder sb = new StringBuilder(
                String.format("=================================\n%8s%12s%10s\n---------------------------------\n",
                "< ms", "Frequency", "Percent"));

        // determine output range
        int minIndex = 0, maxIndex = maxThreshold;
        while (minIndex < maxThreshold && freq[minIndex] == 0) {
            ++minIndex;
        }
        while (maxIndex > 0 && freq[maxIndex - 1] == freq[maxThreshold]) {
            --maxIndex;
        }

        if (minIndex <= maxIndex) {
            for (int i = minIndex; i < maxIndex; ++i) {
                sb.append(String.format("%8.1f%12d%9.2f%%\n",
                        (i + 1) * 0.1, freq[i], ((double)freq[i] / freq[maxThreshold]) * 100));
            }
            sb.append(String.format("%8.1f%12d%9.2f%%\n",
                    maxIndex != maxThreshold ? (maxIndex + 1) * 0.1 : maxVal * 0.1, freq[maxThreshold], 100.00));
        } else {
            sb.append("(empty histogram)\n");
        }
        sb.append("=================================\n");

        return sb.toString();
    }
}