Kii-IOT-Demo-Access-System
==========================
本Demo使用了Kii Cloud的Thing相关功能。截止至本文发布，Kii Cloud国服尚没有Thing功能，所以请开发者于[开发者平台](https://developer.kii.com)中创建美服或者日服Kii Cloud App以调试本Demo。

在调试本Demo的具体代码之前，请首先根据Demo文档通过REST API完成基础建设并记录下以下信息用于参数替换:
* 安保组长`Security Lead`的`access_token`
* 安保组`security`的`groupID`
* 门禁系统 Thing `access_system`的
	* `_venderThingID`
	* `_password`
	* `_accessToken`

##安卓程序
`IOT_Demo_Android(AS)`包含了本demo的安卓应用部分的代码，即门禁系统，是一个Android Studio的项目。由于本Demo安卓应用使用了蓝牙低功耗功能(BLE)，开发者需要`4.4`版本的安卓SDK(API level: 20)编译此应用。在调试时，开发者可以在`Constants.java`中自行更改`App ID`、`App Key`和`Site`来与自己所创建的Kii Cloud App后台所绑定。除此之外，还需要替换`SECURITY_GROUP_NAME`和`EMPLOYEE_GROUP_NAME`。

##Server Extension
`IOT_Demo_Server_Extension`包含了本demo的server extension部分的JS函数代码及Hook配置文件。开发者需要自行根据自己的应用调整以下参数：
* `security`组的`groupID`
* `access_system`的`_accessToken`
* `SECURITY_GROUP_NAME`
* `EMPLOYEE_GROUP_NAME`。

欲将server extension部署于自己的Kii Cloud App后台，开发者需要首先安装[命令行工具](http://documentation.kii.com/cn/guides/commandlinetools/#installation)，下载完毕后于命令行中切换到命令行工具根目录下后执行以下命令即可部署server extension：
```
node bin/kii-servercode.js deploy-file \
  --file <js_function_file> \
  --site us \
  --app-id <your_app_id> \
  --app-key <your_app_key> \
  --client-id <your_client_id> \
  --client-secret <your_client_secret> \
  --hook-config <hook_file>
```
##安全监视器
`IOT_Demo_serurity_monitor`包含了本demo的用于接收MQTT推送消息的安全监视器客户端，是一个C程序。开发者可于`kii_demo.h`中自行更改应用相关信息，其中：
* `STR_DEVICE_VENDOR_ID` - 门禁系统的设备注册ID
* `STR_DEVICE_PASSWORD` - 门禁系统的设备注册密码
* `STR_BUCKET_NAME` - 用于存放进入记录的门禁系统Thing域Bucket名称

开发者可在Unix环境命令行(Linux, MacOS, Windows下的cygwin)中用以下命令编译该C程序：

```
make
```
运行该程序需用以下命令:
```
./kii_demo
```
程序开启后会进行初始化，待命令行中出现以下文字后即可正常接收MQTT推送消息：
```
Get thing token success
Initialize push success
```