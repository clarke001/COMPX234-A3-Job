import java.net.*;

//定义了一个名为 TupleSpaceServer的公共类，其为程序的主类。
public class MyTupleServer {
    //对服务器允许的最小和最大端口号常量进行设定。
    private static final int MIN_PORT = 50000;
    private static final int MAX_PORT = 59999;

    //程序的主方法
    public static void main(String[] args) {

        //检查命令行是否提供了一个合适的参数（端口号），若不是（即参数不是1）则打印错误信息。
        if (args.length != 1) {
            System.out.println("Please give a port number!");
            return;
        }

        int port;  //试将第一个命令行参数转换为整数以作为端口号
        try {
            //使用try...catch是因为parseInt会抛出错误。
            port = Integer.parseInt(args[0]);
            //验证端口的范围是否合规
            if (port < MIN_PORT || port > MAX_PORT) {
                System.out.println("Port must be between 50000 and 59999!");
                return;
            }
        } catch (Exception e) {
            System.out.println("Invalid port number!");
            return;
        }
        //声明一个初始值为NULL的ServerSocket变量
        ServerSocket serverSocket = null;
        //尝试在指定的窗口创建并连接serverSocket
        //serverSocket可能会抛出异常所以使用try...catch进行捕获
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);
        } catch (Exception e) {
            System.out.println("Server error: " + e.getMessage());
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (Exception e) {
                    System.out.println("Error closing server!");
                }
            }
        }
}
}