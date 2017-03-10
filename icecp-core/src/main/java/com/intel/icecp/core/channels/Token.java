/*
 * Copyright (c) 2017 Intel Corporation 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.icecp.core.channels;

import java.lang.reflect.*;

/**
 * Capture the type parameters of a generic class for use in serializing generic messages. The class is abstract to
 * force an anonymous instantiation, which captures the generic parameters. E.g.:
 * <p>
 * {@code Token messageType = new Token<AttributeMessage<Module.State>>(){}; }
 * <p>
 * TODO equality and hash code methods FIXME more unit tests
 *
 */
public abstract class Token<T> {
    private final Type type;

    /**
     * Build a token; use like {@code new Token<A<B, C>>(){ }; }.
     */
    public Token() {
        // TODO check if a generic superclass exists, prevent {@code new Token(){}; }
        Type t = getClass().getGenericSuperclass();
        this.type = ((ParameterizedType) t).getActualTypeArguments()[0];
    }

    /**
     * Build a token directly from a type.
     *
     * @param type the specified type tree to save
     */
    private Token(Type type) {
        this.type = type;
    }

    /**
     * Build a token from a tree of types
     *
     * @param parent the root type; e.g. {@code A} in {@code A<B, C>}
     * @param children the child types from the root; e.g. {@code [B, C]} in {@code A<B, C>}
     * @return a token with a new type tree (e.g. {@code A<B, C>}) generated from the passed classes
     */
    public static Token fromTree(Type parent, Type... children) {
        return new Token(new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return children;
            }

            @Override
            public Type getRawType() {
                return parent;
            }

            @Override
            public Type getOwnerType() {
                return null; // TODO this assumes that parent is an outer-level class; we may need to implement this for inner/outer class relationships, see http://stackoverflow.com/a/17468590
            }
        }) {
        };
    }

    /**
     * Build a token from a specified type; convenience method for {@link #Token(Type)}.
     *
     * @param someType the type to capture
     * @return a token capturing the passed type
     */
    public static Token of(Type someType) {
        return new Token(someType) {
        };
    }

    /**
     * @return the full type tree for this token; generic parameters should be accessible by casting to {@link
     * ParameterizedType}
     */
    public Type type() {
        return type;
    }

    @Override
    public String toString() {
        return type.toString();
    }

    /**
     * @return the class for the type captured by this token.
     */
    public Class toClass() {
        Type thisType = this.type();
        return toClass(thisType)[0];
    }

    /**
     * Determines if the class or interface represented by this
     * {@code Token} object is either the same as, or is a superclass or
     * superinterface of, the class or interface represented by the specified
     * {@code Token} parameter. It returns {@code true} if so;
     * otherwise it returns {@code false}.
     */
    public boolean isAssignableFrom(Class<?> other) {
        return toClass().isAssignableFrom(other);
    }
    
    /**
     * Determines if the class or interface behind this {@code Token} 
     * object is either the same as, or a superclass, or superinterface of, 
     * the class/interface behind the given {@code Token}.
     * 
     * @param other Token to test
     * @return {@code true} if this is assignable from other; {@code false} otherwise
     */
    public boolean isAssignableFrom(Token<?> other) {
        return isAssignableFrom(other.toClass());
    }
    

    private Class[] toClass(Type t) {
        if (t instanceof GenericArrayType) {
            return toClass(((GenericArrayType) type).getGenericComponentType());
        } else if (t instanceof WildcardType) {
            return toClass((((WildcardType) t).getUpperBounds())[0]);
        } else if (t instanceof TypeVariable) {
            return toClass(((TypeVariable) t).getBounds()[0]);
        } else if (t instanceof ParameterizedType) {
            return toClass(((ParameterizedType) t).getRawType());
        } else if (t instanceof Class) {
            return new Class[]{(Class) t};
        } else {
            return new Class[]{Object.class};
        }
    }
}