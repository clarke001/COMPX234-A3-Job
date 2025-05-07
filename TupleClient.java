import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

//定义一个名为TupleClient的公共类。从而使用socket连接到服务器。
public class TupleClient {
    //声明一个值为10000毫秒的，类型为int的常量TIMEOUT，防止无限等待。
    private static int TIMEOUT = 10000;

    //主程序入口
    public static void main(String[] args) {
        //检查命令行参数数量是否为程序所需要的三个参数：主机名、端口号和文件名。
        if (args.length != 3) {
            System.out.println("Need hostname, port, and file name!");
            return;//参数不正确则退出，不再执行接下来的代码
        }

        //赋值
        String hostname = args[0];
        int port;  //储存端口号
        //尝试将String类型转换到int类型
        try {
            port = Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.out.println("Port number is wrong!");
            return;
        }

        //将第三个命令行参数（args[2]赋值给String变量filename
        String filename = args[2];

        //声明多个变量用于网络通信和文件读取并统一初始化为null
        Socket socket = null;//与服务器建立联系
        PrintWriter out = null;//向服务器发送数据
        BufferedReader in =null;//读取服务器响应
        BufferedReader filReader = null;//读取本地文件

        try {
            //创建新的Socket对象，尝试连接到指定的hostname和port,这会建立与服务器的TCP连接。
            socket = new Socket(hostname, port);
            socket.setSoTimeout(TIMEOUT); //若超时（10秒）则抛出错误。
            //创建变量们的对象以方便下一步的实现
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            filReader = new BufferedReader(new FileReader(filename));

            String line;
            while ((line = filReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                System.out.println("Read line: " + line);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Can't find file: " + filename);
            //System.out.println("Connected to server!");
        } catch (Exception e) {
            System.out.println("Can't connect to server: " + e.getMessage());
        } finally {  //开始执行finally块，无论try块是否抛出异常，此块都会执行，可以清理资源（虽然我觉得有点没有必要）
            //if (socket != null) {
            //在finally块中关闭所有打开的资源
                try {
                if (filReader != null) filReader.close();
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
                    //socket.close();
                } catch (Exception e) {
                    System.out.println("Error closing stuff!");
                }
            }
        }
    }
