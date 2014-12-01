package com.intel.icecp.node;

import com.intel.icecp.common.TestMessage;
import com.intel.icecp.core.Attribute;
import com.intel.icecp.core.attributes.AttributeNotFoundException;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.attributes.BaseAttribute;
import com.intel.icecp.core.attributes.CannotInstantiateAttributeException;
import com.intel.icecp.core.attributes.WriteableBaseAttribute;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.messages.ConfigurationMessage;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.mock.MockChannels;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
public class AttributesFactoryTest {
    private static final String HTTP_WWW_GOOGLE_COM = "http://www.google.com/";
    private Channels channels;
    private URI uri;

    @Before
    public void setUp() {
        this.channels = new MockChannels();
        this.uri = URI.create("icecp:/attributes/base/uri");
    }

    <T, V> Optional<T> exerciseBuildAttributesFromMap(Class<? extends Attribute<T>> attributeClass, String attributeName,
                                                      Class<T> expectedValueClass, V value) {
        Map<String, Object> map = new HashMap<>();
        map.put(attributeName, value);
        Collection<Class<? extends Attribute>> list = new ArrayList<>();
        list.add(attributeClass);
        try {
            final Attributes attributes = AttributesFactory.buildAttributesFromMap(channels, uri, map, list);
            return Optional.of(attributes.get(attributeClass));
        } catch (CannotInstantiateAttributeException | AttributeNotFoundException e) {
            return Optional.empty();
        }
    }

    @Test
    public void buildEmptyAttributes() {
        Attributes attributes = AttributesFactory.buildEmptyAttributes(channels, uri);
        assertEquals(0, attributes.size());
    }

    @Test(expected = CannotInstantiateAttributeException.class)
    public void buildAttributesFromMapFail() throws CannotInstantiateAttributeException {
        Map<String, Object> map = new HashMap<>();
        map.put("a", 42L);
        map.put("b", "...");
        map.put("c", new HashMap<>());

        AttributesFactory.buildAttributesFromMap(channels, uri, map, Arrays.asList(A.class, B.class, C.class));
    }

    @Test
    public void buildAttributesFromMapSuccess() throws CannotInstantiateAttributeException {
        Map<String, Object> map = new HashMap<>();
        map.put("a", 42L);
        map.put("b", "...");

        Attributes attributes = AttributesFactory.buildAttributesFromMap(channels, uri, map,
                Arrays.asList(A.class, B.class));
        assertEquals(2, attributes.size());
    }

    @Test
    public void buildAttributesFromMapZeroAndOneAndZeroOneTwoArgumentConstructors() throws Exception {
        // test ability to instantiate 0, 1, and (0,1,2) argument constructor attributes
        Map<String, Object> map = new HashMap<>();

        final String ZERO = "zero-argument-attribute";
        final String ZERO_URI = "zero:/0";
        map.put(ZERO, new URI(ZERO_URI));

        String ONE = "one-argument-attribute";
        String ONE_URI = "one:/1";
        map.put(ONE, new URI(ONE_URI));

        final String ZERO_ONE_TWO = "zero-and-one-and-two-argument-attribute";
        final String ZERO_ONE_TWO_URI = "zeroonetwo:/012";
        map.put(ZERO_ONE_TWO, new URI(ZERO_ONE_TWO_URI));

        Attributes attributes = AttributesFactory.buildAttributesFromMap(channels, uri, map,
                Arrays.asList(ZeroArgumentAttribute.class, OneArgumentAttribute.class,
                        ZeroAndOneAndTwoArgumentAttribute.class));
        assertEquals(3, attributes.size());
        assertEquals(new URI(ONE_URI), attributes.get(ONE, OneArgumentAttribute.class));
        assertEquals(new URI(ZERO_ONE_TWO_URI), attributes.get(ZERO_ONE_TWO, ZeroAndOneAndTwoArgumentAttribute.class));
    }

    @Test
    public void buildWritableAttributesFromZeroOneTwoArgumentConstructors() throws Exception {
        // test ability to instantiate (0,1,2) argument constructor attributes (writable)
        Map<String, Object> map = new HashMap<>();

        final String ZERO_ONE_TWO = "zero-and-one-and-two-argument-writable-attribute";
        final String ZERO_ONE_TWO_URI = "zeroonetwo:/012";
        map.put(ZERO_ONE_TWO, new URI(ZERO_ONE_TWO_URI));

        Attributes attributes = AttributesFactory.buildAttributesFromMap(channels, uri, map,
                Arrays.asList(ZeroAndOneAndTwoArgumentWritableAttribute.class));
        assertEquals(1, attributes.size());
        assertEquals(new URI(ZERO_ONE_TWO_URI), attributes.get(ZERO_ONE_TWO, ZeroAndOneAndTwoArgumentWritableAttribute.class));
    }

