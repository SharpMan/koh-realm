package koh.realm.handlers;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 *
 * @param <E> emitter
 * @param <S> source
 */
public class HandlersProvider<E, S> extends AbstractModule {

    private final Injector parentInjector;

    private final String packageName;
    private final Class<? extends Annotation> attribute;
    private final Class<S> rootClass;

    private final Map<Class<?>, List<HandleMethod<E, S>>> handlers = new HashMap<>();

    public HandlersProvider(Injector parentInjector,
                            String packageName, Class<? extends Annotation> attribute, Class<S> rootClass) {
        this.parentInjector = parentInjector;
        this.packageName = packageName;
        this.attribute = attribute;
        this.rootClass = rootClass;
    }

    public Stream<HandleMethod<E, S>> getLambdas(Class<? extends S> source) {
        List<HandleMethod<E, S>> callbacks = handlers.get(source);
        return callbacks == null
                ? Stream.empty()
                : callbacks.stream();
    }

    public void registerLambda(Class<? extends S> source, HandleMethod<E, S> handle) {
        List<HandleMethod<E, S>> callbacks = handlers.get(source);
        if( callbacks == null) {
            callbacks = new ArrayList<>();
            handlers.put(source, callbacks);
        }
        callbacks.add(handle);
    }

    @Override
    protected void configure() {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(packageName))
                .setScanners(new MethodAnnotationsScanner()));

        reflections.getMethodsAnnotatedWith(attribute).stream().forEach((method) -> {
            if(method.getDeclaringClass() == Handler.class
                    || !Handler.class.isAssignableFrom(method.getDeclaringClass()))
                return;
            if(method.getParameterTypes().length == 2
                    && method.getParameterTypes()[1] != rootClass
                    && rootClass.isAssignableFrom(method.getParameterTypes()[1])) {
                List<HandleMethod<E, S>> callbacks = handlers.get(method.getParameterTypes()[1]);
                if( callbacks == null) {
                    callbacks = new ArrayList<>();
                    handlers.put(method.getParameterTypes()[1], callbacks);
                }
                if(Modifier.isStatic(method.getModifiers()))
                    callbacks.add((E emitter, S source)
                            -> (boolean)method.invoke(null, emitter, source));
                else {
                    if(parentInjector.getBinding(method.getDeclaringClass()) == null)
                        bind(method.getDeclaringClass()).in(Scopes.SINGLETON);
                    callbacks.add((E emitter, S source)
                            -> (boolean)method.invoke(parentInjector.getInstance(method.getDeclaringClass()), emitter, source));
                }
            }
        });
    }
}
