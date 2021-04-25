package de.dytanic.cloudnet.driver.event.invoker;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.EventListenerException;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Generates {@link ListenerInvoker} implementations for certain event handler methods.
 *
 * @see ListenerInvoker
 */
public class ListenerInvokerGenerator {

    private static final String GENERATED_CLASS_TEMPLATE = "GeneratedListenerInvoker_%s";

    private static final String LISTENER_FIELD_NAME = "listener";

    private static final String INVOKE_METHOD_NAME = "invoke";

    private final ClassPool classPool;

    private final Map<ClassLoader, ListenerInvokerClassLoader> invokerClassLoaders;

    public ListenerInvokerGenerator() {
        this.classPool = new ClassPool(ClassPool.getDefault());
        this.invokerClassLoaders = new HashMap<>();
    }

    /**
     * Generates a new {@link ListenerInvoker}.
     *
     * @param listener   The listener object the event listener method is in
     * @param methodName The name of the event listener method
     * @param eventClass The class of the event the listener method is handling
     * @return The new generated {@link ListenerInvoker}, being able the invoke the event listener method.
     */
    public ListenerInvoker generate(Object listener, String methodName, Class<? extends Event> eventClass) {
        Class<?> listenerClass = listener.getClass();
        String listenerClassName = listenerClass.getName();
        String className = String.format(GENERATED_CLASS_TEMPLATE, UUID.randomUUID().toString().replace("-", ""));

        try {
            if (!Modifier.isPublic(listenerClass.getModifiers())) {
                throw new IllegalStateException(String.format("Listener class %s has to be public", listenerClassName));
            }
            if (!Modifier.isPublic(eventClass.getModifiers())) {
                throw new IllegalStateException(String.format("Event class %s has to be public", eventClass.getName()));
            }

            ListenerInvokerClassLoader invokerClassLoader = this.invokerClassLoaders.computeIfAbsent(
                    listenerClass.getClassLoader(),
                    ListenerInvokerClassLoader::new);

            // listener classes might be loaded by another class loader (for example module listeners), add them to the
            // class path of the class pool
            this.classPool.appendClassPath(new LoaderClassPath(invokerClassLoader));

            CtClass listenerInvokerClass = this.classPool.makeClass(className);
            listenerInvokerClass.addInterface(this.classPool.get(ListenerInvoker.class.getName()));

            listenerInvokerClass.addField(this.generateListenerField(listenerInvokerClass, listenerClassName));
            listenerInvokerClass.addConstructor(this.generateInvokerConstructor(listenerInvokerClass, listenerClassName));
            listenerInvokerClass.addMethod(this.generateInvokeImplementation(listenerInvokerClass, methodName, eventClass.getName()));

            Class<?> generatedListenerInvokerClass = invokerClassLoader.defineClass(className, listenerInvokerClass.toBytecode());

            Constructor<?> constructor = generatedListenerInvokerClass.getDeclaredConstructor(listenerClass);
            return (ListenerInvoker) constructor.newInstance(listener);
        } catch (Exception exception) {
            throw new EventListenerException(String.format(
                    "Failed to generate invoker for listener method %s:%s",
                    listenerClassName,
                    methodName), exception);
        }
    }

    private CtField generateListenerField(CtClass listenerInvokerClass, String listenerClassName) throws CannotCompileException {
        return CtField.make(
                String.format("private final %s %s;", listenerClassName, LISTENER_FIELD_NAME),
                listenerInvokerClass);
    }

    private CtConstructor generateInvokerConstructor(CtClass listenerInvokerClass, String listenerClassName) throws CannotCompileException {
        return CtNewConstructor.make(String.format(
                "public %s(%s %s) { this.%s = %s; }",
                listenerInvokerClass.getSimpleName(),
                listenerClassName,
                LISTENER_FIELD_NAME,
                LISTENER_FIELD_NAME,
                LISTENER_FIELD_NAME), listenerInvokerClass);
    }

    private CtMethod generateInvokeImplementation(CtClass listenerInvokerClass, String methodName, String eventClassName) throws CannotCompileException {
        return CtNewMethod.make(String.format(
                "public void %s(%s event) { this.%s.%s((%s) event); }",
                INVOKE_METHOD_NAME,
                Event.class.getName(),
                LISTENER_FIELD_NAME,
                methodName,
                eventClassName
        ), listenerInvokerClass);
    }
}
