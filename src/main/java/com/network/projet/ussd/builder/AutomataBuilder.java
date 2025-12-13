package com.network.projet.ussd.builder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Builder for constructing state machines (automata)
 */
public class AutomataBuilder {
    private String automataId;
    private String name;
    private Set<State> states;
    private Set<Transition> transitions;
    private String initialStateId;
    private Set<String> finalStateIds;

    public AutomataBuilder() {
        this.states = new HashSet<>();
        this.transitions = new HashSet<>();
        this.finalStateIds = new HashSet<>();
    }

    public AutomataBuilder automataId(String automataId) {
        this.automataId = automataId;
        return this;
    }

    public AutomataBuilder name(String name) {
        this.name = name;
        return this;
    }

    public AutomataBuilder addState(String stateId, String label) {
        states.add(new State(stateId, label));
        return this;
    }

    public AutomataBuilder addState(State state) {
        states.add(state);
        return this;
    }

    public AutomataBuilder addTransition(String fromStateId, String toStateId, String trigger) {
        transitions.add(new Transition(fromStateId, toStateId, trigger));
        return this;
    }

    public AutomataBuilder addTransition(Transition transition) {
        transitions.add(transition);
        return this;
    }

    public AutomataBuilder initialState(String stateId) {
        this.initialStateId = stateId;
        return this;
    }

    public AutomataBuilder addFinalState(String stateId) {
        this.finalStateIds.add(stateId);
        return this;
    }

    public Automata build() {
        return new Automata(automataId, name, states, transitions, initialStateId, finalStateIds);
    }

    /**
     * Inner class representing a state in the automata
     */
    public static class State {
        private String stateId;
        private String label;

        public State(String stateId, String label) {
            this.stateId = stateId;
            this.label = label;
        }

        public String getStateId() {
            return stateId;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            State state = (State) o;
            return Objects.equals(stateId, state.stateId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stateId);
        }
    }

    /**
     * Inner class representing a transition in the automata
     */
    public static class Transition {
        private String fromStateId;
        private String toStateId;
        private String trigger;

        public Transition(String fromStateId, String toStateId, String trigger) {
            this.fromStateId = fromStateId;
            this.toStateId = toStateId;
            this.trigger = trigger;
        }

        public String getFromStateId() {
            return fromStateId;
        }

        public String getToStateId() {
            return toStateId;
        }

        public String getTrigger() {
            return trigger;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Transition that = (Transition) o;
            return Objects.equals(fromStateId, that.fromStateId) &&
                   Objects.equals(toStateId, that.toStateId) &&
                   Objects.equals(trigger, that.trigger);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fromStateId, toStateId, trigger);
        }
    }

    /**
     * Inner class representing the built automata
     */
    public static class Automata {
        private String automataId;
        private String name;
        private Set<State> states;
        private Set<Transition> transitions;
        private String initialStateId;
        private Set<String> finalStateIds;

        public Automata(String automataId, String name, Set<State> states,
                       Set<Transition> transitions, String initialStateId, Set<String> finalStateIds) {
            this.automataId = automataId;
            this.name = name;
            this.states = new HashSet<>(states);
            this.transitions = new HashSet<>(transitions);
            this.initialStateId = initialStateId;
            this.finalStateIds = new HashSet<>(finalStateIds);
        }

        public String getAutomataId() {
            return automataId;
        }

        public String getName() {
            return name;
        }

        public Set<State> getStates() {
            return Collections.unmodifiableSet(states);
        }

        public Set<Transition> getTransitions() {
            return Collections.unmodifiableSet(transitions);
        }

        public String getInitialStateId() {
            return initialStateId;
        }

        public Set<String> getFinalStateIds() {
            return Collections.unmodifiableSet(finalStateIds);
        }

        @Override
        public String toString() {
            return "Automata{" +
                    "automataId='" + automataId + '\'' +
                    ", name='" + name + '\'' +
                    ", stateCount=" + states.size() +
                    ", transitionCount=" + transitions.size() +
                    '}';
        }
    }
}