    @Test(expected = CannotInstantiateAttributeException.class)
    public void buildAttributesFromMapTwoArgumentConstructor() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("two-argument-attribute", new URI("two:/2"));

        AttributesFactory.buildAttributesFromMap(channels, uri, map, Arrays.asList(TwoArgumentAttribute.class));
    }

    @Test
    public void buildAttributesFromMapUriToUri() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("string-to-uri-one-argument-attribute", new URI("strigntouri:/0"));

        AttributesFactory.buildAttributesFromMap(channels, uri, map, Arrays.asList(StringToURIOneArgumentAttribute.class));
    }

    @Test
    public void buildAttributesFromMapStringToUri() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("string-to-uri-one-argument-attribute", "strigntouri:/0");

        AttributesFactory.buildAttributesFromMap(channels, uri, map, Arrays.asList(StringToURIOneArgumentAttribute.class));
    }

    @Test
    public void buildAttributesFromMapMultipleOneArgumentConstructors() throws Exception {
        Map<String, Object> map = new HashMap<>();
        final String MULTIPLE_URI = "multiple:/m";
        final String MULTIPLE_ONE_ARGUMENT_ATTRIBUTE = "multiple-one-argument-attribute";
        map.put(MULTIPLE_ONE_ARGUMENT_ATTRIBUTE, new URI(MULTIPLE_URI));

        Attributes attributes = AttributesFactory.buildAttributesFromMap(channels, uri, map,
                Arrays.asList(MultipleOneArgumentAttribute.class));
        assertEquals(1, attributes.size());
        assertEquals(new URI(MULTIPLE_URI), attributes.get(MULTIPLE_ONE_ARGUMENT_ATTRIBUTE,
                MultipleOneArgumentAttribute.class));
    }

    @Test
    public void fromConstructor() {
        long actual = exerciseBuildAttributesFromMap(A.class, "a", Long.class, 42L).get();
        assertEquals(42L, actual);
    }

    @Test
    public void fromConstructorWithIncorrectFindsCompatibleConstructor() {
        assertTrue(exerciseBuildAttributesFromMap(A.class, "a", Long.class, 42L).isPresent());
    }

    @Test
    public void isWritable() {
        assertTrue(AttributesFactory.isWritable(B.class));
        assertFalse(AttributesFactory.isWritable(A.class));
    }

    @Test
    public void fromWriteable() {
        Attribute attribute = AttributesFactory.fromWritable(B.class, "...");
        assertEquals("...", attribute.value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void newAttributeWithValue() {
        AttributesFactory.newAttributeWithValue(C.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void newAttributeWithoutConstructor() {
        AttributesFactory.newAttributeWithValue(D.class, null);
    }

    @Ignore // FIXME -- this test exposes a SonarQube bug; it passes with maven but not SonarQube on the ...
    // build server.  Isolate cause and remove @Ignore annotation.
    @Test
    public void testFromConstructorMapInMap() throws Exception {
        // arrange
        final LinkedHashMap input_value = (LinkedHashMap) decodeConfigFile("{\n" +
                        "  \"map-in-map\": {\n" +
                        "      \"a\": \"string\",\n" +
                        "      \"b\": 3.3,\n" +
                        "      \"c\": 4,\n" +
                        "      \"d\": false\n" +
                        "  }\n" +
                "}").get("map-in-map");

        // act
        final TestMessage actual = exerciseBuildAttributesFromMap(TestMessageAttribute.class,
                "test-message-attribute", TestMessage.class, input_value).get();

        // assert
        assertTrue(actual.getClass().isAssignableFrom((Class) TestMessage.class));
        // TODO: check actual's value, not just its class
    }

    @Test
    public void testConfigStringToUri() throws Exception {
        // arrange
        final String input_value = (String) decodeConfigFile("{\"uri\": \"" + HTTP_WWW_GOOGLE_COM + "\"}").get("uri");

        // act
        final URI actual = exerciseBuildAttributesFromMap(TestURIAttribute.class,
                "test-uri-attribute", URI.class, input_value).get();

        // assert
        assertTrue(actual.getClass().isAssignableFrom((Class) URI.class));
        assertEquals(new URI(HTTP_WWW_GOOGLE_COM), actual);
    }

    @Test
    public void testConfigStringToUriNotWritable() throws Exception {
        // arrange
        final String input_value = (String) decodeConfigFile("{\"uri\": \"" + HTTP_WWW_GOOGLE_COM + "\"}").get("uri");

        // act
        final URI actual = exerciseBuildAttributesFromMap(TestURIAttributeReadOnly.class,
                "test-uri-attribute-read-only", URI.class, input_value).get();

        // assert
        assertEquals(new URI(HTTP_WWW_GOOGLE_COM), actual);
    }

    @Test
    public void testConfigComplicatedMap() throws Exception {
        // arrange
        final LinkedHashMap input_value = (LinkedHashMap) decodeConfigFile("{\"x\":    {\n" +
                        "      \"id\": \"time.range.trigger\",\n" +
                        "      \"publishChannel\": \"/DEX-SCHEDULER\",\n" +
                        "      \"payload\": {\n" +
                        "        \"Test\": \"element\",\n" +
                        "        \"Test3\": 23.456,\n" +
                        "        \"Test2\": 1111\n" +
                        "      }\n" +
                "    }}").get("x");

        // act
        final ComplicatedValueType actual =
                exerciseBuildAttributesFromMap(
                        ComplicatedValueAttribute.class, "complicated-value-attribute", ComplicatedValueType.class,
                        input_value).get();

        // assert
        assertTrue(actual.getClass().isAssignableFrom((Class) ComplicatedValueType.class));
        // TODO: check actual value, not just class
    }


    private ConfigurationMessage decodeConfigFile(final String configFileContents) throws com.intel.icecp.core.metadata.formats.FormatEncodingException {
        final JsonFormat<ConfigurationMessage> formatter = new JsonFormat<>(ConfigurationMessage.class);
        return formatter.decode(new ByteArrayInputStream((
                configFileContents).getBytes()));
    }

    /**
     * This attribute will not work because of how inner class constructors work: these constructors will have the first
     * parameter equal to the enclosing class, e.g. {@code C(AttributesFactoryTest enclosing, ...)}
     */
    class C extends WriteableBaseAttribute<Map<String, String>> {
        public C() {
            super("c", Map.class);
        }
    }
}

/**
 * Note: this class is outside the test so that it's constructor is identifiable
 */
class A extends BaseAttribute<Long> {
    private final long value;

    public A() {
        super("a", long.class);
        this.value = -1;
    }

    public A(long v) {
        super("a", long.class);
        this.value = v;
    }

    @Override
    public Long value() {
        return value;
    }
}

/**
 * Note: unlike A, this class could be an inner class because it uses {@link com.intel.icecp.core.attributes.WriteableAttribute#value(Object)}.
 */
class B extends WriteableBaseAttribute<String> {
    public B() {
        super("b", String.class);
    }
}

class TestMessageAttribute extends WriteableBaseAttribute<TestMessage> {
    public TestMessageAttribute(TestMessage x) {
        super("test-message-attribute", TestMessage.class);
        value(x);
    }

    public TestMessageAttribute(LinkedHashMap m) {
        super("test-message-attribute", TestMessage.class);
        value(TestMessage.build((String) m.get("a"), ((double) m.get("b")), (int)m.get("c"), (boolean)m.get("d")));
    }
}

class ZeroArgumentAttribute extends WriteableBaseAttribute<URI> {
    public ZeroArgumentAttribute() {
        super("zero-argument-attribute", URI.class);
    }
}

class OneArgumentAttribute extends BaseAttribute<URI> {
    private final URI uri;

    public OneArgumentAttribute(URI uri) {
        super("one-argument-attribute", URI.class);
        this.uri = uri;
    }

    @Override
    public URI value() {
        return uri;
    }
}

class MultipleOneArgumentAttribute extends BaseAttribute<URI> {
    private final URI uri;

    public MultipleOneArgumentAttribute(URI uri) {
        super("multiple-one-argument-attribute", URI.class);
        this.uri = uri;
    }

    public MultipleOneArgumentAttribute(Long foo) {
        super("multiple-one-argument-attribute", URI.class);
        this.uri = null;
    }

    @Override
    public URI value() {
        return uri;
    }
}

class StringToURIOneArgumentAttribute extends WriteableBaseAttribute<URI> {
    public StringToURIOneArgumentAttribute(String foo) throws URISyntaxException {
        super("string-to-uri-one-argument-attribute", URI.class);

        if (foo == null) {
            value(null);
            return;
        }
        try {
            value(new URI(foo));
        } catch (URISyntaxException e) {
            value(new URI("http://www.google.com/"));
        }
    }

    public StringToURIOneArgumentAttribute(URI foo) {
        super("string-to-uri-one-argument-attribute", URI.class);
        value(foo);
    }
}

class TwoArgumentAttribute extends BaseAttribute<URI> {
    private final URI uri;

    public TwoArgumentAttribute(URI uri, String irrelevant) {
        super("two-argument-attribute" + irrelevant, URI.class);
        this.uri = uri;
    }

    @Override
    public URI value() {
        return uri;
    }
}

class ZeroAndOneAndTwoArgumentAttribute extends BaseAttribute<URI> {
    private final URI uri;

    public ZeroAndOneAndTwoArgumentAttribute() {
        super("zero-and-one-and-two-argument-attribute", URI.class);
        this.uri = null;
    }

    public ZeroAndOneAndTwoArgumentAttribute(URI uri) {
        super("zero-and-one-and-two-argument-attribute", URI.class);
        this.uri = uri;
    }

    public ZeroAndOneAndTwoArgumentAttribute(URI uri, String irrelevant) {
        super("zero-and-one-and-two-argument-attribute" + irrelevant, URI.class);
        this.uri = uri;
    }

    @Override
    public URI value() {
        return uri;
    }
}

class ZeroAndOneAndTwoArgumentWritableAttribute extends WriteableBaseAttribute<URI> {
    public ZeroAndOneAndTwoArgumentWritableAttribute() {
        super("zero-and-one-and-two-argument-writable-attribute", URI.class);
        value(null);
    }

    public ZeroAndOneAndTwoArgumentWritableAttribute(URI uri) {
        super("zero-and-one-and-two-argument-writable-attribute", URI.class);
        value(uri);
    }

    public ZeroAndOneAndTwoArgumentWritableAttribute(URI uri, String irrelevant) {
        super("zero-and-one-and-two-argument-writable-attribute" + irrelevant, URI.class);
        value(uri);
    }
}

/**
 * This attribute will not work because it is neither writeable nor has a constructor that can be set with a value.
 */
class D implements Attribute<Integer> {
    @Override
    public String name() {
        return "d";
    }

    @Override
    public Class<Integer> type() {
        return Integer.class;
    }

    @Override
    public Integer value() {
        return 42;
    }
}

class TestURIAttribute extends WriteableBaseAttribute<URI> {
    private final URI DEFAULT_URI = new URI("http://example.com/");

    public TestURIAttribute(String input) throws URISyntaxException {
        super("test-uri-attribute", URI.class);
        if (input == null) {
            value(null);
            return;
        }
        try {
            value(new URI(input));
        } catch (URISyntaxException e) {
            value(DEFAULT_URI);
        }
    }
}

class TestURIAttributeReadOnly extends BaseAttribute<URI> {
    private final URI DEFAULT_URI = new URI("http://example.com/");
    private final URI value;

    public TestURIAttributeReadOnly(String input) throws URISyntaxException {
        super("test-uri-attribute-read-only", URI.class);
        URI tmpValue;

        if (input == null) {
            tmpValue = null;
        } else {
            try {
                tmpValue = new URI(input);
            } catch (URISyntaxException e) {
                tmpValue = DEFAULT_URI;
            }
        }
        value = tmpValue;
    }

    @Override
    public URI value() {
        return value;
    }
}


class ComplicatedValueAttribute extends WriteableBaseAttribute<ComplicatedValueType> {
    public ComplicatedValueAttribute(Map<String, Object> input) {
        super("complicated-value-attribute", ComplicatedValueType.class);
        if (input == null) {
            value(null);
        } else {
            value(new ComplicatedValueType((String) input.get("id"),
                    (String) input.get("publishChannel"),
                    (Map<String, Object>) input.get("payload")));
        }
    }
}

class ComplicatedValueType {
    private final String id;
    private final Map<String, Object> payload;
    private String publishChannel;

    public ComplicatedValueType(String id,
                                String publishChannel,
                                Map<String, Object> payload) {
        this.id = id;
        this.payload = payload;
        this.publishChannel = publishChannel;
    }
}

