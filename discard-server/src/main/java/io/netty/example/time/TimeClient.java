package io.netty.example.time;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class TimeClient {
    public static void main(String[] args) throws InterruptedException {
        int port = 37;
        String host = "192.168.1.181";
        host="192.168.3.4";
        port=8234;
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            final Bootstrap bootstrap = new Bootstrap();//1
            bootstrap.group(workerGroup);//2
            bootstrap.channel(NioSocketChannel.class);//3
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);//4
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new TimeDecoder(), new TimeClientHandler());
                }
            });

            final ChannelFuture connectFuture = bootstrap.connect(host, port).sync();//5

            connectFuture.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
}
