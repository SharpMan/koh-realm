package koh.realm.handlers;

@FunctionalInterface
public interface HandleMethod<E, S> {

    /**
     *
     * @param emitter Emitter of the event to handle
     * @param source Source of the event
     * @return if false, abort handlers propagation
     */
    boolean handle(E emitter, S source) throws Throwable;
}
