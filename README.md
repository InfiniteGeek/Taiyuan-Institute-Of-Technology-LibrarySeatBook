# Taiyuan-Institute-Of-Technology-LibrarySeatBook(太原工业学院预约图书馆座位软件)

## 使用说明

1.双击打开LibrarySeatBook.exe安装程序，安装后填写学号等信息【其中‘令牌’需要手动使用Fiddler抓取，看《如何抓取Token令牌》】

时间设置为20：30：50、天限设置保持默认(默认预约后天)，保存设置

2.电脑在预约时间前(20：30：50)【秒数可以提前几秒】保持开机，即可完成运行

3.设置不睡眠不锁屏仅息屏【看《修改电源计划-不睡眠不锁屏仅息屏》】

 

 

 

## 如何抓取Token个人令牌

1.下载Fiddler抓包软件并自解压

<img src="https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250320105734486.png" alt="image-20250320105734486" style="zoom: 50%;" />

2.去自解压目录找到软件并打开，快捷方式和应用本体都可以打开

![image-20250325081732731](https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250325081732731.png)

3.取消更新和警告，配置软件

<img src="https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250320105916931.png" alt="image-20250320105916931" style="zoom:50%;" />

3.1 打开选项进⾏设置，在‘基本’项中，取消勾选‘在启动时通知更新’。

![image-20250320110013676](https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250320110013676.png)

![image-20250325081909657](https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250325081909657.png)

3.2 在‘Http’项中勾选解密HTTP；打开‘动作’，先将根证书导出至桌面，随后信任；勾选忽略服务器证书错误。

![image-20250320110359016](https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250320110359016.png)

3.3 登录电脑版微信并打开⼩程序⾯板，打开预约⼩程序即可，[无需点进‘图书馆预约’]。

<img src="https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250420220318281.png" alt="image-20250420220318281" style="zoom: 50%;" />

3.4 查看Fiddler抓包情况，找到最后⼀次mipsevice.tit.edu.cn的host；先点击左侧的‘检查’ 🔍 按钮，随后
设置上下窗⼝为‘原始’模式。在上⽅窗⼝显示‘openid’，则抓取正确。将‘openid’中的内容[不含引号]全部
复制下来，填写进新版软件的‘令牌’⼀栏。

![image-20250420220427442](https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250420220427442.png)

 

 

 

## 修改电源计划-不睡眠不锁屏仅息屏（为了防止睡眠睡死不执行预约命令）

## 1.不睡眠

### 1.1 window+r，输入‘control’打开控制面板

![image-20250217221021638](https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250217221021638.png)

### 1.2 进入控制面板，设置成‘小图标’界面

<img src="https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250217221555994.png" alt="image-20250217221555994" style="zoom:50%;" />

### 1.3 选择电源选项

<img src="https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250217221654828.png" alt="image-20250217221654828" style="zoom:50%;" />

### 1.4 点击更改计划

<img src="https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250217221952676.png" alt="image-20250217221952676" style="zoom:50%;" />

### 1.5 更改高级电源设置

<img src="https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250217222021776.png" alt="image-20250217222021776" style="zoom:50%;" />

### 1.6 高级电源计划设置

#### 1.6.1 设置‘在此时间后关闭硬盘’，将‘使用电源’和‘接通电源’设置为‘从不’，数字为‘0’

<img src="https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250217222127748.png" alt="image-20250217222127748" style="zoom:67%;" />

#### 1.6.2 将睡眠改为‘从不’，‘允许使用唤醒定时器’设置为‘启用’

<img src="https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250217222415915.png" alt="image-20250217222415915" style="zoom:67%;" />

#### 1.6.3 息屏设置，时间自行设置（建议设置5min）

<img src="https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250217222809347.png" alt="image-20250217222809347" style="zoom:67%;" />

#### 1.6.4 选择‘应用’保存设置

### 1.7 回到1.4步骤的页面

#### 1.7.1 点击设置合盖或电源按钮的功能

<img src="https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250221002040352.png" alt="image-20250221002040352" style="zoom:50%;" />

#### 1.7.2 全部设置为“不采取任何操作”，可以设置合盖睡眠，但不要合盖，睡眠情况下，软件不运行！

<img src="https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250221002211379.png" alt="image-20250221002211379" style="zoom:50%;" />

## 2.不锁屏（win10为例，win11类似）

### 2.1 window+i，快捷打开系统设置

<img src="https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250221000429044.png" alt="image-20250221000429044" style="zoom:50%;" />

### 2.2 个性化

<img src="https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250221000541087.png" alt="image-20250221000541087" style="zoom:50%;" />

### 2.3 屏幕保护程序设置

<img src="https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250221000648702.png" alt="image-20250221000648702" style="zoom:50%;" />

#### 2.3.1 取消勾选“在恢复时显示登陆屏幕”；“屏幕”保护程序设置为“无”

<img src="https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250221000901067.png" alt="image-20250221000901067" style="zoom:50%;" />

#### 2.3.2 确认保存

## 3.仅息屏（win10为例，win11类似）

### 3.1 window+i，快捷打开设置

<img src="https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250221001417738.png" alt="image-20250221001417738" style="zoom:50%;" />

### 3.2 登录选项设置，将“离开电脑要求重新登录”设置为“从不”

![image-20250221001531890](https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250221001531890.png)

 

 

 

## 提示：如果软件界面模糊，解决方法：

右键exe-属性-兼容性-更改更高的DPI

将“使用此设置修复此程序的缩放问题，而不是“设置"中的缩放问题”和“高DPI缩放替代 替代高DPI缩放行为(应用程序)”这两项打勾✔

![image-20250420221004785](https://cdn.jsdelivr.net/gh/InfiniteGeek/Picture/windows/image-20250420221004785.png)
