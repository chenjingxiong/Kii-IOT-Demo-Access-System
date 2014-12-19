#IOT Demo Android
用户进入应用后，将会看到三个功能按钮，Scan、App和Register。此文将说明这三个功能的使用和注意事项。

##1.Activate door control
此功能主要为扫描周围的transmitter，读取其数据，并对数据进行以下处理。
###1.1 授权
当transmitter距离detector足够近的距离时，detector将获得transmitter设备资料并进上传到server从而进行授权判断，并返回结果。
####发送设备资料
~~~java
 	JSONObject rawArg = new JSONObject();
    
    // Set the custom parameters.
    rawArg.put("bluetooth_addr", mBeaconMajorMinor);
    KiiServerCodeEntryArgument arg = KiiServerCodeEntryArgument
            .newArgument(rawArg);
    
    // Execute the Server Code
    KiiServerCodeExecResult res = entry.execute(arg);
    //
    // Parse the result.
    JSONObject returned = res.getReturnedValue();
    returnedValue = returned.getString("returnedValue");
~~~
#####注意事项
transmitter进入可发送距离后，授权判断请求只会发送一次。除非transmitter离开一定距离后重新进入可发送距离才会触发第二次发送。
##2.App
此功能主要为让用户进行登陆，从而根据自己的用户类型可以进行查看access log和管理用户功能。点击App模块后直接进入用户登陆界面，其他功能都需要先登陆才可以进行。

#### 登录
```
	try {
		user = KiiUser.logIn(username, password);
	    }
	    token = user.getAccessToken();
	} catch (Exception e) {
	    token = null;
	}
```
#####注意事项
如果想体验全部功能，请使用admin账户登陆，用户名：leonardean，密码：123123。
###2.1 管理(Manage)
admin和在security组中的用户可以查看所有群组中的用户并进行修改。admin可修改的部分为两个，一个是修改用户授权状态，另一个是修改用户的所在群组。而Security中的用户只可以改变用户的授权状态。

选择群组

![](http://img1.ph.126.net/Na1bI7MbRmpobB62UuwZ1g==/6608407930563547807.jpg)

用户状态修改

![](http://img1.ph.126.net/VotOLF9VUSLa8RYy6pRbuw==/2637702006855754156.jpg)

列表里面每个item有三个属性：
* 用户名（username）
* 所在群组 (employee, security, anonymous)
* 是否授权通过（authorize）
	* 点击此按钮将会更改此用户的授权状态

####更改授权
```
try{
    mKiiObject.refresh();
    //strings[0]为相反的现有状态
    mKiiObject.set("authority", strings[0]);
    mKiiObject.save();
} catch (AppException e) {
    // handle error
} catch (IOException e) {
    // handle error
}
```
#### 添加用户到security
点击add user to security按钮并输入username，将会将用户加入到security group。
#### 添加用户到employee
同上，用户加入到employee group
```
	mAddedUser = null;
	try {
	    mAddedUser = KiiUser.findUserByUserName(mAddedUserName);
	}catch (Exception e){
	    e.printStackTrace();
	}
	if(mAddedUser == null){
		//用户不存在
	    return null;
	}
	mSecurityGroup.addUser(mAddedUser);
	try{
	    mSecurityGroup.save();
	}catch (Exception e){
	    e.printStackTrace();
	}
```
#### 移除用户
![](http://img0.ph.126.net/oofUhd7s7-LUNcCkJb2d4Q==/6608584951934574741.jpg)
```
try {
	 //params[0]是username
    KiiUser kiiUser = KiiUser.findUserByUserName(params[0]);
    if (mGroupChoice.equals("security")) {
        mSecurityGroup.removeUser(kiiUser);
        mSecurityGroup.save();
    } else if (mGroupChoice.equals("employee")) {
        mEmployeeGroup.removeUser(kiiUser);
        mEmployeeGroup.save();
    }

} catch (Exception e) {
    errorMsg = e.getLocalizedMessage();
}
```
### 2.2 登陆记录（Access Log）
查看登陆记录。

![](http://img2.ph.126.net/w0qDUKlA_LLI78-GygCxaA==/3115083567274649501.jpg)

列表里面的每个item有三个属性：
* 用户名（username）
* 扫描时间
* 是否授权通过

####得到登录记录列表
```
	try{
        KiiQuery all_query = new KiiQuery();
        KiiQueryResult<KiiObject> result = Kii.bucket("access_log").query(all_query);
        mObjLists = result.getResult();
    }catch (IOException e) {
        // handle error
    } catch (AppException e) {
        // handle error
    }
```

##### 注意事项
普通用户只能看到自己的登陆记录，admin可以看到所有的用户记录。
### 2.3 注销
```
 KiiUser.logOut();
```
##3.注册
注册时需要首先扫描transmitter的设备信息，并得到其major和minor的id，然后上传到服务器进行注册。
#### 注册
```
	KiiUser.Builder builder = KiiUser.builderWithName(username);
	user = builder.build();
	try {
		//注册
	    user.register(password);
	    
		//上传major和minor的id到用户资料
	    UserFields userFields = new UserFields();
	    userFields.set("bluetooth_addr", mBeaconMajorMinor);
	    user.update(userFields);

	}catch (Exception e){
	    e.printStackTrace();
	}
```
#####注意事项
当detector没有得到major和minor的id时，用户无法注册。




