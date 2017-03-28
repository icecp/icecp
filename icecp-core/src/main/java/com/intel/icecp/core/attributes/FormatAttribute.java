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
package com.intel.icecp.core.attributes;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.channels.Token;
import java.io.Serializable;

/**
 * Attribute representing a format
 *
 * @param <M> Message type
 */
public class FormatAttribute<M extends Message> extends BaseAttribute<FormatAttribute.FormatInfo> {

    public static final String FORMAT_ATTRIBUTE_ID = "format";

    private final FormatInfo formatInfo;

    /**
     * Collects the necessary format info, i.e., {@link #format} and
     * {@link #type}
     *
     * @param <M1>
     */
    public static class FormatInfo<M1 extends Message> implements Serializable {

        public final Token<M1> type;
        public final String attributeMimeType;

        public FormatInfo(Token<M1> type, String attributeMimeType) {
            this.type = type;
            this.attributeMimeType = attributeMimeType;
        }
    }

    public FormatAttribute(Token<M> type, String attributeMimeType) {
        super(FORMAT_ATTRIBUTE_ID, FormatInfo.class);
        this.formatInfo = new FormatInfo(type, attributeMimeType);
    }

    /**
     * {@inheritDoc }
     *
     */
    @Override
    public FormatInfo value() {
        return formatInfo;
    }

}
