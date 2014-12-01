package com.intel.icecp.core.attributes;

/**
 * Describes the channel namespace for remote implementations of {@link Attributes}. Since attribute reading and writing
 * will occur over channels, the channel namespace must be consistent across the ICECP system. Channels for reading
 * attributes must be constructed using the following pattern: [base uri]/{@link #READ_SUFFIX}/[attribute name].
 * Channels for writing attributes must be constructed using the following pattern: [base uri]/{@link
 * #WRITE_SUFFIX}/[attribute name]. Additionally, values returned by the read channel must be wrapped in the following
 * pseudo-code structure: {@code {d: [the attribute value], ts: [the timestamp of the reading]} } (see {@link
 * AttributeMessage}); this wrapping does not apply to written values.
 *
 */
public class AttributesNamespace {
    public static final String READ_SUFFIX = "$attr-rd";
    public static final String WRITE_SUFFIX = "$attr-wr";

    private AttributesNamespace() {
        // no instances of this class are allowed
    }
}
