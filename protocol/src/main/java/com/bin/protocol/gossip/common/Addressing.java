package com.bin.protocol.gossip.common;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Addressing {

    //   xxx/IP:port
    private static final Pattern HOST_FORMAT = Pattern.compile("(\\S*)\\/{1}(\\S+)");

    public static InetAddress getLocalIpAddress() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

/*
    public static Address fromRemoteAddress(Channel channel) {
        return fromSocketAddress(channel.remoteAddress());
    }
*/

    public static Address resoleHostAddress(SocketAddress socketAddress) {
        return resoleHostAddress(socketAddress.toString());
    }

    public static Address resoleHostAddress(String socketAddress) {
        return Address.create(resoleName(socketAddress), resoleAddress(socketAddress).port());
    }

    public static String resoleName(SocketAddress socketAddress) {
        return resoleName(socketAddress.toString());
    }

    /**
     *
     * @return hostName/host
     */
    public static String resoleName(String socketAddress) {
        String name = hostMatcher(socketAddress).group(1);
        if ("".equals(name)) {
            name = resoleAddress(socketAddress).host();
        }
        return name;
    }

    public static Address resoleAddress(SocketAddress socketAddress) {
        return resoleAddress(socketAddress.toString());
    }

    public static Address resoleAddress(String socketAddress) {
        // 127.0.0.1:8090
        return Address.from(hostMatcher(socketAddress).group(2));
    }

    private static Matcher hostMatcher(String socketAddress) {
        Matcher m = HOST_FORMAT.matcher(socketAddress);
        if (!m.matches()) {
            throw new IllegalArgumentException(String.format("unsupported address [%s]!", new Object[] {socketAddress}));
        }
        return m;
    }



  /*  public static Address fromLocalAddress(Channel channel) {
        return fromSocketAddress(channel.localAddress());
    }
*/
    /**
     *
     * @param socketAddress  localhost/127.0.0.1:8090
     */
    public static Address fromSocketAddress(SocketAddress socketAddress) {
        return Address.from(socketAddress.toString());
    }


    public static void main(String[] args) throws IOException {
        Socket socket1 =  new Socket("www.baidu.com", 80);
        SocketAddress socketAddress = socket1.getRemoteSocketAddress();
        socket1.close();
        Socket socket2 =  new Socket();
        socket2.connect(socketAddress);
        socket2.close();
        InetSocketAddress inetSocketAddress1 = (InetSocketAddress) socketAddress;
        System.out.println("服务器域名:"
                + inetSocketAddress1.getAddress().getHostName());
        System.out.println("服务器IP:"
                + inetSocketAddress1.getAddress().getHostAddress());
        System.out.println("服务器端口:" + inetSocketAddress1.getPort());
        System.out.println(inetSocketAddress1.getHostName());
        InetSocketAddress inetSocketAddress2 = (InetSocketAddress) socket2
                .getLocalSocketAddress();
        System.out.println("本地IP:"
                + InetAddress.getLocalHost()
                .getHostAddress());
        System.out.println("本地端口:" + inetSocketAddress2.getPort());
    }
}
