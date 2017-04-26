package de.fosd.jdime.stats;

import java.util.concurrent.TimeUnit;

/**
 * A measured runtime.
 */
public final class Runtime {

    /**
     * The label to be used for the {@link Runtime} measuring the time it took to perform a merge.
     */
    public static final String MERGE_LABEL = "merge";

    /**
     * This value indicates that no runtime has been stored in a {@link Runtime}.
     */
    public static final long NO_MEASUREMENT = -1;

    /**
     * A currently runtime measurement. Calling {@link #stop()} or the {@link #close()} method will set the runtime of
     * the {@link Runtime} object this {@link Measurement} belongs to.
     */
    public final static class Measurement implements AutoCloseable {

        private final Runtime rt;
        private long startTime;

        /**
         * Constructs a new {@link Measurement}.
         *
         * @param rt
         *         the {@link Runtime} to store measured runtimes in
         */
        private Measurement(Runtime rt) {
            this.rt = rt;
            this.startTime = System.nanoTime();
        }

        /**
         * Stores the runtime measurement in the associated {@link Runtime} and returns it.
         *
         * @return the measured runtime in milliseconds
         */
        public long stop() {
            rt.setTimeMS(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
            return rt.getTimeMS();
        }

        /**
         * Calls {@link #stop()}.
         */
        @Override
        public void close() {
            stop();
        }
    }

    private final String label;
    private long timeMS;

    /**
     * Constructs a new {@link Runtime} with the given label.
     *
     * @param label the label to use
     */
    public Runtime(String label) {
        this.label = label;
        this.timeMS = NO_MEASUREMENT;
    }

    /**
     * Copy constructor.
     *
     * @param toCopy the {@link Runtime} to copy
     */
    public Runtime(Runtime toCopy) {
        this.label = toCopy.label;
        this.timeMS = toCopy.timeMS;
    }

    /**
     * Starts a runtime measurement. Calling {@link Measurement#stop()} or {@link Measurement#close()} on the returned
     * {@link Measurement} will set the value of this {@link Runtime} to the elapsed time in milliseconds.
     *
     * @return the new runtime {@link Measurement}
     */
    public Measurement time() {
        return new Measurement(this);
    }

    /**
     * Adds the given {@link Runtime} to this {@link Runtime}. The 'no measurement' value {@value #NO_MEASUREMENT} will
     * be treated as 0.
     *
     * @param toAdd
     *         the {@link Runtime} to add
     * @throws IllegalArgumentException
     *         if the given {@code toAdd} does not have the same label as this {@link Runtime}
     */
    public void add(Runtime toAdd) {

        if (!label.equals(toAdd.label)) {
            throw new IllegalArgumentException("The label of the Runtime to add (" + toAdd.label + ") does not " +
                    "match the label of this Runtime (" + label + ").");
        }

        if (timeMS != NO_MEASUREMENT || toAdd.timeMS != NO_MEASUREMENT) {
            long l = timeMS == NO_MEASUREMENT ? 0 : timeMS;
            long r = toAdd.timeMS == NO_MEASUREMENT ? 0 : toAdd.timeMS;
            timeMS = l + r;
        }
    }

    /**
     * Returns the label associated with this {@link Runtime}.
     *
     * @return the label of this {@link Runtime}
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the last runtime measurement that was stored in this {@link Runtime}. Returns {@value #NO_MEASUREMENT}
     * if no measurement has been stored yet.
     *
     * @return the last stored runtime in milliseconds or {@value #NO_MEASUREMENT}
     */
    public long getTimeMS() {
        return timeMS;
    }

    /**
     * Sets the stored runtime to the new value in milliseconds.
     *
     * @param timeMS
     *         the new runtime in milliseconds
     * @throws IllegalArgumentException
     *         if the {@code timeMS} is smaller than 0
     */
    public void setTimeMS(long timeMS) {

        if (timeMS < 0) {
            throw new IllegalArgumentException("New runtime (" + timeMS + ") must not be smaller than zero.");
        }

        this.timeMS = timeMS;
    }
}
