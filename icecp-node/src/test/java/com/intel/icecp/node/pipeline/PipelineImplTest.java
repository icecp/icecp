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
package com.intel.icecp.node.pipeline;

import com.intel.icecp.core.pipeline.Operation;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.core.pipeline.exception.PipelineException;
import java.util.Objects;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for class {@link com.intel.icecp.node.pipeline.PipelineImpl}
 *
 */
public class PipelineImplTest {

    // ******************* Classes used for test *******************/
    class A {
    }

    class B extends A {

        public String s;

        public B() {
        }

        public B(String s) {
            this.s = s;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof B)) {
                return false;
            }
            return ((B) o).s.equals(this.s);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.s);
            return hash;
        }

    }

    class OpStringToB extends Operation<String, B> {

        public OpStringToB() {
            super(String.class, B.class);
        }

        

        @Override
        public B execute(String input) {
            return new B(input);
        }

        @Override
        public String executeInverse(B input) {
            return input.s;
        }

    }

    class OpBToString extends Operation<B, String> {

        public OpBToString() {
            super(B.class, String.class);
        }

        @Override
        public String execute(B input) {
            return "great";
        }

        @Override
        public B executeInverse(String input) {
            return new B();
        }

    }
    
    class OpAToString extends Operation<A, String> {

        public OpAToString() {
            super(A.class, String.class);
        }

        @Override
        public String execute(A input) {
            return input.getClass().getName();
        }

        @Override
        public A executeInverse(String input) {
            return new A();
        }

    }

    class OpStringToA extends Operation<String, A> {

        public OpStringToA() {
            super(String.class, A.class);
        }

        @Override
        public A execute(String input) {
            return new A();
        }

        @Override
        public String executeInverse(A input) {
            return "output";
        }

    }
    
    class OpStringToString extends Operation<String, String> {

        public OpStringToString() {
            super(String.class, String.class);
        }

        @Override
        public String execute(String input) {
            return input;
        }

        @Override
        public String executeInverse(String input) {
            return input;
        }

    }
    
    
    @Test(expected = PipelineException.class)
    public void nonExecutableTest1() throws PipelineException {
        Pipeline<Integer, B> pipeline = new PipelineImpl<>(Integer.class, B.class).append(new OpStringToB());
        pipeline.execute(2);
    }

    @Test(expected = PipelineException.class)
    public void nonExecutableTest2() throws PipelineException {
        Pipeline<String, B> pipeline = new PipelineImpl<>(String.class, B.class).append(new OpStringToB());
        pipeline.append(new OpStringToB());
        pipeline.execute("one");
    }

    @Test(expected = PipelineException.class)
    public void nonInverseExecutableTest1() throws PipelineException {
        Pipeline<Integer, B> pipeline = new PipelineImpl<>(Integer.class, B.class).append(new OpStringToB());
        pipeline.executeInverse(new B());
    }

    @Test(expected = PipelineException.class)
    public void nonInverseExecutableTest2() throws PipelineException {
        Pipeline<String, B> pipeline = new PipelineImpl<>(String.class, B.class).append(new OpStringToB());
        pipeline.append(new OpStringToB());
        pipeline.executeInverse(new B());
    }

    @Test(expected = PipelineException.class)
    public void executableButNonIverseExecutableTest() throws PipelineException {
        Pipeline<String, String> pipeline = new PipelineImpl(String.class, String.class).append(new OpStringToB());
        pipeline.append(new OpAToString());
        try {
            pipeline.execute("test");
        } catch (PipelineException e) {
            Assert.fail("not supposed to have exception here!");
        }

        pipeline.executeInverse("inverseexecute");

    }

    @Test(expected = PipelineException.class)
    public void inverseExecutableButNotExecutableTest() throws PipelineException {
        Pipeline<String, String> pipeline = new PipelineImpl<>(String.class, String.class).append(new OpStringToA());
        pipeline.append(new OpBToString());
        try {
            pipeline.executeInverse("test");
        } catch (PipelineException e) {
            Assert.fail("not supposed to have exception here!");
        }

        pipeline.execute("execute");

    }
}
