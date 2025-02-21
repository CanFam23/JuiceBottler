/**
 * The {@code Orange} class represents an orange going through different processing states
 * in a factory. Each {@link Orange} starts in the {@link State#Fetched} state and goes
 * through multiple stages.
 *
 * <p>Each state transition is controlled by {@link #runProcess()}, which takes the
 * orange to the next {@link State} and simulates processing time using {@link #doWork()}.</p>
 *
 * <p>The {@link State} enum defines the different stages an orange can go through,
 * with each state having a predefined time required for completion.</p>
 */
public class Orange {
    /**
     * Enumeration used to define different states a {@link Orange} can be in.
     */
    public enum State {
        /** Represents when orange is fetched. */
        Fetched(15),

        /** Represents when orange is peeled. */
        Peeled(38),

        /** Represents when orange is squeezed. */
        Squeezed(29),

        /** Represents when orange is bottled. */
        Bottled(17),

        /** Represents when orange is processed. */
        Processed(1);

        /** Final index of the enum. */
        private static final int finalIndex = State.values().length - 1;

        /** Time it takes to complete the state in milliseconds. */
        final int timeToComplete;

        /**
         * Creates new state enum.
         *
         * @param timeToComplete Time it takes to complete this state (In milliseconds).
         */
        State(int timeToComplete) {
            this.timeToComplete = timeToComplete;
        }

        /** Gets the next state of the orange in {@link Enum#ordinal() ordinal} order. */
        State getNext() {
            final int currIndex = this.ordinal();
            if (currIndex >= finalIndex) {
                throw new IllegalStateException("Already at final state");
            }
            return State.values()[currIndex + 1];
        }
    }

    /** Current {@link Orange.State state} of the orange. */
    private State state;

    /**
     * Creates a new orange object
     * Calls {@link #doWork()} to represent time it takes to fetch the orange.
     */
    public Orange() {
        state = State.Fetched;
        doWork();
    }

    /**
     * Gets the current {@link Orange.State state} of the orange
     *
     * @return Current state of the orange.
     */
    public State getState() {
        return state;
    }

    /** Gets next state of orange and then calls {@link #doWork()}. */
    public void runProcess() {
        // Don't attempt to process an already completed orange
        if (state == State.Processed) {
            throw new IllegalStateException("This orange has already been processed");
        }
        state = state.getNext();
        doWork();
    }

    /** Sleeps current thread for {@link Orange.State#timeToComplete} amount of time. */
    private void doWork() {
        // Sleep for the amount of time necessary to do the work
        try {
            Thread.sleep(state.timeToComplete);
        } catch (InterruptedException e) {
            System.err.println("Incomplete orange processing, juice may be bad");
        }
    }
}
