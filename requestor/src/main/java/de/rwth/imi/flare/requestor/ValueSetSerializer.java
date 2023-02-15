package de.rwth.imi.flare.requestor;

import org.apache.commons.lang3.ArrayUtils;
import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.SerializerException;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class ValueSetSerializer implements Serializer<Set> {
    final int testIdSize = 36;

    public ValueSetSerializer(ClassLoader classLoader){

    }


    @Override
    public ByteBuffer serialize(Set stringSet) throws SerializerException {
        //TODO get id size at runtime


        //ByteBuffer byteBuffer = ByteBuffer.allocate(testIdSize * stringSet.size());
        LinkedList<Byte> byteList = new LinkedList<Byte>();

        //int index = 0;
        for(Object idObj : stringSet){
            String id = (String)idObj;
            byte idLength = (byte)id.length();//TODO maybe handle overflow here

            byteList.add(idLength);
            //byteList.add(id.getBytes(Charset.defaultCharset()));
            byte[] currentIdBytes = id.getBytes(Charset.defaultCharset());
            for (byte currentIdByte : currentIdBytes) {
                byteList.add(currentIdByte);
            }
           // byteList.add(index, idLength);
           // index += 1;

            //byteBuffer.put(index, id.getBytes(Charset.defaultCharset()));
           // index += testIdSize;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(byteList.size());

        byte[] idByteArray = ArrayUtils.toPrimitive(byteList.toArray(new Byte[0]));
        byteBuffer.put(0, idByteArray);
        return byteBuffer;
    }

    @Override
    public Set read(ByteBuffer byteBuffer) throws ClassNotFoundException, SerializerException {
        /*int byteAmount = byteBuffer.remaining();
        Set<String> readIds = new HashSet<>();
        int idCount = byteAmount  / testIdSize;

        for(int i = 0; i < idCount; i++){
            int startIndex =  i * testIdSize;

            byte[] idBytes = new byte[testIdSize];
            byteBuffer.get(startIndex, idBytes );
            readIds.add(new String(idBytes, Charset.defaultCharset()));
        }
        return readIds;*/
        int byteAmount = byteBuffer.remaining();
        Set<String> readIds = new HashSet<>();

        int indexCounter = 0;
        while(indexCounter < byteAmount){
            byte idLength = byteBuffer.get(indexCounter);
            indexCounter += 1;

            byte[] idBytes = new byte[idLength];
            byteBuffer.get(indexCounter, idBytes );
            indexCounter += idLength;

            readIds.add(new String(idBytes, Charset.defaultCharset()));
        }
        return readIds;
    }

    @Override
    public boolean equals(Set stringSet, ByteBuffer byteBuffer) throws ClassNotFoundException, SerializerException {
        return false;
    }
}
