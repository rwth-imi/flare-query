package de.rwth.imi.flare.requestor;

import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.SerializerException;

import java.nio.ByteBuffer;

public class ValueSetSerializer implements Serializer<String> {
    @Override
    public ByteBuffer serialize(String s) throws SerializerException {
        return null;
    }

    @Override
    public String read(ByteBuffer byteBuffer) throws ClassNotFoundException, SerializerException {
        return null;
    }

    @Override
    public boolean equals(String s, ByteBuffer byteBuffer) throws ClassNotFoundException, SerializerException {
        return false;
    }
}
