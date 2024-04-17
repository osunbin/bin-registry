package com.bin.protocol.gossip.codec;

import com.bin.protocol.gossip.Member;
import com.bin.protocol.gossip.Message;
import com.bin.protocol.gossip.cluster.gossip.Gossip;
import com.bin.protocol.gossip.cluster.membership.MembershipRecord;
import com.bin.protocol.gossip.cluster.operation.Operation;
import com.bin.protocol.gossip.common.Address;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.protostuff.ByteBufInput;
import io.protostuff.ByteBufOutput;
import io.protostuff.Input;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.bin.protocol.gossip.cluster.failurefetector.MemberStatus.LEAVING;

public class ProtostuffSerializer {


    private final ConcurrentHashMap<Class<?>, Schema<?>> schemaMap//
            = new ConcurrentHashMap<>(16);


   private static final Schema<Operation> operationSchema
            = RuntimeSchema.getSchema(Operation.class);

    public static Operation readOperation(ByteBuf byteBuf) throws IOException {
        Input input = new ByteBufInput(byteBuf, true);
        Operation operation = operationSchema.newMessage();
        operationSchema.mergeFrom(input,operation);
        return operation;
    }


    public static ByteBuf writeOperation(int initSize,Operation op) throws IOException {
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(initSize);
        buffer.writeIntLE(1);
        ByteBufOutput output = new ByteBufOutput(buffer);
        operationSchema.writeTo(output,op);
        buffer.writeIntLE(buffer.readableBytes() - 4);
        return buffer;
    }



    private <T> Schema<T> schema(Class<T> clazz) {
        Schema<T> schema = (Schema<T>) schemaMap.get(clazz);

        if (schema != null) {
            return schema;
        }

        schema = (Schema<T>) schemaMap.computeIfAbsent(clazz,
                k -> RuntimeSchema.getSchema(clazz));
        return schema;
    }

}
