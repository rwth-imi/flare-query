package de.rwth.imi.flare.requestor;

import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.SerializerException;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

public class ValueSetSerializer implements Serializer<Set> {
    final int testIdSize = 36;

    public ValueSetSerializer(ClassLoader classLoader){

    }


    @Override
    public ByteBuffer serialize(Set stringSet) throws SerializerException {
        //TODO get id size at runtime


        ByteBuffer byteBuffer = ByteBuffer.allocate(testIdSize * stringSet.size());

        int index = 0;
        for(Object idObj : stringSet){
            String id = (String)idObj;
            byteBuffer.put(index, id.getBytes(Charset.defaultCharset()));
            index += testIdSize;
        }
        return byteBuffer;
    }

    @Override
    public Set read(ByteBuffer byteBuffer) throws ClassNotFoundException, SerializerException {
        int byteAmount = byteBuffer.remaining();
        Set<String> readIds = new HashSet<>();
        int idCount = byteAmount  / testIdSize;

        for(int i = 0; i < idCount; i++){
            int startIndex =  i * testIdSize;

            byte[] idBytes = new byte[testIdSize];
            byteBuffer.get(startIndex, idBytes );
            readIds.add(new String(idBytes, Charset.defaultCharset()));
        }
        return readIds;
    }

    @Override
    public boolean equals(Set stringSet, ByteBuffer byteBuffer) throws ClassNotFoundException, SerializerException {
        return false;
    }
}
