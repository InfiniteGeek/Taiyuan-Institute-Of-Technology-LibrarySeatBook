import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class library {

    private static final String Version = "v1.6.6";
    private static final String Update_Path = System.getProperty("user.dir") + File.separator + "Update.exe";
    private static final String CONFIG_FILE = "config.txt";
    private static final String MC_INI_FILE = "mc.ini";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");


    public static void main(String[] args) throws Exception {

        if (!validateHardware()) {
            SoftDog();
        } else {
            if (isConfigIncomplete()) {
                CreateConfig();//加载或创建配置文件
            } else if (ConfigComplete()) {
                JOptionPane.showMessageDialog(null, "配置文件内容不完整，请补充！", "配置缺失", JOptionPane.ERROR_MESSAGE);
                CreateConfig();
            } else if (getConfig("预约时间") != null && isTimeComplete(getConfig("预约时间"))) {
                LocalDateTime now = LocalDateTime.of(LocalDate.now(), LocalTime.now());
                LocalTime scheduledTime1 = LocalTime.parse(getConfig("预约时间"), TIME);
                LocalDateTime scheduledTime = LocalDateTime.of(LocalDate.now(), scheduledTime1);

                JOptionPane Running_JDialog = new JOptionPane("  （@ • ‿ • @ ）  预约程序已正常启动！", JOptionPane.PLAIN_MESSAGE);
                JDialog Running_successed_JDialog = Running_JDialog.createDialog(null, "运行状态");
                Running_successed_JDialog.setModal(false); // 设置为非模态
                Running_successed_JDialog.setVisible(true);

                try {
                    TimeUnit.SECONDS.sleep(5);
                    Running_successed_JDialog.dispose();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (CheckNetwork()) {
                    if((!LatestVersion().equals(Version)) && now.isBefore(scheduledTime )){startProcess(Update_Path);}
                }

                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                // 定义任务
                Runnable task = () -> {
                    // System.out.println("Task executed at: " + LocalTime.now());
                    try {
                        if (TestSystemDelay()) {
                            createTask().run();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane Block_JDialog = new JOptionPane("网络连接中断或预约请求响应超时，小程序瘫痪中ing······", JOptionPane.WARNING_MESSAGE);
                        JDialog Running_Block_JDialog = Block_JDialog.createDialog(null, "响应超时");
                        Running_Block_JDialog.setModal(false); // 设置为非模态
                        Running_Block_JDialog.setVisible(true);
                    }
                };


                long delay = Duration.between(now, scheduledTime).toMillis();//between(开始,结束)

                if (delay <= 0) {
                    createTask().run();
                    delay += Duration.ofDays(1).toMillis();
                }
                //System.out.println(delay/1000/60);
                //安排任务每天特定时间执行一次
                scheduler.scheduleAtFixedRate(task, delay, Duration.ofDays(1).toMillis(), TimeUnit.MILLISECONDS);
            } else {
                JOptionPane.showMessageDialog(null, "配置文件内容不完整，请补充！", "配置缺失", JOptionPane.ERROR_MESSAGE);
                CreateConfig();
            }
        }


    }

    //检测mc.ini文件是否存在
    private static boolean validateHardware() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(MC_INI_FILE));
            String storedUUID = reader.readLine();
            reader.close();

            String currentUUID = getHardwareUUID();
            return currentUUID != null && currentUUID.equals(storedUUID);
        } catch (Exception e) {
            return false; // 文件不存在或读取失败
        }
    }

    //保存mc.ini文件
    private static boolean validateCard() {
        String currentUUID = getHardwareUUID();
        if (!currentUUID.equals(getHardwareUUID())) {
            return false;
        }

        // 将硬件信息存储到 mc.ini 文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(MC_INI_FILE))) {
            writer.write(currentUUID);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    //获取主板uuid
    private static String getHardwareUUID() {
        String uuid = "";
        try {
            // 使用 WMIC 命令获取主板 UUID
            Process process = Runtime.getRuntime().exec("wmic csproduct get uuid");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() > 0 && !line.contains("UUID")) {
                    uuid = line.trim();
                    break;
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uuid;
    }

    //获取动态密钥
    private static final String API_URL = "https://api.bilibili.com/x/relation/stat?vmid=1302624960";

    private static boolean CheckNetwork() throws Exception {
        while (true) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            Runtime.getRuntime().exec("ping www.baidu.com").getInputStream(), "GBK"))) {

                if (reader.lines().anyMatch(line -> line.contains("Ping 请求找不到主机 www.baidu.com。请检查该名称，然后重试。"))) {
                    //System.out.println("无网络");
                    Thread.sleep(180_000);
                    return CheckNetwork();
                } else {
                    //System.out.println("有网络");
                    return true;
                }
            }
        }
    }
    private static String getFollowerCount() {

        try {
            // 发送HTTP请求获取JSON数据
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");

            // 读取响应
            StringBuilder response = new StringBuilder();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
            }

            // 解析JSON数据
            JSONObject jsonObject = new JSONObject(response.toString());
            if (jsonObject.has("data") && jsonObject.getJSONObject("data").has("follower")) {
                return jsonObject.getJSONObject("data").getString("follower");
            } else {
                return "1302624960";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "1302624960";
    }
    private static String LatestVersion() throws Exception {
        URL url = new URL("https://api.github.com/repos/InfiniteGeek/Taiyuan-Institute-Of-Technology-LibrarySeatBook/releases/latest");

        // 读取 JSON 数据
        Scanner scanner = new Scanner(url.openStream());
        String jsonString = scanner.useDelimiter("\\A").next();
        scanner.close();

        JSONObject release = new JSONObject(jsonString);
        return release.getString("tag_name");
    }


    private static void startProcess(String exePath) throws IOException {
        new ProcessBuilder(exePath)
                .directory(new File(System.getProperty("user.dir"))) // 保持工作目录一致
                .start();
    }

    private static Runnable createTask() {
        LocalDate today = LocalDate.now();
        String cardNumber = getConfig("学号");
        String labRoomId = getConfig("自习室");
        String seatNum = getConfig("座位号");
        String labRoomId1 = getConfig("备选自习室");
        String seatNum1 = getConfig("备选座位号");
        String roomName = getRoomName(labRoomId);
        String roomName1 = getRoomName(labRoomId1);
        String Day = getConfig("预约天限");


        return () -> {
            LocalDate tomorrowafter = today.plusDays(Long.parseLong(Day));
            String tomorrowafterDateStr = tomorrowafter.format(formatter);
            String jsonData = loadJsonData(tomorrowafterDateStr);
            String jsonData1 = loadJsonData1(tomorrowafterDateStr);

            //System.out.println(jsonData);
            //第一次预约，首选预约
            sendRequest(jsonData);
            //当首选座位被约，立即预约备选座位预约
            if (responseBody.contains("座位已经 在当前时间段已经被预约")) {
                //第一次预约失败
                JOptionPane Appointment_failed = new JOptionPane(cardNumber + "  预约失败！ " + "【首选座位】: " + roomName + "的" + seatNum + "号座位 已经被约！ 尝试预约【备选座位】······", JOptionPane.INFORMATION_MESSAGE);
                JDialog Appointment_failed_JDialog = Appointment_failed.createDialog(null, "预约失败");
                Appointment_failed_JDialog.setModal(false); // 设置为非模态
                Appointment_failed_JDialog.setVisible(true);

                try {
                    TimeUnit.SECONDS.sleep(5);
                    Appointment_failed_JDialog.dispose(); // 关闭第一次预约失败的对话框
                    TimeUnit.SECONDS.sleep(5);
                    sendRequest(jsonData1);//第二次预约

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (responseBody.contains("座位已经 在当前时间段已经被预约")) {
                    //第二次预约失败
                    JOptionPane Failed_JDialog = new JOptionPane(cardNumber + "  预约失败！ " + "【备选座位】: " + roomName1 + "的" + seatNum1 + "号座位 已经被约", JOptionPane.WARNING_MESSAGE);
                    JDialog Book_Failed_JDialog = Failed_JDialog.createDialog(null, "预约失败");
                    Book_Failed_JDialog.setModal(false); // 设置为非模态
                    Book_Failed_JDialog.setVisible(true);
                } else if (responseBody.contains("4.1 接受预约  jwt 版本 校验通过后 进行预约成功")) {
                    //第二次预约成功
                    JOptionPane Appointment_successful1 = new JOptionPane(cardNumber + "  预约成功！ " + " 预约【备选座位】: " + roomName1 + "的" + seatNum1 + "号座位成功", JOptionPane.INFORMATION_MESSAGE);
                    JDialog Appointment_successful1_JDialog = Appointment_successful1.createDialog(null, "预约成功");
                    Appointment_successful1_JDialog.setModal(false);
                    Appointment_successful1_JDialog.setVisible(true);
                    try {
                        TimeUnit.MINUTES.sleep(15);
                        Appointment_successful1_JDialog.dispose();//关闭第二次预约成功的对话框
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            // 检查并显示消息
            else if (responseBody.contains("4.1 接受预约  jwt 版本 校验通过后 进行预约成功")) {
                //第一次预约成功
                JOptionPane Appointment_successful = new JOptionPane(cardNumber + "  预约成功！ " + " 预约【首选座位】: " + roomName + "的" + seatNum + "号座位成功", JOptionPane.INFORMATION_MESSAGE);
                JDialog Appointment_successful_JDialog = Appointment_successful.createDialog(null, "预约成功");
                Appointment_successful_JDialog.setModal(false);
                Appointment_successful_JDialog.setVisible(true);
                try {
                    TimeUnit.MINUTES.sleep(15);
                    Appointment_successful_JDialog.dispose();//关闭第一次预约成功的对话框
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (responseBody.contains("当前用户在 所选择时间周期内 已预约")) {
                try {
                    QueryRequest();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (responseBody.contains("4.1 接受预约  jwt 版本 校验通过后 进行预约参数缺失 或者日期非今日明日后日")) {
                JOptionPane.showMessageDialog(null, "(〒︿〒)  不在规定预约时间内", "预约出错", JOptionPane.WARNING_MESSAGE);
            } else if (responseBody.contains("当前人员 的 请求已经在处理中了请稍后重试")) {
                JOptionPane.showMessageDialog(null, "(〒︿〒)  服务器拥堵，预约请求阻塞", "预约出错", JOptionPane.WARNING_MESSAGE);
            } else if (responseBody.contains("token 校验失败! 请刷新小程序重试!")) {
                JOptionPane.showMessageDialog(null, "(〒︿〒)  令牌更新，预约请求失效", "预约失效", JOptionPane.WARNING_MESSAGE);
            }


        };
    }


    //测试预约系统的开放时间
    private static boolean TestSystemDelay() throws IOException {

        String urlString = "https://mipservice.tit.edu.cn/consumeServer/ReadingRoomWx/getReadyForOneSchLabApply";

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Origin", "https://mipweb.tit.edu.cn");
        connection.setRequestProperty("Referer", "https://mipweb.tit.edu.cn");
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
        connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
        connection.setDoOutput(true);

        LocalDate today = LocalDate.now();
        String cardNumber = getConfig("学号");
        String labRoomId = getConfig("自习室");
        String Day = getConfig("预约天限");

        LocalDate tomorrowafter = today.plusDays(Long.parseLong(Day));
        String tomorrowafterDateStr = tomorrowafter.format(formatter);
        String jsonInputString = String.format("{\"cardNumber\": \"%s\", \"applyDate\": \"%s\", \"applyStartTime\": \"06:30\", \"applyDuration\": \"15.5\", \"labRoomId\": \"%s\"}",
                cardNumber, tomorrowafterDateStr, labRoomId);

        // 构建请求体
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 读取响应
        //int responseCode = connection.getResponseCode();
        //boolean continueLoop = true;

        try (Scanner scanner = new Scanner(connection.getInputStream(), "UTF-8").useDelimiter("\\A")) {
            if (scanner.hasNext()) {
                String response = scanner.next();
                System.out.println("TestSystemDelay>>>" + response);
                if (response.contains("当前用户在 所选择时间周期内 已预约") || response.contains(" 3. 预约准备 查询某一个阅览室的 日期 时间的 座位空闲情况成功")) {
                    return true;
                } else {
                    return TestSystemDelay();
                }

            }
        }
        return false;
    }


    private static String responseBody;

    // 发送HTTP请求-预约座位的方法
    private static String sendRequest(String jsonData) {
        String Book_URL = "https://mipservice.tit.edu.cn/consumeServer/ReadingRoomWx/userSchLabJwtApply";
        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()

                .uri(URI.create(Book_URL))
                .header("Content-Type", "application/json")
                .header("Accept", "*/*")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Origin", "https://mipweb.tit.edu.cn")
                .header("Referer", "https://mipweb.tit.edu.cn/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

            responseBody = response.body();
            //System.out.println("Status Code: " + response.statusCode());
            System.out.println("sendRequest>>>" + responseBody);
            return responseBody;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonData;

    }

    //重复预约查询
    private static String Query_URL = "https://mipservice.tit.edu.cn/consumeServer/ReadingRoomWx/getUserSchLabApplyHisLogViso";

    private static void QueryRequest() throws Exception {

        String jsonInputString = "{\"viso\":{\"cardNumber\":\"" + getConfig("学号") + "\"}}";

        StringBuilder results;
        URL url = new URL(Query_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // 设置请求方法和请求头
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        connection.setRequestProperty("Referer", "https://servicewechat.com/wx0367a00ab5db62a5/57/page-frame.html");
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
        connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
        connection.setDoOutput(true); // 允许输出


        // 发送请求体
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // 读取响应
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        // 解析 JSON 响应体
        JSONObject jsonResponse = new JSONObject(response.toString());
        JSONObject viso = jsonResponse.getJSONObject("viso");
        JSONArray list = viso.getJSONArray("list");

        results = new StringBuilder();

        // 获取今天的日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8")); // 设置时区为北京时间

        for (int i = 0; i < list.length(); i++) {
            JSONObject record = list.getJSONObject(i);
            String applyDate = record.getString("applyDate");
            if (applyDate.equals(LocalDate.now().plusDays(2).format(formatter))) {
                String roomName = record.getString("roomName"); // 楼层信息
                String seatNum = record.getString("seatNum"); // 座位号

                if (seatNum.equals(getConfig("座位号")) && roomName.equals(getConfig("自习室"))) {
                    results.append("当前用户在 " + LocalDate.now().plusDays(2).format(formatter) + "  已预约【首选座位】: ").append(roomName).append("的").append(seatNum).append("号座位");
                } else if (seatNum.equals(getConfig("备选座位号")) && roomName.equals(getConfig("备选自习室"))) {
                    results.append("当前用户在 " + LocalDate.now().plusDays(2).format(formatter) + "  已预约【备选座位】: ").append(roomName).append("的").append(seatNum).append("号座位");
                } else {
                    results.append("当前用户在 " + LocalDate.now().plusDays(2).format(formatter) + "  已预约: ").append(roomName).append("的").append(seatNum).append("号座位");
                }
            }
        }

        JOptionPane Repeat_JDialog = new JOptionPane(results.toString(), JOptionPane.INFORMATION_MESSAGE);
        JDialog Book_repeated_JDialog = Repeat_JDialog.createDialog(null, "重复预约");
        Book_repeated_JDialog.setModal(false); // 设置为非模态
        Book_repeated_JDialog.setVisible(true);
    }

    private static String TokenQuery() {
        String urlString = "https://mipservice.tit.edu.cn/txSchServer/StuInfoWx/getInfoFromWXSch";
        String openid = getConfig("个人令牌");
        String jsonInputString = "{\"openid\":\"" + openid + "\"}";

        try {
            // 创建 URL 对象并打开连接
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 设置请求头
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Host", "mipservice.tit.edu.cn");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setRequestProperty("Content-Length", String.valueOf(jsonInputString.getBytes(StandardCharsets.UTF_8).length));
            conn.setRequestProperty("xweb_xhr", "1");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 MicroMessenger/7.0.20.1781(0x6700143B) NetType/WIFI MiniProgramEnv/Windows WindowsWechat/WMPF WindowsWechat(0x63090a13) UnifiedPCWindowsWechat(0xf2540020) XWEB/11503");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Sec-Fetch-Site", "cross-site");
            conn.setRequestProperty("Sec-Fetch-Mode", "cors");
            conn.setRequestProperty("Sec-Fetch-Dest", "empty");
            conn.setRequestProperty("Referer", "https://servicewechat.com/wx0367a00ab5db62a5/59/page-frame.html");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");

            // 发送请求
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 读取响应
            StringBuilder response = new StringBuilder();
            try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
                while (scanner.hasNextLine()) {
                    response.append(scanner.nextLine());
                }
            }

            // 解析 JSON 响应获取 token
            JSONObject jsonResponse = new JSONObject(response.toString());
            String token = jsonResponse.getString("token");
            String userJwtToken = token.replace(" ", "%20");
            //System.out.println(userJwtToken);
            return userJwtToken;

        } catch (Exception e) {
            // 捕获并处理异常
            e.printStackTrace();
            return null;
        }
    }

    //创建楼层-自习室的集合
    private static String[] getRoomNames() {
        return new String[]{
                "二层-报刊阅览室",
                "三层-中文阅览室一",
                "三层-中文阅览室二",
                "三层-休闲区",
                "四层-中文阅览室三",
                "四层-中文阅览室四",
                "五层-外文阅览室",
                "五层-多功能阅览室",
                "五层-综合阅览室",
                "六层-多媒体阅览室",
                "六层-电子阅览室"};
    }

    //创建楼层-自习室指向数字的hashmap
    private static String getRoomId(String roomName) {
        Map<String, String> roomMap = new HashMap<>();
        roomMap.put("二层-报刊阅览室", "1");
        roomMap.put("三层-中文阅览室一", "2");
        roomMap.put("三层-中文阅览室二", "3");
        roomMap.put("三层-休闲区", "4");
        roomMap.put("四层-中文阅览室三", "5");
        roomMap.put("四层-中文阅览室四", "6");
        roomMap.put("五层-外文阅览室", "7");
        roomMap.put("五层-多功能阅览室", "16");
        roomMap.put("五层-综合阅览室", "8");
        roomMap.put("六层-多媒体阅览室", "9");
        roomMap.put("六层-电子阅览室", "10");
        return roomMap.getOrDefault(roomName, "1"); // 默认为 二层-报刊阅览室
    }

    //创建楼层-数字指向自习室的hashmap
    private static String getRoomName(String roomId) {
        Map<String, String> roomMap = new HashMap<>();
        roomMap.put("1", "二层-报刊阅览室");
        roomMap.put("2", "三层-中文阅览室一");
        roomMap.put("3", "三层-中文阅览室二");
        roomMap.put("4", "三层-休闲区");
        roomMap.put("5", "四层-中文阅览室三");
        roomMap.put("6", "四层-中文阅览室四");
        roomMap.put("7", "五层-外文阅览室");
        roomMap.put("16", "五层-多功能阅览室");
        roomMap.put("8", "五层-综合阅览室");
        roomMap.put("9", "六层-多媒体阅览室");
        roomMap.put("10", "六层-电子阅览室");
        return roomMap.getOrDefault(roomId, "二层-报刊阅览室"); // 默认为 二层-报刊阅览室
    }

    //每层的自习室对应的座位范围
    private static final Map<String, Integer[]> roomSeatRange = new HashMap<>();

    static {
        roomSeatRange.put("二层-报刊阅览室", new Integer[]{1, 307});
        roomSeatRange.put("三层-中文阅览室一", new Integer[]{1, 307});
        roomSeatRange.put("三层-中文阅览室二", new Integer[]{1, 307});
        roomSeatRange.put("三层-休闲区", new Integer[]{1, 56});
        roomSeatRange.put("四层-中文阅览室三", new Integer[]{1, 288});
        roomSeatRange.put("四层-中文阅览室四", new Integer[]{1, 288});
        roomSeatRange.put("五层-外文阅览室", new Integer[]{1, 416});
        roomSeatRange.put("五层-多功能阅览室", new Integer[]{1, 216});
        roomSeatRange.put("五层-综合阅览室", new Integer[]{1, 416});
        roomSeatRange.put("六层-多媒体阅览室", new Integer[]{1, 212});
        roomSeatRange.put("六层-电子阅览室", new Integer[]{1, 288});
    }

    private static String[] getDaySets() {
        return new String[]{
                "后天" + " ( " + LocalDate.now().plusDays(2).format(formatter1) + " )",
                "明天" + " ( " + LocalDate.now().plusDays(1).format(formatter1) + " )",
                "今天" + " ( " + LocalDate.now().plusDays(0).format(formatter1) + " )",
        };
    }

    private static String getNo_Day(String DayON) {
        Map<String, String> Daymap = new HashMap<>();
        Daymap.put("后天" + " ( " + LocalDate.now().plusDays(2).format(formatter1) + " )", "2");
        Daymap.put("明天" + " ( " + LocalDate.now().plusDays(1).format(formatter1) + " )", "1");
        Daymap.put("今天" + " ( " + LocalDate.now().plusDays(0).format(formatter1) + " )", "0");
        return Daymap.getOrDefault(DayON, "2"); // 默认为 后天
    }

    private static void SoftDog() throws Exception {

        // 使用 JOptionPane 获取用户输入的卡密
        String ID = JOptionPane.showInputDialog(null, "请输入卡密(联系qq1781468911购买):", "卡密验证", JOptionPane.QUESTION_MESSAGE);
        // 如果用户点击取消或关闭对话框，ID 为 null
        if (ID == null) {
            System.exit(0); // 退出程序
        }
        // 去除输入的多余空格
        ID = ID.trim();
        // 验证卡密

        if (ID.equals(getFollowerCount())) {
            JOptionPane.showMessageDialog(null, "卡密验证成功", "成功", JOptionPane.INFORMATION_MESSAGE);
            validateCard();
            CreateConfig();
        } else {
            JOptionPane.showMessageDialog(null, "卡密验证失败", "失败", JOptionPane.ERROR_MESSAGE);
            SoftDog();
        }
    }

    //创建配置文件的UI面板
    private static void CreateConfig() throws Exception {
        JFrame frame = new JFrame("座位预约_"+LatestVersion());//设置窗口的title
        frame.setResizable(false); //设置窗口不可调整大小
        frame.setSize(280, 420); // 设置窗口的初始大小
        ImageIcon icon = new ImageIcon("resource//IMG\\座位预约.png");
        frame.setIconImage(icon.getImage());//给窗体设置图标方法

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//设置窗口关闭后关闭程序
        frame.setLocationRelativeTo(null);//居中定位

        JLabel labelCardNumber = new JLabel("学  号:");
        labelCardNumber.setFont(new Font("宋体", Font.BOLD, 14));
        JTextField textFieldCardNumber = new JTextField(18);
        textFieldCardNumber.setPreferredSize(new Dimension(14, 25));
        textFieldCardNumber.setFont(new Font("宋体", Font.BOLD, 16));

        JLabel labelJwtToken = new JLabel("令  牌:");
        labelJwtToken.setFont(new Font("宋体", Font.BOLD, 14));
        JTextField textFieldJwtToken = new JTextField(18);
        textFieldJwtToken.setPreferredSize(new Dimension(14, 25));
        textFieldJwtToken.setFont(new Font("宋体", Font.BOLD, 16));
        textFieldJwtToken.setBackground(Color.WHITE);
        textFieldJwtToken.setForeground(new Color(0, 122, 255)); // 设置前景色（文本颜色）
        textFieldJwtToken.setBorder(BorderFactory.createLineBorder(new Color(0, 122, 255))); // 设置下拉框的边框色为黑色


        JLabel labelRoomName = new JLabel("自习室:");
        JComboBox<String> comboBoxRoomName = new JComboBox<>(getRoomNames());
        labelRoomName.setFont(new Font("宋体", Font.BOLD, 14));
        comboBoxRoomName.setFont(new Font("宋体", Font.BOLD, 14));
        comboBoxRoomName.setBackground(Color.WHITE); // 设置背景色为白色
        comboBoxRoomName.setForeground(Color.BLACK); // 设置前景色（文本颜色）
        comboBoxRoomName.setBorder(BorderFactory.createLineBorder(new Color(0, 122, 255))); // 设置下拉框的边框色为黑色
        comboBoxRoomName.setPreferredSize(new Dimension(164, 28)); // 宽度为100像素，高度为25像素

        JLabel labelSeatNum = new JLabel("座位号:");
        labelSeatNum.setFont(new Font("宋体", Font.BOLD, 14));
        JTextField textFieldSeatNum = new JTextField(18);
        textFieldSeatNum.setPreferredSize(new Dimension(14, 25));
        textFieldSeatNum.setFont(new Font("宋体", Font.BOLD, 16));

        JLabel labelRoomName1 = new JLabel("备选室:");
        JComboBox<String> comboBoxRoomName1 = new JComboBox<>(getRoomNames());
        labelRoomName1.setFont(new Font("宋体", Font.BOLD, 14));
        comboBoxRoomName1.setFont(new Font("宋体", Font.BOLD, 14));
        comboBoxRoomName1.setBackground(Color.WHITE); // 设置背景色为白色
        comboBoxRoomName1.setForeground(Color.BLACK); // 设置前景色（文本颜色）
        comboBoxRoomName1.setBorder(BorderFactory.createLineBorder(new Color(0, 122, 255))); // 设置下拉框的边框色
        comboBoxRoomName1.setPreferredSize(new Dimension(164, 28)); // 宽度为100像素，高度为25像素

        JLabel labelSeatNum1 = new JLabel("备选号:");
        labelSeatNum1.setFont(new Font("宋体", Font.BOLD, 14));
        JTextField textFieldSeatNum1 = new JTextField(18);
        textFieldSeatNum1.setPreferredSize(new Dimension(15, 25));
        textFieldSeatNum1.setFont(new Font("宋体", Font.BOLD, 16));

        JLabel labelDaySet = new JLabel("天  限:");
        JComboBox<String> comboBoxDaySet = new JComboBox<>(getDaySets());
        labelDaySet.setFont(new Font("宋体", Font.BOLD, 14));
        comboBoxDaySet.setFont(new Font("宋体", Font.BOLD, 14));
        comboBoxDaySet.setBackground(Color.WHITE); // 设置背景色为白色
        comboBoxDaySet.setForeground(Color.BLACK); // 设置前景色（文本颜色）
        comboBoxDaySet.setBorder(BorderFactory.createLineBorder(new Color(0, 122, 255))); // 设置下拉框的边框色
        comboBoxDaySet.setPreferredSize(new Dimension(164, 28)); // 宽度为100像素，高度为25像素

        JLabel Timer = new JLabel("时  间:");
        Timer.setFont(new Font("宋体", Font.BOLD, 14));

        JComboBox<String> comboBoxHour = new JComboBox<>();
        for (int i = 0; i <= 23; i++) {
            comboBoxHour.addItem(String.format("%02d", i));//格式化占位符，表示数字至少占两位，如果不足两位则用0填充
        }

        comboBoxHour.setBackground(Color.WHITE); // 设置背景色为白色
        comboBoxHour.setForeground(Color.BLACK); // 设置前景色（文本颜色）
        comboBoxHour.setBorder(BorderFactory.createLineBorder(new Color(0, 122, 255)));
        comboBoxHour.setPreferredSize(new Dimension(46, 25));
        comboBoxHour.setFont(new Font("宋体", Font.BOLD, 15));

        JLabel colon = new JLabel(":");
        colon.setFont(new Font("微软雅黑", Font.BOLD, 10));

        JComboBox<String> comboBoxMinute = new JComboBox<>();
        for (int i = 0; i <= 59; i++) {
            comboBoxMinute.addItem(String.format("%02d", i));
        }

        comboBoxMinute.setBackground(Color.WHITE); // 设置背景色为白色
        comboBoxMinute.setForeground(Color.BLACK); // 设置前景色（文本颜色）
        comboBoxMinute.setBorder(BorderFactory.createLineBorder(new Color(0, 122, 255)));
        comboBoxMinute.setPreferredSize(new Dimension(46, 25));
        comboBoxMinute.setFont(new Font("宋体", Font.BOLD, 15));

        JLabel colon1 = new JLabel(":");
        colon1.setFont(new Font("微软雅黑", Font.BOLD, 10));

        JComboBox<String> comboBoxSecond = new JComboBox<>();
        for (int i = 0; i <= 59; i++) {
            comboBoxSecond.addItem(String.format("%02d", i));
        }
        comboBoxSecond.setBackground(Color.WHITE); // 设置背景色为白色
        comboBoxSecond.setForeground(Color.BLACK); // 设置前景色（文本颜色）为黑色
        comboBoxSecond.setBorder(BorderFactory.createLineBorder(new Color(0, 122, 255)));
        comboBoxSecond.setPreferredSize(new Dimension(46, 25));
        comboBoxSecond.setFont(new Font("宋体", Font.BOLD, 15));

        JButton buttonSave = new JButton("保   存");
        buttonSave.setFont(new Font("微软雅黑", Font.BOLD, 14));
        buttonSave.setBackground(new Color(0, 122, 255));
        buttonSave.setForeground(Color.WHITE);

        buttonSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String cardNumber = textFieldCardNumber.getText().trim();
                String roomName = (String) comboBoxRoomName.getSelectedItem();
                String roomName1 = (String) comboBoxRoomName1.getSelectedItem();

                String labRoomId = getRoomId(roomName);
                String labRoomId1 = getRoomId(roomName1);

                String seatNumStr = textFieldSeatNum.getText().trim();
                String seatNumStr1 = textFieldSeatNum1.getText().trim();

                String userJwtToken = textFieldJwtToken.getText();

                String Hour = (String) comboBoxHour.getSelectedItem();
                String Minute = (String) comboBoxMinute.getSelectedItem();
                String Second = (String) comboBoxSecond.getSelectedItem();

                String DaySet = (String) comboBoxDaySet.getSelectedItem();
                String Day = getNo_Day(DaySet);

                if (cardNumber.isEmpty() || seatNumStr.isEmpty() || seatNumStr1.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "学号、座位号、备选座位号、令牌不能为空！");
                    return;
                }
                if (!isPositiveInteger(cardNumber) || !isPositiveInteger(seatNumStr) || !isPositiveInteger(seatNumStr1)) {
                    JOptionPane.showMessageDialog(frame, "学号、座位号、备选座位号必须是数字！");
                    return;
                }
                if (seatNumStr.equals(seatNumStr1) && labRoomId.equals(labRoomId1)) {
                    JOptionPane.showMessageDialog(frame, "座位号和备选座位号不能相同！");
                    return;
                }


                int seatNum = Integer.parseInt(seatNumStr);
                int seatNum1 = Integer.parseInt(seatNumStr1);

                Integer[] range = roomSeatRange.get(roomName);
                Integer[] range1 = roomSeatRange.get(roomName1);

                if (seatNum < range[0] || seatNum > range[1] || seatNum1 < range1[0] || seatNum1 > range1[1]) {
                    if (roomName.equals(roomName1)) {
                        JOptionPane.showMessageDialog(frame, "座位号或备选座位号填写错误！" + roomName + "的座位号范围：" + range[0] + "-" + range[1], "信息错误", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(frame, "座位号或备选座位号填写错误！" + "\n" + roomName + "的座位号范围：" + range[0] + "-" + range[1] + "\n" + roomName1 + "的座位号范围：" + range1[0] + "-" + range1[1], "信息错误", JOptionPane.ERROR_MESSAGE);
                    }
                    return;
                }

                String Time = Hour + ":" + Minute + ":" + Second;
                saveConfig(cardNumber, userJwtToken, labRoomId, seatNum, labRoomId1, seatNum1, Time, Day);  // 保存配置
                loadConfigIntoMemory();
                JOptionPane.showMessageDialog(null, "保存配置成功！");
                frame.dispose();

                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

                Runnable task = () -> {
                    try {
                        if (TestSystemDelay()) {
                            createTask().run();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane Block_JDialog = new JOptionPane("网络连接中断或预约请求响应超时，小程序瘫痪中ing······", JOptionPane.WARNING_MESSAGE);
                        JDialog Running_Block_JDialog = Block_JDialog.createDialog(null, "响应超时");
                        Running_Block_JDialog.setModal(false); // 设置为非模态
                        Running_Block_JDialog.setVisible(true);
                    }
                };

                LocalDateTime now = LocalDateTime.of(LocalDate.now(), LocalTime.now());
                LocalTime scheduledTime1 = LocalTime.parse(Time, TIME);
                LocalDateTime scheduledTime = LocalDateTime.of(LocalDate.now(), scheduledTime1);

                long delay = Duration.between(now, scheduledTime).toMillis();//between(开始,结束)

                if (delay <= 0) {
                    createTask().run();
                    delay += Duration.ofDays(1).toMillis();
                } else {
                    if (AutoStart()) {
                        JOptionPane AutoStart_JDialog = new JOptionPane("  （@ • ‿ • @ ）  预约程序已配置自启动！", JOptionPane.PLAIN_MESSAGE);
                        JDialog AutoStart_successed_JDialog = AutoStart_JDialog.createDialog(null, "自启配置");
                        AutoStart_successed_JDialog.setModal(false); // 设置为非模态
                        AutoStart_successed_JDialog.setVisible(true);
                    } else {
                        JOptionPane AutoStart1_JDialog = new JOptionPane("  (〒︿〒)  预约程序配置自启动失败！以管理员身份重试", JOptionPane.PLAIN_MESSAGE);
                        JDialog Running_failed_JDialog = AutoStart1_JDialog.createDialog(null, "自启配置");
                        Running_failed_JDialog.setModal(false); // 设置为非模态
                        Running_failed_JDialog.setVisible(true);
                    }
                }
                //安排任务每天特定时间执行一次
                scheduler.scheduleAtFixedRate(task, delay, Duration.ofDays(1).toMillis(), TimeUnit.MILLISECONDS);
            }
        });

        JPanel panel = new JPanel();
        panel.add(labelCardNumber);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(textFieldCardNumber);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(labelJwtToken);
        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(textFieldJwtToken);
        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(labelRoomName);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(comboBoxRoomName);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(labelSeatNum);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(textFieldSeatNum);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(labelRoomName1);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(comboBoxRoomName1);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(labelSeatNum1);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(textFieldSeatNum1);


        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(labelDaySet);
        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(comboBoxDaySet);
        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(Timer);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(comboBoxHour);
        panel.add(colon);
        panel.add(comboBoxMinute);
        panel.add(colon1);
        panel.add(comboBoxSecond);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(buttonSave);
        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(50));

        frame.add(panel);
        frame.setVisible(true);
    }

    //使用正则表达式来匹配字符串为正整数，不含有空格等其他字符
    public static boolean isPositiveInteger(String str) {
        Pattern pattern = Pattern.compile("^[0-9]\\d*$");
        Matcher matcher = pattern.matcher(str.trim()); // 使用trim()去除前后空格
        return matcher.matches();
    }

    public static boolean isTimeComplete(String time) {
        // 正则表达式，用于匹配格式为HH:mm的时间字符串
        Pattern pattern = Pattern.compile("^(2[0-3]|[01]?[0-9]):([0-5]?[0-9]):([0-5]?[0-9])$");
        Matcher matcher = pattern.matcher(time.trim());
        // 如果时间字符串匹配正则表达式，则时间完整且格式正确
        return matcher.matches();
    }

    // 判断文件不存在或读取错误时认为配置不完整
    private static boolean isConfigIncomplete() {
        File configFile = new File(CONFIG_FILE_PATH);
        if (configFile.exists()) {
            try {
                // 读取文件内容
                String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE_PATH)));
                // 检查文件内容是否为空
                return content.trim().isEmpty();
            } catch (IOException e) {
                return true;
            }
        } else {
            return true;
        }
    }

    private static boolean ConfigComplete() {
        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("=")) {
                    String[] parts = line.split("=", 2); // 限制分割为两部分
                    if (parts.length > 1) {
                        String beforeEqual = parts[0].trim(); // 去除前后空白字符
                        String afterEqual = parts[1].trim(); // 去除前后空白字符
                        // 检查等号前后的内容是否都存在
                        if (beforeEqual.isEmpty() || afterEqual.isEmpty()) {
                            return true; // 如果等号前后任一部分为空，返回
                        }
                    } else {
                        return true; // 如果没有等号后面的部分，返回
                    }
                } else {
                    return true;
                }
            }

        } catch (IOException e) {
            return true; // 发生异常时，返回
        }
        loadConfigIntoMemory();
        return false; // 所有行都检查完毕，没有发现等号前后没有内容的情况
    }

    //保存创建的config.txt配置文件，获取填写在面板的信息并写入文件
    private static void saveConfig(String cardNumber, String openid, String labRoomId, int seatNum, String labRoomId1, int seatNum1, String Time, String Day) {
        String content = String.format("学号=%s\n个人令牌=%s\n自习室=%s\n座位号=%s\n备选自习室=%s\n备选座位号=%s\n预约时间=%s\n预约天限=%s\n", cardNumber, openid, labRoomId, seatNum, labRoomId1, seatNum1, Time, Day);
        try {
            Files.writeString(Paths.get(CONFIG_FILE), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //自启动配置

    private static boolean AutoStart() {
        try {
            // library.exe 的相对路径（假设在当前工作目录的根目录下）
            Path lnkPath = Path.of("C:\\ProgramData\\Microsoft\\Windows\\Start Menu\\Programs\\LibrarySeatBook.lnk");

            // 目标路径（Windows 启动文件夹）
            String startupFolder = System.getenv("APPDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup\\";
            Path targetPath = Path.of(startupFolder, "LibrarySeatBook.lnk");
            // 复制文件
            Files.copy(lnkPath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            return true;

        } catch (Exception ex) {
            return false;
        }
    }

    // 加载配置文件中的信息
    private static String loadJsonData(String dateStr) {
        String cardNumber = getConfig("学号");
        String labRoomId = getConfig("自习室");
        String seatNum = getConfig("座位号");
        String userJwtToken = TokenQuery();
        return String.format("{\"cardNumber\": \"%s\", \"applyDate\": \"%s\", \"applyStartTime\": \"06:30\", \"applyDuration\": \"15.5\", \"labRoomId\": \"%s\", \"seatNum\": \"%s\", \"userJwtToken\": \"%s\"}",
                cardNumber, dateStr, labRoomId, seatNum, userJwtToken);
    }

    private static String loadJsonData1(String dateStr) {
        String cardNumber = getConfig("学号");
        String labRoomId1 = getConfig("备选自习室");
        String seatNum1 = getConfig("备选座位号");
        String userJwtToken = TokenQuery();
        return String.format("{\"cardNumber\": \"%s\", \"applyDate\": \"%s\", \"applyStartTime\": \"06:30\", \"applyDuration\": \"15.5\", \"labRoomId\": \"%s\", \"seatNum\": \"%s\", \"userJwtToken\": \"%s\"}",
                cardNumber, dateStr, labRoomId1, seatNum1, userJwtToken);
    }


    //读取配置文件中的信息
    private static final String CONFIG_FILE_PATH = System.getProperty("user.dir") + File.separator + "config.txt";

    private static void loadConfigIntoMemory() {
        try {
            Path configFilePath = Paths.get(CONFIG_FILE_PATH);
            if (Files.exists(configFilePath)) {
                String content = Files.readString(configFilePath);
                for (String line : content.lines().toList()) {
                    if (!line.trim().isEmpty()) {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            configCache.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> configCache = new HashMap<>(); // 存储配置的内存映射

    // 根据键从内存映射中获取配置值
    private static String getConfig(String key) {
        return configCache.get(key);
    }
}



