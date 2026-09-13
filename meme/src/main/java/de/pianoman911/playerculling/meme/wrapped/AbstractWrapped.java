package de.pianoman911.playerculling.meme.wrapped;

public abstract class AbstractWrapped {

    protected Class<?> wrappedClass;

    public record Instance<T extends AbstractWrapped>(Object wrappedInstance) {

    }
}
