package com.bin.protocol.gossip.network.netty;



import com.bin.protocol.gossip.cluster.ClusterService;
import com.bin.protocol.gossip.cluster.operation.Operation;
import com.bin.protocol.gossip.codec.ProtostuffSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class NettyHandler extends ChannelDuplexHandler {

    private final Logger logger = LoggerFactory.getLogger(NettyHandler.class);



    private ClusterService clusterService;


    public NettyHandler(ClusterService clusterService){
        this.clusterService = clusterService;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;

        int total = byteBuf.readIntLE();

        Operation operation = ProtostuffSerializer.readOperation(byteBuf);
        operation.setService(clusterService);
        operation.execute();
    }

}
