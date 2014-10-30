import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NioServer
{
  public static void main (String[] args)
  {
    try
    {
      Selector selector = Selector.open();
      ServerSocketChannel server = ServerSocketChannel.open();
      server.configureBlocking(false);
      server.socket().bind(new InetSocketAddress("localhost", 5555));
      server.register(selector, SelectionKey.OP_ACCEPT);

      ByteBuffer buffer = ByteBuffer.allocate(8192);

      int i = 0;

      while (true)
      {
        System.out.println("Select");
        selector.select(20000); //blocks
        Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
        while (keys.hasNext())
        {
          ++i;
          System.out.printf("Loop %d\n", i);
          SelectionKey key = keys.next();
          keys.remove();

          if (key.isValid())
          {
            if (key.isAcceptable())
            {
              System.out.println("Accept");
              ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
              SocketChannel socketChannel = serverChannel.accept();
              if (socketChannel != null)
              {
                socketChannel.configureBlocking(false);
                socketChannel.register(selector, SelectionKey.OP_READ);
              }
            }
            else if (key.isReadable())
            {
              buffer.clear();
              SocketChannel socketChannel = (SocketChannel) key.channel();
              int n = socketChannel.read(buffer);
              System.out.printf("Read %d bytes\n", n);
              if (n < 0)
              {
                socketChannel.close();
                key.cancel();
              }
              else
              {
                key.interestOps(SelectionKey.OP_WRITE);
              }
            }
            else if (key.isWritable())
            {
                SocketChannel socketChannel = (SocketChannel) key.channel();
                ByteBuffer buf = ByteBuffer.wrap(buffer.array());
                while (buf.remaining() > 0)
                {
                   socketChannel.write(buf);
                }
                key.interestOps(SelectionKey.OP_READ);
            }
          }
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
