package io.netty.example.time2.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class TimeClient {
    private int port;
    private String ip;

    public TimeClient(int port, String ip) {
        this.port = port;
        this.ip = ip;
    }

    public void run() throws InterruptedException {

        EventLoopGroup workerGroup = new NioEventLoopGroup();
        Bootstrap client = new Bootstrap();
        client.group(workerGroup);
        client.channel(NioSocketChannel.class);
        client.option(ChannelOption.SO_KEEPALIVE, true);

        client.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new TimeDecoder(), new TimeClientHandler());
            }
        });
        ChannelFuture connectFuture = client.connect(ip, port).sync();
        connectFuture.channel().closeFuture().sync();
    }

    public static void main(String[] args) throws InterruptedException {
        new TimeClient(37, "192.168.3.4").run();
    }
}
