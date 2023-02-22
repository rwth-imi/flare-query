package de.rwth.imi.flare.requestor;

import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.SerializerException;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class CacheValueSerializer implements Serializer<CacheValue> {

    public CacheValueSerializer(ClassLoader classLoader) {
    }

    /**
     * A {@code CacheValue} is serialized by writing the following bytes:
     * <ul>
     *   <li>a version byte</li>
     *   <li>for each patient ID:
     *   <ul>
     *     <li>the length of the ID</li>
     *     <li>n bytes of the ID</li>
     *   </ul>
     *   </li>
     * </ul>
     *
     * @param value the cache value to serialize
     * @return the buffer of serialized bytes
     * @throws SerializerException if something goes wrong
     */
    @Override
    public ByteBuffer serialize(CacheValue value) throws SerializerException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(serializedSize(value));
        byteBuffer.put((byte) 0); //version byte

        for (String id : value.patientIds()) {
            byte[] bytes = id.getBytes(US_ASCII);
            if (bytes.length > 255) {
                throw new SerializerException("Can't serialize the patient ID `%s` because it's longer as 255 bytes.".formatted(id));
            }
            byteBuffer.put((byte) bytes.length);
            byteBuffer.put(bytes);
        }

        return byteBuffer.flip();
    }

    private int serializedSize(CacheValue value) {
        return value.patientIds().stream().mapToInt(id -> id.getBytes(US_ASCII).length + 1).sum() + 1;
    }

    @Override
    public CacheValue read(ByteBuffer byteBuffer) throws SerializerException {
        if (!byteBuffer.hasRemaining()) {
            throw new SerializerException("Can't deserialize an empty byte buffer.");
        }
        byte versionByte = byteBuffer.get();
        if (versionByte != 0) {
            throw new SerializerException("Can't deserialize because version %d isn't supported.".formatted(versionByte));
        }
        Set<String> ids = new HashSet<>();
        while (byteBuffer.remaining() > 0) {
            byte[] idBytes = new byte[byteBuffer.get()];
            byteBuffer.get(idBytes);
            ids.add(new String(idBytes, US_ASCII));
        }
        return new CacheValue(ids);
    }

    @Override
    public boolean equals(CacheValue value, ByteBuffer byteBuffer) throws SerializerException {
        return value.equals(read(byteBuffer));
    }
}
