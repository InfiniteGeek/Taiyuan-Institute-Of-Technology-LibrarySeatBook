import org.json.JSONArray;
import org.json.JSONObject;
import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.*;

public class Update {

    public static void main(String[] args) throws Exception {

        if(CheckNetwork()) {

            JOptionPane Update_Running_JDialog = new JOptionPane("  （@ • ‿ • @ ）  最新预约程序已就绪！5s后自动重启程序", JOptionPane.PLAIN_MESSAGE);
            JDialog Update_Running_successed_JDialog = Update_Running_JDialog.createDialog(null, "运行状态");
            Update_Running_successed_JDialog.setModal(false); // 设置为非模态
            Update_Running_successed_JDialog.setVisible(true);
            try {
                TimeUnit.SECONDS.sleep(5);
                Update_Running_successed_JDialog.dispose();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            JOptionPane UpdateContent_JDialog = new JOptionPane(getUpdateContent(), JOptionPane.INFORMATION_MESSAGE);
            JDialog UpdateContent_Display_JDialog = UpdateContent_JDialog.createDialog(null, " "+getLatestVersion() + " 更新日志");
            UpdateContent_Display_JDialog.setModal(false); // 设置为非模态
            UpdateContent_Display_JDialog.setVisible(true);
            try {
                TimeUnit.SECONDS.sleep(5);
                UpdateContent_Display_JDialog.dispose();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                // 获取最新版本下载地址
                String downloadUrl = getLatestDownloadUrl();

                // 终止旧进程
                String exePath = System.getProperty("user.dir") + File.separator + "LibrarySeatBook.exe";

                if (isProcessRunning(exePath)) {
                    killProcess(exePath);
                    Thread.sleep(1000); // 等待资源释放
                }

                // 直接下载并保存为目标文件名
                downloadDirectly(downloadUrl, exePath);
                // 重启程序
                startProcess(exePath);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static String getLatestVersion() throws Exception {
        URL url = new URL("https://api.github.com/repos/InfiniteGeek/Taiyuan-Institute-Of-Technology-LibrarySeatBook/releases/latest");

        // 读取 JSON 数据
        Scanner scanner = new Scanner(url.openStream());
        String jsonString = scanner.useDelimiter("\\A").next();
        scanner.close();

        JSONObject release = new JSONObject(jsonString);
        return release.getString("tag_name");
    }
    private static String getLatestDownloadUrl() throws Exception {
        URL apiUrl = new URL("https://api.github.com/repos/InfiniteGeek/Taiyuan-Institute-Of-Technology-LibrarySeatBook/releases/latest");
        HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

        // 读取 JSON 响应
        StringBuilder jsonResponse = new StringBuilder();
        try (Scanner scanner = new Scanner(conn.getInputStream())) {
            while (scanner.hasNextLine()) {
                jsonResponse.append(scanner.nextLine());
            }
        }
        JSONObject release = new JSONObject(jsonResponse.toString());
        String tag = release.getString("tag_name");
        // 构建代理下载地址（根据实际代理服务调整）
        return getBestProxyUrl() + "https://github.com/InfiniteGeek/Taiyuan-Institute-Of-Technology-LibrarySeatBook/releases/download/" + tag + "/LibrarySeatBook_single.exe";

    }
    private static String getUpdateContent() throws Exception {
        URL apiUrl = new URL("https://api.github.com/repos/InfiniteGeek/Taiyuan-Institute-Of-Technology-LibrarySeatBook/releases/latest");
        HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

        // 读取 JSON 响应
        StringBuilder jsonResponse = new StringBuilder();
        try (Scanner scanner = new Scanner(conn.getInputStream())) {
            while (scanner.hasNextLine()) {
                jsonResponse.append(scanner.nextLine());
            }
        }

        JSONObject release = new JSONObject(jsonResponse.toString());
        String body = release.getString("body");
        // 构建代理下载地址（根据实际代理服务调整）
        return body;
        }
    private static boolean isProcessRunning(String exePath) throws IOException {
        Process proc = Runtime.getRuntime().exec(new String[]{
                "cmd.exe", "/c",
                "wmic process where \"ExecutablePath='" + exePath.replace("\\", "\\\\") + "'\" get ProcessId"
        });

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream()))) {
            return reader.readLine() != null;
        }
    }

    // 终止指定路径的进程
    private static void killProcess(String exePath) throws Exception {
        Process proc = Runtime.getRuntime().exec(new String[]{
                "cmd.exe", "/c",
                "wmic process where \"ExecutablePath='" + exePath.replace("\\", "\\\\") + "'\" call terminate"
        });

    }


    // 下载并替换文件
    private static void downloadDirectly(String downloadUrl, String savePath) throws IOException {
        URL url = new URL(downloadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(true); // 自动处理重定向

        // 创建目标文件流
        File targetFile = new File(savePath);
        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(targetFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

    }
    // 启动进程
    private static void startProcess(String exePath) throws IOException {
        new ProcessBuilder(exePath)
                .directory(new File(System.getProperty("user.dir"))) // 保持工作目录一致
                .start();
    }
    private static String getBestProxyUrl() throws Exception {
        HttpURLConnection conn;
        BufferedReader reader;
        // 1. 创建HTTP连接
        URL url = new URL("https://api.akams.cn/github");
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        // 3. 读取响应内容
        reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        // 4. 解析JSON数据
        JSONObject json = new JSONObject(response.toString());
        if (json.getInt("code") != 200) {
            return "";
        }

        // 5. 寻找最低延迟代理
        JSONArray proxies = json.getJSONArray("data");
        return findMinimumLatencyProxy(proxies);

    }


    private static boolean CheckNetwork() throws Exception {
        while (true) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            Runtime.getRuntime().exec("ping www.baidu.com").getInputStream(), "GBK"))) {

                if (reader.lines().anyMatch(line -> line.contains("Ping 请求找不到主机 www.baidu.com。请检查该名称，然后重试。"))) {
                    System.out.println("无网络");
                    Thread.sleep(180_000);
                    return CheckNetwork();
                } else {
                    System.out.println("有网络");
                    return true;
                }
            }
        }
    }


        private static String findMinimumLatencyProxy(JSONArray proxies) throws Exception {
        if (proxies.length() == 0) return "";

            int minLatency = Integer.MAX_VALUE;
            for (int i = 0; i < proxies.length(); i++) {
                JSONObject proxy = proxies.getJSONObject(i);
                int latency = proxy.getInt("latency");

                if (latency < minLatency) {
                    minLatency = latency;
                }
            }
// 第二阶段：在最低延迟代理中找最大速度
            double maxSpeed = -1;
            String optimalUrl = null;
            for (int i = 0; i < proxies.length(); i++) {
                JSONObject proxy = proxies.getJSONObject(i);
                int latency = proxy.getInt("latency");
                double speed = proxy.getDouble("speed");

                if (latency == minLatency && speed > maxSpeed) {
                    maxSpeed = speed;
                    optimalUrl = proxy.getString("url");
                }
            }

            return optimalUrl+"/";
        }}


