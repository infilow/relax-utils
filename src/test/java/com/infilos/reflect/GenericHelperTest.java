package com.infilos.reflect;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class GenericHelperTest extends Assert {

    @Test
    public void testGetFieldType() throws Exception {
        assertEquals(URI.class, GenericHelper.getType(Orange.class.getField("uris")));
    }

    @Test
    public void testGetMethodParameterType() throws Exception {
        final Parameter param = ClassHelper.findParams(Orange.class.getMethod("set", List.class)).iterator().next();
        assertEquals(Integer.class, GenericHelper.getType(param));
    }

    @Test
    public void testGetConstructorParameterType() throws Exception {
        final Parameter param = ClassHelper.findParams(Orange.class.getConstructor(Queue.class)).iterator().next();
        assertEquals(URI.class, GenericHelper.getType(param));
    }

    @Test
    public void testGetReturnType() throws Exception {
        assertEquals(URL.class, GenericHelper.getReturnType(Orange.class.getMethod("urls")));
    }

    public static class Orange {

        public Collection<URI> uris;

        public Orange(final Queue<URI> uris) {
            this.uris = uris;
        }

        public Set<URL> urls() {
            return null;
        }

        public void set(final List<Integer> integers) {
        }
    }


    @Test
    public void testGetInterfaceParameter() throws Exception {
        class URIConsumer implements Consumer<URI> {
            @Override
            public void accept(URI uri) {
            }
        }

        final Type[] interfaceTypes = GenericHelper.getInterfaceTypes(Consumer.class, URIConsumer.class);

        // Consumer has only one parameter, so we are expecting one type
        assertEquals(1, interfaceTypes.length);

        // The type we're expecting is URI
        assertEquals(URI.class, interfaceTypes[0]);
    }

    @Test
    public void parametersSpecifiedByParent() throws Exception {
        class URIConsumer implements Consumer<URI> {
            @Override
            public void accept(URI uri) {
            }
        }

        class SpecializedConsumer extends URIConsumer {
        }

        final Type[] interfaceTypes = GenericHelper.getInterfaceTypes(Consumer.class, SpecializedConsumer.class);

        // Consumer has only one parameter, so we are expecting one type
        assertEquals(1, interfaceTypes.length);

        // The type we're expecting is URI
        assertEquals(URI.class, interfaceTypes[0]);
    }

    /**
     * Scenario: our parent class implemented the generic interface and did not specify the actual type either.  The actual type is declared by the subclass.
     */
    @Test
    public void parametersDeferredByParent() {

        class URIConsumer<T> implements Consumer<T> {
            @Override
            public void accept(T uri) {
            }
        }

        class SpecializedConsumer extends URIConsumer<URI> {
        }

        final Type[] interfaceTypes = GenericHelper.getInterfaceTypes(Consumer.class, SpecializedConsumer.class);

        // Consumer has only one parameter, so we are expecting one type
        assertEquals(1, interfaceTypes.length);

        // The type we're expecting is URI
        assertEquals(URI.class, interfaceTypes[0]);
    }

    /**
     * Scenario: our parent class implemented the generic interface and did not specify the actual type either.  The actual type is declared by the subclass.
     */
    @Test
    public void parametersDeferredByParentOfParent() {

        class URIConsumer<T> implements Consumer<T> {
            @Override
            public void accept(T uri) {
            }
        }

        class SpecializedConsumer<V> extends URIConsumer<V> {
        }

        class VerySpecializedConsumer extends SpecializedConsumer<URI> {
        }

        final Type[] interfaceTypes = GenericHelper.getInterfaceTypes(Consumer.class, VerySpecializedConsumer.class);

        // Consumer has only one parameter, so we are expecting one type
        assertEquals(1, interfaceTypes.length);

        // The type we're expecting is URI
        assertEquals(URI.class, interfaceTypes[0]);
    }

    /**
     * The interface we are after is coming to us from another interface we implement.  Let's ensure we can resolve it.
     */
    @Test
    public void interfaceInheritance() {

        class URIConsumer implements ImprovedConsumer<URI> {
            @Override
            public void accept(URI uri) {

            }
        }

        final Type[] interfaceTypes = GenericHelper.getInterfaceTypes(Consumer.class, URIConsumer.class);

        // Consumer has only one parameter, so we are expecting one type
        assertEquals(1, interfaceTypes.length);

        // The type we're expecting is URI
        assertEquals(URI.class, interfaceTypes[0]);
    }

    interface ImprovedConsumer<T> extends Consumer<T> {
    }


    /**
     * Our parent has a type variable that maps to a type variable of one of its interfaces that itself maps to an interface
     */
    @Test
    public void interfaceInheritanceVariable() {

        class URIConsumer<R> implements ImprovedConsumer<R> {
            @Override
            public void accept(R uri) {

            }
        }

        class SpecializedConsumer extends URIConsumer<URI> {
        }

        final Type[] interfaceTypes = GenericHelper.getInterfaceTypes(Consumer.class, SpecializedConsumer.class);

        // Consumer has only one parameter, so we are expecting one type
        assertEquals(1, interfaceTypes.length);

        // The type we're expecting is URI
        assertEquals(URI.class, interfaceTypes[0]);
    }

    /**
     * Our parent has a type variable that maps to a type variable of one of its interfaces that itself maps to an interface
     */
    @Test
    public void unrelatedGenericInterfacesAreIgnored() {

        class URIConsumer implements Consumer<URI>, Function<URL, File> {
            @Override
            public void accept(URI uri) {
            }

            @Override
            public File apply(URL url) {
                return null;
            }
        }

        final Type[] interfaceTypes = GenericHelper.getInterfaceTypes(Consumer.class, URIConsumer.class);

        // Consumer has only one parameter, so we are expecting one type
        assertEquals(1, interfaceTypes.length);

        // The type we're expecting is URI
        assertEquals(URI.class, interfaceTypes[0]);
    }

    @Test
    public void multipleParametersAreSupported() {

        class URIConsumer implements Consumer<URI>, Function<URL, File> {
            @Override
            public void accept(URI uri) {
            }

            @Override
            public File apply(URL url) {
                return null;
            }
        }

        final Type[] interfaceTypes = GenericHelper.getInterfaceTypes(Function.class, URIConsumer.class);

        // Consumer has only one parameter, so we are expecting one type
        assertEquals(2, interfaceTypes.length);

        // The type we're expecting is URI
        assertEquals(URL.class, interfaceTypes[0]);
        assertEquals(File.class, interfaceTypes[1]);
    }

    /**
     * URIConsumer defines only one of the two type parameters, the other is left a type variable to be defined by the subclass.
     *
     * Verify we can handle resolving the variables
     */
    @Test
    public void mixOfDirectlyAndIndirectlyDefinedParameters() {

        class URIConsumer<I> implements Consumer<URI>, Function<I, File> {
            @Override
            public void accept(URI uri) {
            }

            @Override
            public File apply(I url) {
                return null;
            }
        }

        class SpecializedURIConsumer extends URIConsumer<URL> {
        }

        final Type[] interfaceTypes = GenericHelper.getInterfaceTypes(Function.class, SpecializedURIConsumer.class);

        // Consumer has only one parameter, so we are expecting one type
        assertEquals(2, interfaceTypes.length);

        // The type we're expecting is URI
        assertEquals(URL.class, interfaceTypes[0]);
        assertEquals(File.class, interfaceTypes[1]);
    }

    @Test
    public void typeParameterValueHasTypeParameter() {

        class FunctionConsumer implements Consumer<Function<URL, File>> {
            @Override
            public void accept(Function<URL, File> urlFileFunction) {

            }
        }

        final Type[] interfaceTypes = GenericHelper.getInterfaceTypes(Consumer.class, FunctionConsumer.class);

        // Consumer has only one parameter, so we are expecting one type
        assertEquals(1, interfaceTypes.length);

        // The type we're expecting is URI
        assertTrue(interfaceTypes[0] instanceof ParameterizedType);

        final ParameterizedType functionType = (ParameterizedType) interfaceTypes[0];

        assertEquals(Function.class, functionType.getRawType());
        assertEquals(URL.class, functionType.getActualTypeArguments()[0]);
        assertEquals(File.class, functionType.getActualTypeArguments()[1]);
    }

    @Test
    public void canResolveTypesOfParameterizedType() {

        class FunctionConsumer<V> implements Consumer<Function<V, File>> {
            @Override
            public void accept(Function<V, File> urlFileFunction) {

            }
        }

        class SpecializedFunctionConsumer extends FunctionConsumer<URL> {
            @Override
            public void accept(Function<URL, File> urlFileFunction) {

            }
        }

        final Type[] interfaceTypes = GenericHelper.getInterfaceTypes(Consumer.class, SpecializedFunctionConsumer.class);

        // Consumer has only one parameter, so we are expecting one type
        assertEquals(1, interfaceTypes.length);

        // Function is a parameterized type
        assertTrue(interfaceTypes[0] instanceof ParameterizedType);
        final ParameterizedType functionType = (ParameterizedType) interfaceTypes[0];
        assertEquals(Function.class, functionType.getRawType());
        assertEquals(URL.class, functionType.getActualTypeArguments()[0]);
        assertEquals(File.class, functionType.getActualTypeArguments()[1]);
    }

    /**
     * If the specified class does not implement the interface, null will be returned
     */
    @Test
    public void interfaceNotImplemented() {

        class URIConsumer implements Consumer<URI> {
            @Override
            public void accept(URI uri) {
            }
        }

        final Type[] interfaceTypes = GenericHelper.getInterfaceTypes(Function.class, URIConsumer.class);

        // Consumer has only one parameter, so we are expecting one type
        assertNull(interfaceTypes);
    }
}