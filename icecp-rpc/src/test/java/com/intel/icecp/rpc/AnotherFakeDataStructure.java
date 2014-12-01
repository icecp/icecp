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
package com.intel.icecp.rpc;

/**
 * For use as an input/output parameter when testing commands
 *
 */
class AnotherFakeDataStructure {

    AnotherFakeDataStructure() {}

    AnotherFakeDataStructure(int a, float b, Object c, int d, double hidden) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.hidden = hidden;
    }

    public int a;
    public float b;
    public Object c;
    public int d;
    private double hidden;
}