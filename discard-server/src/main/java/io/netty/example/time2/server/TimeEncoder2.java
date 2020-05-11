package io.netty.example.time2.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.example.time2.UnixTime;
import io.netty.handler.codec.MessageToByteEncoder;

public class TimeEncoder2 extends MessageToByteEncoder<UnixTime> {
    @Override
    protected void encode(ChannelHandlerContext ctx, UnixTime msg, ByteBuf out) throws Exception {
        out.writeInt((int) msg.getValue());
    }
}
