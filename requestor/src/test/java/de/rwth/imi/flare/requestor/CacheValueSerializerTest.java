package de.rwth.imi.flare.requestor;

import org.ehcache.spi.serialization.SerializerException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CacheValueSerializerTest {

    public static final String PATIENT_ID = "patient-id-183132";

    private CacheValueSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new CacheValueSerializer(CacheValueSerializerTest.class.getClassLoader());
    }

    @Test
    void serialize_empty() {
        CacheValue value = new CacheValue();

        ByteBuffer byteBuffer = serializer.serialize(value);

        assertEquals(1, byteBuffer.capacity());
        assertEquals(1, byteBuffer.remaining(), "the buffer is ready to be read");
        assertEquals(0, byteBuffer.get(), "version byte");
    }

    @Test
    void serialize_one_patientId() {
        CacheValue value = new CacheValue(Set.of(PATIENT_ID));

        ByteBuffer byteBuffer = serializer.serialize(value);

        assertEquals(2 + PATIENT_ID.length(), byteBuffer.capacity());
        assertEquals(2 + PATIENT_ID.length(), byteBuffer.remaining(), "the buffer is ready to be read");
        assertEquals(0, byteBuffer.get(), "version byte");
        assertEquals(PATIENT_ID.length(), byteBuffer.get(), "patient id length");
        assertEquals('p', byteBuffer.get(), "first patient id byte");
        assertEquals('a', byteBuffer.get(), "second patient id byte");
        assertEquals('t', byteBuffer.get(), "third patient id byte");
    }

    @Test
    void serialize_value_with_extended_unicode_chars() {
        CacheValue value = new CacheValue(Set.of("€\uD83D\uDD25"));

        ByteBuffer byteBuffer = serializer.serialize(value);

        assertEquals(4, byteBuffer.capacity());
        assertEquals(4, byteBuffer.remaining());
        assertEquals(0, byteBuffer.get(), "version byte");
        assertEquals(2, byteBuffer.get(), "patient id length");
        assertEquals('?', byteBuffer.get(), "first patient id byte");
        assertEquals('?', byteBuffer.get(), "second patient id byte");
    }

    @ParameterizedTest
    @MethodSource("provideCacheValues")
    void serialize_roundTrip(CacheValue value) {
        ByteBuffer byteBuffer = serializer.serialize(value);

        assertEquals(value, serializer.read(byteBuffer));
    }

    private static Stream<Arguments> provideCacheValues() {
        return IntStream.range(0, 100)
                .mapToObj(CacheValueSerializerTest::createPatientIdSet)
                .map(patientIds -> Arguments.of(new CacheValue(patientIds)));
    }

    @NotNull
    private static Set<String> createPatientIdSet(int numberOfIds) {
        return IntStream.range(0, numberOfIds).mapToObj("patient-id-%d"::formatted).collect(Collectors.toSet());
    }

    @Test
    void read_empty() {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[]{0});

        CacheValue value = serializer.read(byteBuffer);

        assertEquals(new CacheValue(), value);
    }

    @Test
    void read_unsupportedVersion() {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[]{1});

        SerializerException exception = assertThrows(SerializerException.class, () -> serializer.read(byteBuffer));

        assertEquals("Can't deserialize because version 1 isn't supported.", exception.getMessage());
    }

    @Test
    void read_emptyByteBuffer() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(0);

        SerializerException exception = assertThrows(SerializerException.class, () -> serializer.read(byteBuffer));

        assertEquals("Can't deserialize an empty byte buffer.", exception.getMessage());
    }
}
