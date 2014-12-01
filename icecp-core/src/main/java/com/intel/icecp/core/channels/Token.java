/*
 * ******************************************************************************
 *
 * INTEL CONFIDENTIAL
 *
 * Copyright 2013 - 2016 Intel Corporation All Rights Reserved.
 *
 * The source code contained or described herein and all documents related to
 * the source code ("Material") are owned by Intel Corporation or its suppliers
 * or licensors. Title to the Material remains with Intel Corporation or its
 * suppliers and licensors. The Material contains trade secrets and proprietary
 * and confidential information of Intel or its suppliers and licensors. The
 * Material is protected by worldwide copyright and trade secret laws and treaty
 * provisions. No part of the Material may be used, copied, reproduced,
 * modified, published, uploaded, posted, transmitted, distributed, or disclosed
 * in any way without Intel's prior express written permission.
 *
 * No license under any patent, copyright, trade secret or other intellectual
 * property right is granted to or conferred upon you by disclosure or delivery
 * of the Materials, either expressly, by implication, inducement, estoppel or
 * otherwise. Any license under such intellectual property rights must be
 * express and approved by Intel in writing.
 *
 * Unless otherwise agreed by Intel in writing, you may not remove or alter this
 * notice or any other notice embedded in Materials by Intel or Intel's
 * suppliers or licensors in any way.
 *
 * ******************************************************************************
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