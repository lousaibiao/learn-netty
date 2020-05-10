> [文档](https://netty.io/index.html) [使用手册](https://netty.io/wiki/user-guide-for-4.x.html#wiki-h3-14)
>
> 近期公司通过`TCP`连接的的方式接了一个硬件设备，用了最基础的`ServerSocket`类，参考的oracle的[文档](https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html)。实现的比较简单，放在[github](https://github.com/lousaibiao/danxihuanbao-socketserver-java)上，不过这里应该用`Netty`才是正解。所以，过一下`Netty`的入门文档。

# 序言

## 问题

我们一般会用Http客户端库来调用web服务，获取数据。如果一个东西是出于一般性目的设计出来的，那么他在某些方面可能就不是最合适的。比如获取大文件，收发邮件，展示实时的金融数据，游戏数据传输等。为了实现这些需求，需要一个为其高度优化的特定协议。还有一个无法避免的问题是你可能需要调用老系统的数据，但是他的协议又是特定。重点来了，如何在不牺牲`可靠性`和`性能`的前提下`快速`实现这么一个系统。

# 解决方案

用Netty。用Netty。用Netty。重要的事情说3遍。

Netty是一个**异步** **事件驱动** **网络框架** ，可以用来快速开发易维护，高性能，可扩展的服务端/客户端。换句话说他简化了TCP和UDP 等服务的网络开发。

容易开发或者快速开发并不意味着他会牺牲可维护性或者是面临性能问题。Netty吸取了大量用于实现FTP，SMTP，HTTP协议的经验，并且仔细小心谨慎的设计。所以，他在易于开发，追求性能，确保稳定性和灵活性上并没有对任何一点有所妥协。

有人可能会说别的框架他们也这么说自己，那Netty到底或者为什么和他们不一样。答案是他的*设计理念*。Netty提供的API用起来就非常舒服。现在可能不是那么直观，但是当你使用的时候就会体会到。

# 开始使用

这节会围绕Netty的核心构建过程，用几个例子来让你快速上手。学完这节你会可以在Netty框架的基础上学会写client和server。

如果你想学的深入一点，了解一下他的底层实现，[第二节，架构概览](https://netty.io/3.8/guide/#architecture)是个不错的起点。

## 开始之前

这节需要两个东西，新版的Netty和jdk1.6+。[Netty下载地址](https://netty.io/downloads.html)。

```xml
<dependencies>
    <!-- https://mvnrepository.com/artifact/io.netty/netty-all -->
    <dependency>
        <groupId>io.netty</groupId>
        <artifactId>netty-all</artifactId>
        <version>4.1.49.Final</version>
    </dependency>
</dependencies>
```

随着你不断往下看，你能会对这节引入的类有疑惑，你可以随时通过API文档来了解更多。类名都是带链接的，可以直接点过去。



## 编写一个Discard 服务器

### 前半部分

世界上最简单的协议并不是输出Hello world，而是[Discard](https://tools.ietf.org/html/rfc863)，就是过来什么都直接丢弃，并且不给任何回复。下面让我们直接从Netty提供的handler实现来处理IO事件。

```java
package io.netty.example.discard;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class DiscardServerHandler extends ChannelInboundHandlerAdapter {//1
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {//2
//        super.channelRead(ctx, msg);
        ((ByteBuf) msg).release();//3
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {//4
//        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
        ctx.close();
    }
}

```

有以下几点：

1. 我们写一个`DiscardServerHandler`类继承自[ChannelInboundHandlerAdapter](https://netty.io/4.1/api/io/netty/channel/ChannelInboundHandlerAdapter.html)，这个`ChannelInboundHandlerAdapter`继承自抽象类`ChannelHandlerAdapter`并且实现了接口`ChannelInboundHandler`。`ChannelInboundHandler`提供了各种各样的可重写的事件handler方法。这里只要使用`ChannelInboundHandlerAdapter`对`ChannelInboundHandler`的默认实现就好，不需要自己去实现所有的`ChannelInboundHandler`方法。

2. `channelRead`方法我们重写掉了，这个方法会在收到客户端消息的时候调用。例子中，消息`msg`的类型为[ByteBuf](https://netty.io/4.1/api/io/netty/buffer/ByteBuf.html)。`ByteBuf`是对`byte[]`的一种抽象，可以让我们访问数组内容。

3. 我们这里需要实现的是Discard协议，就是丢弃协议，所以需要忽略收到的所有消息。ByteBuf是一种`reference-counted`的对象（可以简单理解指针之类的东西），必须通过显式调用其`release`方法来释放。通常，我们的`channelRead`方法是下面这样的

   ```java
   @Override
   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
       try{
           //对msg做一些处理
       }finally {
           ReferenceCountUtil.release(msg);
       }
   }
   ```

4. Netty在处理IO的遇到exception就会进入`exceptionCaught`方法。通常，需要做一下日志记录，然后把相关的channel（通道）关闭。这里做法也不是固定的，你可以先发一个带code的Response然后再关闭。

### 后半部分

到这一步，我们已经实现了Discard服务的前半部分，剩下的就是写一个`main`方法然后来启动这个`DiscardServerHandler`服务。

```java
package io.netty.example.discard;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class DiscardServer {
    private int port;

    public DiscardServer(int port) {
        this.port = port;
    }

    public void run() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();//1
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();//2
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)//3
                    .childHandler(new ChannelInitializer<SocketChannel>() {//4
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new DiscardServerHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)//5
                    .childOption(ChannelOption.SO_KEEPALIVE, true);//6

            //绑定端口启动服务
            ChannelFuture f = b.bind(port).sync();//7
            //server关闭的时候调用。因为这里是Discard 服务，所以永远不会调用。
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int port = 8080;
        if (args.length > 0)
            port = Integer.parseInt(args[0]);

        new DiscardServer(port).run();
    }
}

```

有以下几点：

1. [NioEventLoopGroup](https://netty.io/4.1/api/io/netty/channel/nio/NioEventLoopGroup.html)是一个多线程的event loop（事件环？）。Netty针对不同的情况提供了多(18)种`EventLoopGroup`的实现，因为这里是一个服务端应用，所以使用`NioEventLoopGroup`。new出来两个对象，通常第一个叫boss，接收进来的连接。第二个，通常叫worker，因为当boss接收了连接之后会把链接注册给worker，让worker来处理后面的通信。每个`EventLoopGroup`使用线程数以及他们如何被映射到[Channel](https://netty.io/4.1/api/io/netty/channel/Channel.html)由`EventLoopGroup`的实现决定，并且可能可以通过构造函数来指定。

   1. 下面是`NioEventGroup`的部分构造函数。

      ![pic](https://images.cnblogs.com/cnblogs_com/sheldon-lou/1761451/o_20050915510445.png)

   2. 什么是Netty的`Channel`。按照[文档](https://netty.io/4.1/api/io/netty/channel/Channel.html)的介绍，可以简单理解为socket的一个抽象，或者是IO的操作，包括IO的读写，连接，绑定等。Channel会给使用者提供以下功能：

      + 当前的状态（连接是否已经打开或者连上）
      + channel的[配置参数](https://netty.io/4.1/api/io/netty/channel/ChannelConfig.html)。（接收的缓冲区大小）
      + socket，io相关的操作（读写等）
      + 处理io事件的[管道](https://netty.io/4.1/api/io/netty/channel/ChannelPipeline.html)
      + ~~详细的以后再说~~

2. [ServerBootstarp](https://netty.io/4.1/api/io/netty/bootstrap/ServerBootstrap.html)是一个配置server的帮助类，你可以使用Channel自己来配置，但是会比较枯燥，所以，大多数情况下直接使用这个`ServerBootstrap`就好。

3. `NioServerSocketChannel`是一个Channel的实例，用来处理进来的连接（上面说的channel的功能）。

4.  [ChannelInitializer](https://netty.io/4.1/api/io/netty/channel/ChannelInitializer.html)是一个特殊的Handler，作用是帮助用户配置Channel。通常的作用是把ChannelHandler放到ChannelPipeline（管道）里面，请求会进入到Pipeline，处理就按照这个Pipeline配置的Handler来。`DiscardServerHandler`就是一种Handler。

5. 用来配置Channel的参数。顺道看一下`ServerBootstrap`的定义，这个`ServerBootstrap`是用来启动`ServerChannel`，`ServerChannel`实际上就是一个`Channel`。我们这里实现的是一个TCP/IP server，所以，可以设置`tcpNoDelay`和`keepAlive`等参数。具体设置看文档。

   ```java
   public class ServerBootstrap extends AbstractBootstrap<ServerBootstrap, ServerChannel>{}
   ```

6. option的为接收连接的配置，也就是给boss用，后面的childOption为worker配置选项。

7. 万事俱备，只欠把绑定端口配置上去然后启动服务。`main`方法里面。

恭喜，搞定。用个tcp 客户端连接试试~~可以看到连接成功，发送了3字节，然后因为是Discard，所以没有返回。

![pic](https://images.cnblogs.com/cnblogs_com/sheldon-lou/1761451/o_20051009331546.png)

## 收到的数据

让我们稍微修改一下代码，以便看看我们收到的数据。按照之前的例子，需要再channelRead方法里面做修改。

```java
@Override
public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    final ByteBuf in = (ByteBuf) msg;
    try {
        System.out.println(in.toString(CharsetUtil.US_ASCII));
    } finally {
        in.release();
    }
}
```

msg可以直接转换成`ByteBuf`对象，然后用ByteBuf的toString方法，设置ASCII参数装成string打印出来。

运行起来然后可以直接在浏览器输入localhost:8080访问，就能看到传过来的数据。

![pic](https://images.cnblogs.com/cnblogs_com/sheldon-lou/1761451/o_20051011151747.png)

## 写一个Echo服务器

我们写一个Echo服务，客户端输入什么，我们就回复什么。

```java
public class EchoServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.write(msg);//1
        ctx.flush();//2
    }
}
```

1. 通过[ChannelHandlerContext](https://netty.io/4.1/api/io/netty/channel/ChannelHandlerContext.html)对象，我们可以触发一些IO事件或者执行一些操作。这里我们不需要手动*release msg*，因为当我们执行了`wirte`方法，Netty会帮我们释放。
2. `ctx.write(Object)`会把内容写到缓冲区，在调用flush后再输出出去。可以用`writeAndFlush`方法代替。

测试一下，发送3个字节，收到3个字节的回复。

![pic](https://images.cnblogs.com/cnblogs_com/sheldon-lou/1761451/o_20051011581148.png)

## 写一个时间服务器

这个例子用来实现一个[Time](https://tools.ietf.org/html/rfc868)协议。通过实现这个协议，我们可以了解Netty如何**构造**和**发送**数据。根据RFC868协议，Time协议有这么几步

1. 服务器监听37端口
2. 客户端连接
3. 服务端返回一个4字节的int时间数据
4. 客户端接收到这个数据
5. 客户端关闭连接
6. 服务端关闭连接。

这里服务端忽略收到的任何客户端数据，而是当客户端一建立连接就返回数据，所以这里不使用`channelRead`方法，而是`channelActive`方法。

```java
package io.netty.example.time;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class TimeServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {//1
        final ByteBuf timeBuf = ctx.alloc().buffer(4);//2
        timeBuf.writeInt((int) (System.currentTimeMillis() / 1000L + 2208988800L));

        final ChannelFuture channelFuture = ctx.writeAndFlush(timeBuf);//3
        channelFuture.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                assert channelFuture == future;
                ctx.close();
            }
        });//4

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}

```

1. 重写的是`channelActive`方法，这个方法会在连接进来的时候调用。

2. 因为要返回一个int值，所以需要4个字节，通过`ChannelHandlerContext`分配，然后writeAndFlush方法写入并发送。

3. 把数据发送给非阻塞IO流的时候不需要调用`java.nio.ByteBuffer.flip()`方法，Netty的`ByteBuf`没有提供这个方法，因为他不需要。`ByteBuf`内部有两个指针，一个用于读，一个用于写。write的时候读指针移动，写指针不动，反之同理。在使用`ByteBuffer`的时候如果没有flip，数据就会乱。

   Netty里面所有的IO操作都是异步的，这样可能会导致write没有开始（或者没有完成）之前就连接就close掉了。比如下面的代码：

   ```java
   Channel ch = ...;
   ch.writeAndFlush(message);
   ch.close();//这也不是立马关闭，也是一个ChannelFuture对象
   ```

   `write(writeAndFlush)`返回的是一个`ChannelFuture`对象，来大致看下这个对象的解释。

   ![pic](https://images.cnblogs.com/cnblogs_com/sheldon-lou/1761451/o_20051014220249.png)

   继承自Future，表示一个Channel的IO操作的结果，不过他还没完成，只是表示已经创建。【详细的以后再讲。】

4. 如何能知道这个IO操作的结果呢？我们可以给这个ChannelFuture增加一个`ChannelFutureListener`的实例（接口），然后实现它的`operationComplete`方法。这里面的方法比较简单，就是close掉这个`ChannelHandlerContext`，所以，可以使用定义好的`ChannelFutureListener.CLOSE`方法。像下面这样

   ```java
   channelFuture.addListener(ChannelFutureListener.CLOSE);
   ```

5. 测试一下，当我们一连接上，就会收到server返回的4个字节的数据，然后关闭连接。

   ![pic](https://images.cnblogs.com/cnblogs_com/sheldon-lou/1761451/o_20051014365250.png)

