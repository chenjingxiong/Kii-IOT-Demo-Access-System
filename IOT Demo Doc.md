##背景简介
![](https://raw.githubusercontent.com/leonardean/ImgUpload/master/B.png)

趋势表明物联网技术具有很大的潜力来从多角度提高人们的生活质量。现今物联网的概念已经在传感设备、智能家居、可穿戴设备中有所体现。基于硬件设备，云端服务对数据价值体现以及业务逻辑实现起着重要作用。[Kii Cloud](http://cn.kii.com/)致力于为物联网设备提供优质专业的云服务, 为智能硬件设备提供用户管理，设备管理，数据管理，远程控制, 数据分析, 消息推送等功能。开发者可以从几乎所有平台（[iOS](http://documentation.kii.com/cn/guides/ios/)，[安卓](http://documentation.kii.com/cn/guides/android/)，[Unity](http://documentation.kii.com/cn/guides/unity/)，[Javascript](http://documentation.kii.com/cn/guides/javascript/)，[REST](http://documentation.kii.com/cn/guides/rest/)）利用相应SDK开发包使用Kii Cloud所提供的云端服务，从而自由灵活地实现各种后端业务逻辑。本文将引入一个典型的物联网应用场景的Demo，并详细介绍如何有效使用Kii Cloud以驱动整个物联网应用。

为了很好的理解本文中所提到的技术方案，请确保在阅读本文之前对以下内容有所了解：

* [用户管理](http://documentation.kii.com/cn/starts/cloudsdk/managing-users/)以及[群组管理](http://documentation.kii.com/cn/starts/cloudsdk/managing-groups/)
* [设备管理](http://documentation.kii.com/cn/starts/thingsdk/)
* [数据管理](http://documentation.kii.com/cn/starts/cloudsdk/managing-data/)：[KiiBucket](http://documentation.kii.com/cn/starts/cloudsdk/cloudoverview/bucket/)和[KiiObject](http://documentation.kii.com/cn/starts/cloudsdk/cloudoverview/kiiobject/)
* [Server Extension](http://documentation.kii.com/cn/guides/serverextension/)
* [消息推送](http://documentation.kii.com/cn/starts/thingsdk/push-notification/)

##应用场景
本文所引入的Demo的应用场景是一个基于低功耗蓝牙(BLE)Beacon技术的门禁系统。假设在一个公司里，每个房间的进入许可人员需要有一个门禁系统来控制。(如工程师可以进入实验室，市场部人员可以进入接待会议室，总经理可以进入个人办公室等)每个员工则需要一个可以代表其身份标识的随身设备(Beacon Transmitter)来通过蓝牙和门禁系统(Access System)进行数据交互，从而验证其是否有权限进入该房间。每次当有人试图进入一个房间时，不论结果如何，门禁系统都会把当次进入记录保存在云端以供日后查看。此应用场景中的另一个角色是安保组，他们可以在安保室中通过安全监视器(Security Monitor)得到最新的房间进入事件。每个员工可以在门禁系统上查看自己的进入记录，而安保组的人员则可以查看所有的进入记录。

总结看来，整个应用场景涉及到三个设备：

* 一个具有低功耗蓝牙功能的Beacon发射器，供每个公司人员随身携带用于房间进入时的身份验证。
* 一个用作门禁系统的Beacon扫描设备，安装于每个房门且可以操控门的自动开关。当扫描到附近有Beacon信号时则会根据该Beacon发射器的蓝牙标识判断该人员的进入权限并作出相应操作(是否开门),并把此次进入事件保存至云端。
* 一个用作安全监视器的设备。当有进入事件发生的时候，云端则会发出一条基于MQTT协议的推送消息，安全监视器则会接收并显示此消息，当该进入记录为无权限进入时，则会开启警报。
 
下图是整个应用场景的展示：
![](http://chuantu.biz/t/55/1417748803x-1376440095.png)
##后台设计
以上所述的应用场景可以通过充分利用Kii Cloud的功能特性来进行后台设计。以下将从用户，群组，设备，数据管理，权限，推送机制等方面阐述如何在Kii Cloud中搭建一个适用于以上物联网场景的云端服务：
####用户管理
本应用中包含一个特殊的用户，即为安保组长`Security Lead`，他将有着最高的权限来管理安保组和员工组。
####群组管理
本应用中所包含的两个用户群组：安保组`security`、员工组`employee`需皆由安保组长创建，且对两个组内的成员进行添加与删除。
####设备管理
由于在此次Demo中，我们会用现有的Beacon发射器来作为员工的身份标识，所以本应用中与Kii Cloud有数据交互的设备有两个：门禁系统`Access System`和安全监视器`Security Monitor`。虽然在物理层面上，门禁系统和安全监视器是两个设备，但是从应用角度考虑，它们所拥有的权限相同且没有个体特征属性，所以在Kii Cloud中使用同一个身份标识，即为**同一个**Thing。
####数据管理
在本应用中会有两个Bucket，分别是：

* `security`组下的Group域Bucket `security_profile`。这个Bucket用于存放和管理所有用户的安全档案，当每个用户被创建的时候，Server Extension就会在该Bucket中添加一个新的用户档案。该档案也是判断用户是否有权限进入房间的唯一依据。`security`组中的成员对该Bucket有最高的控制权限。当用户被添加至`security`组或`employee`组时或者从这两个组中被删除时，Server Extension都会修改存储该用户档案的Object。该类Object包含的字段有：`username`, `user_UUID`, `bluetooth_addr`, `authority`, `<groupName>`。其中`<groupName>`字段为两个用户群组的名称，用于表达该用户是否在相应的群组中。
* 另一个Bucket `access_log`用于存放进入房间事件的记录。为了确保该Bucket中的数据安全且不会被篡改(即便安保组长也不应有权限更改记录)，特别设置为门禁系统设备下的Thing域Bucket。每当门禁系统接收到一次来自用户的进入请求时，Server Extension就会依据Bucket `security_profile` 中的档案对此次进入的权限进行判断，然后把结果返回给门禁系统设备，并把此次进入事件添加到`access_log`中。


####权限列表
本应用截止至此已经通过群组以及Bucket域实现了一部分的权限控制：除了应用admin之外，只有`security`组成员可以对Group域Bucket `security_profile`进行数据更改，普通用户无法更改；没有任何用户可以对Bucket `access_log`中的数据进行更改，确保了记录的真实性和安全性。然而仍需要对`access_log`及其中的Object进行权限列表的更改以使得其中Object可以完全被`security`组成员读取，且可以被单个Object所涉及的个人用户所读取。这样，安保组就可以查看所有的进入记录，而每个非安保组的注册用户则可以查看到自己的进入记录。

####推送机制
如上文所述，安全监视器可以接收到每次进入房间事件的推送消息，而这种基于MQTT协议的推送则可以使用Kii Cloud的[Push to App](http://documentation.kii.com/cn/starts/cloudsdk/managing-push-notification/push_kiicloud/push-to-app/)推送机制来实现。只需安全监视器订阅Bucket `access_log`即可，每当该Bucket有新Object被添加时，Kii Cloud都会向安全监视器推送Object被添加的MQTT消息。

综上所述，针对本文引入的应用场景，可以利用Kii Cloud设计出如下图所示的后台结构。
![](http://oi61.tinypic.com/34q1h93.jpg)

##技术实现
为了使本文中的Demo可以更方便地被编译、运行和调试，在此尽可能地减少了对硬件设备的要求，故做出了以下举措。

* 使用现有的[iOS应用](https://itunes.apple.com/us/app/locate-beacon/id738709014?mt=8)作为蓝牙Beacon发射器。通常蓝牙设备的蓝牙地址是唯一的且可以作为该设备的身份标识，本应用拟用蓝牙地址作为代表用户身份的标识并与用户账号绑定。但苹果刻意将蓝牙地址随机化，所以本着Demo的目的，将Beacon信号的Minor/Major值作为身份标识。
* 本Demo将安全监视器设备模拟为可以运行于PC上的命令行程序。其运行的嵌入式代码同样适用于载有Linux系统芯片的硬件设备。
* 本Demo中的门禁系统为安卓应用，除了用于扫描蓝牙Beacon信号以接收房间进入请求外，该安卓应用还可用于用户的注册、登录，群组管理，以及房间进入记录的查询。

下文将详细介绍本Demo中的技术实现方法。
###1.基础搭建
基础搭建是一些非应用的操作，这些操作将为本Demo构建基本的用户，设备，以及群组。以下操作均为REST API调用。注意：开发者在进行如下操作时需要根据各自情况替换以下字段：

* `appID`
* `appKey`
* 安保组长的`loginName`、`password`、`access_token`
* 门禁系统的`access_token`
* `security`组、`employee`组的`groupName`和`groupID`

开发者同样需要记录上述字段以用于安卓、server extension、安全监视器代码的调试部署。

####1.1创建安保组长(Security Lead)用户
#####Request
```
curl -v -X POST \
  -H "content-type:application/vnd.kii.RegistrationRequest+json" \
  -H "x-kii-appid:d521a372" \
  -H "x-kii-appkey:a5b2f02a13efa9d71604106bd598a022" \
  "https://api.kii.com/api/apps/d521a372/users" \
  -d '{"loginName":"leonardean", "password":"123123"}'
```
#####Response
```
{
  "userID" : "bb149fa00022-05f8-4e11-80a7-08204183",
  "internalUserID" : 424994040052713472,
  "loginName" : "leonardean",
  "_hasPassword" : true,
  "_disabled" : false
}
```
####1.1.1获取安保组长的access token，用于后续群组建立
#####Request
```
curl -v -X POST \
  -H "content-type:application/json" \
  -H "x-kii-appid:d521a372" \
  -H "x-kii-appkey:a5b2f02a13efa9d71604106bd598a022" \
  "https://api.kii.com/api/oauth2/token" \
  -d '{"username":"leonardean", "password":"123123"}'
```
#####Response
```
{
  "id" : "bb149fa00022-05f8-4e11-80a7-08204183",
  "access_token" : "qAjEApcDh72u5UvxKPNzDYPvLU-ooXTkcRpfEw424MU",
  "expires_in" : 9223372036854775,
  "token_type" : "Bearer"
}
```
####1.2创建安保组`security`
#####Request
```
curl -v -X POST \
  -H "accept: application/vnd.kii.GroupCreationResponse+json" \
  -H "content-type: application/vnd.kii.GroupCreationRequest+json" \
  -H "Authorization: Bearer qAjEApcDh72u5UvxKPNzDYPvLU-ooXTkcRpfEw424MU" \
  -H "x-kii-appid:d521a372" \
  -H "x-kii-appkey:a5b2f02a13efa9d71604106bd598a022" \
  "https://api.kii.com/api/apps/d521a372/groups" \
  -d '{
        "name" : "security",
        "owner" : "bb149fa00022-05f8-4e11-80a7-08204183"
      }'
```
#####Response
```
{
  "groupID" : "6fya1mdi88fehboc1kvw2nnv7",
  "notFoundUsers" : [ ]
}
```
####1.3创建员工组`employee`
#####Request
```
curl -v -X POST \
  -H "accept: application/vnd.kii.GroupCreationResponse+json" \
  -H "content-type: application/vnd.kii.GroupCreationRequest+json" \
  -H "Authorization: Bearer qAjEApcDh72u5UvxKPNzDYPvLU-ooXTkcRpfEw424MU" \
  -H "x-kii-appid:d521a372" \
  -H "x-kii-appkey:a5b2f02a13efa9d71604106bd598a022" \
  "https://api.kii.com/api/apps/d521a372/groups" \
  -d '{
        "name" : "employee",
        "owner" : "bb149fa00022-05f8-4e11-80a7-08204183"
      }'
```
#####Response
```
{
  "groupID" : "obpm16lvb00qz950scki51km3",
  "notFoundUsers" : [ ]
}
```
####1.4创建设备：门禁系统`access_system`、安全监视器`security monitor`
#####Request
```
curl -v -X POST \
  -H "content-type:application/vnd.kii.ThingRegistrationAndAuthorizationRequest+json" \
  -H "x-kii-appid:d521a372" \
  -H "x-kii-appkey:a5b2f02a13efa9d71604106bd598a022" \
  "https://api.kii.com/api/apps/d521a372/things" \
  -d '{
        "_vendorThingID": "access_system",
        "_password": "123123"
      }'
```
#####Response
```
{
  "_thingID" : "th.c805dfa00022-9bea-4e11-6297-02049543",
  "_vendorThingID" : "access_system",
  "_created" : 1417416572216,
  "_accessToken" : "akWPSVXNtsEX7cNDAjfEEhnD0FemtkLaTw5crEPdYw4"
}
```
####1.5更改Thing域bucket`access_log`的权限列表，使其对`security`组和`employee`组可查询
#####Request
```
curl -v -X PUT \
  -H "Authorization: Bearer akWPSVXNtsEX7cNDAjfEEhnD0FemtkLaTw5crEPdYw4" \
  -H "x-kii-appid:d521a372" \
  -H "x-kii-appkey:a5b2f02a13efa9d71604106bd598a022" \
  "https://api.kii.com/api/apps/d521a372/things/VENDOR_THING_ID:access_system/buckets/access_log/acl/QUERY_OBJECTS_IN_BUCKET/GroupID:6fya1mdi88fehboc1kvw2nnv7"
```
```
curl -v -X PUT \
  -H "Authorization: Bearer akWPSVXNtsEX7cNDAjfEEhnD0FemtkLaTw5crEPdYw4" \
  -H "x-kii-appid:d521a372" \
  -H "x-kii-appkey:a5b2f02a13efa9d71604106bd598a022" \
  "https://api.kii.com/api/apps/d521a372/things/VENDOR_THING_ID:access_system/buckets/access_log/acl/QUERY_OBJECTS_IN_BUCKET/GroupID:obpm16lvb00qz950scki51km3"
```
###2.新用户注册
####2.1安卓应用
当新用户于门禁系统上注册时，需要其打开蓝牙Beacon发射器，将其设备的蓝牙标识与其账号在注册的时候绑定。
#####Java代码
~~~java
KiiUser user;
KiiUser.Builder builder = KiiUser.builderWithName(username);
UserFields userFields = new UserFields();
userFields.set("bluetooth_addr", mBeaconMajorMinor);

user = builder.build();
try {
    user.register(password);
    user.update(userFields);
}catch (Exception e){
    //Exception Handle
}
~~~
####2.2 Server Extension
在新用户注册并绑定其蓝牙标识后，Kii Cloud会通过触发器自动执行指定Server Extension函数，从而将新用户资料添加到Group域Bucket `security_profile`中。
#####Hook.config文件
```
{
  "kiicloud://users": [
    {
      "when": "USER_UPDATED",
      "what": "EXECUTE_SERVER_CODE",
      "endpoint": "add_secure_profile"
    }
  ]
}
```
#####JavaScript代码
~~~javascript
function add_secure_profile(params, context, done) {
  //get admin
  var admin = context.getAppAdminContext();
  //get the newly created user
  var user = admin.userWithID(params.userID);
  //get the security group by hard code and the scope bucket secure_profile
  var security_group = admin.groupWithID(SECURITY_GROUP_ID);
  //get the access profile group scope bucket
  var secure_profile = security_group.bucketWithName("secure_profile");

  //obtain user info
  user.refresh({
    success: function (user) {
      //create a new object in the bucket
      var obj = secure_profile.createObject();
      //set values for the new object in bucket access_profile
      obj.set("username", user.getUsername());
      obj.set("user_UUID", user.getUUID());
      obj.set("authority", false);
      obj.set("bluetooth_addr", user.get("bluetooth_addr"));
      obj.set(SECURITY_GROUP_NAME, false);
      obj.set(EMPLOYEE_GROUP_NAME, false);
      //save the object
      obj.save({
        success: function (theObject) {
          done("Object saved!");
        },
        failure: function (theObject, errorString) {
          done("Error saving object: " + errorString);
        }
      });
    },
    failure: function () {
      done("failed refresh user");
    }
  });
}
~~~
###3.员工组成员管理
####3.1安卓应用
当有新用户注册后，安保组长就可以通过该用户的`username`把该用户添加至安保组`security`或者员工组`employee`，成为公司的一员。
#####Java代码
~~~java
class AddEmployeeToSecurityTask extends AsyncTask<Void, Void, Void>{
    ProgressDialog progressDialog = null;
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(ManageActivity.this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Add to security group");
        progressDialog.show();
    }
    @Override
    protected Void doInBackground(Void... voids) {
        try {
            mAddedUser = KiiUser.findUserByUserName(mAddedUserName);
        }catch (Exception e){
            //Exception Handle
        }
        if(mAddedUser == null){
            return null;
        }
        mSecurityGroup.addUser(mAddedUser);
        try{
            mSecurityGroup.save();
        }catch (Exception e){
            //Exception Handle
        }
        return null;
    }
    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        progressDialog.dismiss();
        new EmployeeListTask().execute();
    }
}
~~~
####3.2 Server Extension
当用户被添加至用户组后即会触发由server extension所设定的`GROUP_MEMBERS_ADDED`事件，并自动执行函数`grant_authority`将该用户在Bucket `security_profile`中的安全档案的`authority`字段变为`true`。则该用户被自动授予房间进入权限。
#####Hook.config文件
```
{
  "kiicloud://groups": [
    {
      "when": "GROUP_MEMBERS_ADDED",
      "what": "EXECUTE_SERVER_CODE",
      "endpoint": "grant_authority"
    }
  ]
}
```
#####JavaScript代码
~~~javascript
function authority_modify(context, params, user, authority, done) {
  var admin = context.getAppAdminContext();
  var clause = KiiClause.equals("bluetooth_addr", user.get("bluetooth_addr"));
  var query = KiiQuery.queryWithClause(clause);
  var security_group = admin.groupWithID(SECURITY_GROUP_ID);
  var secure_profile = security_group.bucketWithName("secure_profile");
  var target_group = admin.groupWithID(params.groupID);

  //query in group scope bucket secure_profile to find the user
  var queryCallbacks = {
    success: function (queryPerformed, resultSet, nextQuery) {
      target_group.refresh({
        success: function (refreshedGroup) {
          var obj = resultSet[0];
          //modify the value of authority
          obj.set("authority", authority);
          //set whether the user is in a certain group
          obj.set(refreshedGroup.getName(), authority);
          obj.save({
            success: function (theObject) {
              done("authority granted/revoked: " + authority);
            },
            failure: function (theObject, errorString) {
              done("failed authority modify: " + errorString);
            }
          })
        },
        failure: function () {
        }
      });

    },
    failure: function (queryPerformed, anErrorString) {
      done("Error querying object: " + anErrorString);
    }
  }
  // Execute the query
  secure_profile.executeQuery(query, queryCallbacks);
}

/*
 ** On user added to group "employee", grant authority by default
 */
function grant_authority(params, context, done) {
  var admin = context.getAppAdminContext();
  //get the newly added user from params
  var user = admin.userWithID(params.members);

  //get user info
  user.refresh({
    success: function (user) {
      //grant authority
      authority_modify(context, params, user, true, done);
    },
    failure: function () {
      done("failed get user info");
    }
  });
}
~~~
同理，也可以由server extension设置当用户被移除出用户组时触发事件`GROUP_MEMBERS_REMOVED`并自动将其权限取消。
###4.蓝牙扫描
####4.1安卓应用
为了确保整个门禁系统的安全性，对用户企图进入房间时的权限判断并非由门禁系统来执行，而是由服务端逻辑函数来执行。当用户使用自己的蓝牙Beacon发射器与门禁系统的距离小于一定阀值时，门禁系统则会主动从客户端请求Kii Cloud执行server extension函数`check_authority`并取得返回值。
#####Java代码
~~~java
class AuthoriseTask extends AsyncTask<Void, Void, Void> {
    ProgressDialog progressDialog = null;
    String returnedValue = null;
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(ScanActivity.this);
        progressDialog.setMessage("Authorising");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }
    @Override
    protected Void doInBackground(Void... voids) {
        // Instantiate with the endpoint.
        KiiServerCodeEntry entry = Kii.serverCodeEntry("check_authority");
        try {
            JSONObject rawArg = new JSONObject();
            // Set the custom parameters.
            rawArg.put("bluetooth_addr", mBeaconMajorMinor);
            KiiServerCodeEntryArgument arg = KiiServerCodeEntryArgument.newArgument(rawArg);
            // Execute the Server Code
            KiiServerCodeExecResult res = entry.execute(arg);
            // Parse the result.
            JSONObject returned = res.getReturnedValue();
            returnedValue = returned.getString("returnedValue");
        } catch (AppException ae) {
            Toast.makeText(ScanActivity.this, ae.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        } catch (IOException ie) {
            Toast.makeText(ScanActivity.this, ie.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        } catch (JSONException je) {
            Toast.makeText(ScanActivity.this, je.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
        return null;
    }
    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        progressDialog.dismiss();
        Toast.makeText(ScanActivity.this, returnedValue, Toast.LENGTH_LONG).show();
    }
}
~~~
####4.2 Sever Extension
server extension函数`check_authority`通过对照该用户在`security_profile`中的档案字段`authority`来判断该用户是否有进入权限，并将判断结果返回给门禁系统。
#####JavaScript代码
~~~javascript
/*
 ** Called by access_system, return the authority of given bluetooth addr
 */
function check_authority(params, context, done) {
  //get the parsed parameter
  var bluetooth_addr = params.bluetooth_addr;
  var admin = context.getAppAdminContext();
  var security_group = admin.groupWithID(SECURITY_GROUP_ID);
  var secure_profile = security_group.bucketWithName("secure_profile");
  //query for the user attempting to access in the secure_profile
  var clause = KiiClause.equals("bluetooth_addr", bluetooth_addr);
  var query = KiiQuery.queryWithClause(clause);

  var queryCallbacks = {
    success: function (queryPerformed, resultSet, nextQuery) {
      var obj = resultSet[0];
      if (obj != undefined) {
        obj.refresh({
          success: function (theObject) {
            //if user found in the secure_profile, insert access log in the thing scope bucket access_log
            insert_access_log(theObject._customInfo, done);
          },
          failure: function (theObject, errorString) {
            done("Error refreshing object: " + errorString);
          }
        });
      } else if (obj == undefined) {
        var theObject = {
          "username": "ANONYMOUS",
          "UUID": "ANONYMOUS",
          "authority": false
        }
        insert_access_log(theObject, done);
      }
    },
    failure: function (queryPerformed, anErrorString) {
      //if user not found in the secure_profile
      done("error in querying object in bucket secure_profile");
    }
  }
  secure_profile.executeQuery(query, queryCallbacks);
}
~~~
###5.添加门禁记录
在上述函数`check_authority`中，当通过字段`bluetooth_addr`查询取得与当前用户所匹配的安全档案后，不论该用户是否有权限，server extension都会把此次进入事件添加到房间进入记录Thing域Bucket `access_log`中。

~~~javascript
function insert_access_log(secure_profile, done) {
  var endpoint = Kii.getBaseURL() + "/apps/" + Kii.getAppID() +
    "/things/VENDOR_THING_ID:access_system/buckets/access_log/objects";
  //insert the access info (the secure profile) into thing scope bucket access_log
  $.ajax({
    type: "POST",
    url: endpoint,
    data: JSON.stringify(secure_profile),
    headers: {
      'Authorization': 'Bearer ' + ACCESS_SYSTEM_TOKEN,
      'X-Kii-AppID': Kii.getAppID(),
      'X-Kii-AppKey': Kii.getAppKey(),
      'Content-Type': 'application/json'
    },
    success: function (data) {
      //modify the acl of the newly added access log
      modify_access_log_acl(data, done, secure_profile);
    },
    error: function (XMLHttpRequest, textStatus, errorThrown) {
      done("Access log insert failed! : " + XMLHttpRequest.status +
      " / " + XMLHttpRequest.responseText + " / " + textStatus + " / " + errorThrown);
    }
  });
}
~~~
###6.更改门禁记录ACL
上文中已提到，为了确保进入记录的准确性和安全性，其数据是保存在门禁系统的Thing域Bucket `access_log`中的，则所以只有门禁系统本身以及应用管理员对该Bucket有查询权限以及所包含的数据Object有读写权限。在基础搭建中，已经将该Bucket的查询权限向用户组`employee`以及`security`开放，但其中所包含的各个数据Object仍然需要权限更改设置。当server extension函数在把进入事件添加到进入记录中后，对这条新添记录进行权限设置，使其可以被用户组`security`和这条记录所涉及的用户个人所读取。
#####JavaScript代码
~~~javascript
function modify_access_log_acl(data, done, secure_profile) {
  var endpoint = Kii.getBaseURL() + "/apps/" + Kii.getAppID() +
    "/things/VENDOR_THING_ID:access_system/buckets/access_log/objects/" +
    data.objectID + "/acl/READ_EXISTING_OBJECT/GroupID:" + SECURITY_GROUP_ID;
  //add READ_EXISTING_OBJECT to security group
  $.ajax({
    type: "PUT",
    url: endpoint,
    headers: {
      'Authorization': 'Bearer ' + ACCESS_SYSTEM_TOKEN,
      'X-Kii-AppID': Kii.getAppID(),
      'X-Kii-AppKey': Kii.getAppKey()
    },
    success: function () {
      //add READ_EXISTING_OBJECT to the user
      var endpoint_user = Kii.getBaseURL() + "/apps/" + Kii.getAppID() +
        "/things/VENDOR_THING_ID:access_system/buckets/access_log/objects/" +
        data.objectID + "/acl/READ_EXISTING_OBJECT/UserID:" + secure_profile.user_UUID;
      $.ajax({
        type: "PUT",
        url: endpoint_user,
        headers: {
          'Authorization': 'Bearer ' + ACCESS_SYSTEM_TOKEN,
          'X-Kii-AppID': Kii.getAppID(),
          'X-Kii-AppKey': Kii.getAppKey()
        },
        success: function () {
          //return authority
          done(secure_profile.authority);
        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
          done(secure_profile.authority + "fail to set obj acl for user due to " + XMLHttpRequest.status +
          " / " + XMLHttpRequest.responseText + " / " + textStatus + " / " + errorThrown + "\n endpoint_user: " + endpoint_user);
        }
      });
    },
    error: function (XMLHttpRequest, textStatus, errorThrown) {
      done("Access log acl modify failed! : " + XMLHttpRequest.status +
      " / " + XMLHttpRequest.responseText + " / " + textStatus + " / " + errorThrown);
    }
  });
}
~~~
###7.MQTT消息推送
本Demo的另一设备安全监视器，担任着接收房间进入事件推送消息的角色。安全监视器订阅Bucket `access_log`，当有新的数据Object被添加到Bucket中时，Kii Cloud便会自动向安全监视器推送MQTT消息。
#####C代码
~~~c
/*****************************************************************************
*
*  kiiPush_subscribeThingBucket
*
*  \param: bucketID - the bucket ID
*
*  \return 0:success; -1: failure
*
*  \brief  Subscribes thing scope bucket
*
*****************************************************************************/
int kiiPush_subscribeThingBucket(char * bucketID)
{
  char * p1;
  char * p2;
  char * buf;

  buf = g_kii_data.sendBuf;
  memset(buf, 0, KII_SEND_BUF_SIZE);
  strcpy(buf, STR_POST);
  // url
  strcpy(buf + strlen(buf), "/api/apps/");
  strcpy(buf + strlen(buf), g_kii_data.appID);
  strcpy(buf + strlen(buf), "/things/VENDOR_THING_ID:");
  strcpy(buf + strlen(buf), g_kii_data.vendorDeviceID);
  strcpy(buf + strlen(buf), "/buckets/");
  strcpy(buf + strlen(buf), bucketID);
  strcpy(buf + strlen(buf), "/filters/all/push/subscriptions/things");
  strcpy(buf + strlen(buf), STR_HTTP);
  strcpy(buf + strlen(buf), STR_CRLF);
  //Connection
  strcpy(buf + strlen(buf), "Connection: Keep-Alive\r\n");
  //Host
  strcpy(buf + strlen(buf), "Host: ");
  strcpy(buf + strlen(buf), g_kii_data.host);
  strcpy(buf + strlen(buf), STR_CRLF);
  //x-kii-appid
  strcpy(buf + strlen(buf), STR_KII_APPID);
  strcpy(buf + strlen(buf), g_kii_data.appID);
  strcpy(buf + strlen(buf), STR_CRLF);
  //x-kii-appkey 
  strcpy(buf + strlen(buf), STR_KII_APPKEY);
  strcpy(buf + strlen(buf), g_kii_data.appKey);
  strcpy(buf + strlen(buf), STR_CRLF);
  //Authorization
  strcpy(buf + strlen(buf), STR_AUTHORIZATION);
  strcpy(buf + strlen(buf), " Bearer ");
  strcpy(buf + strlen(buf), g_kii_data.accessToken);
  strcpy(buf + strlen(buf), STR_CRLF);
  strcpy(buf + strlen(buf), STR_CRLF);

  g_kii_data.sendDataLen = strlen(buf);

  if (kiiHal_transfer() != 0) {
    KII_DEBUG("kii-error: transfer data error !\r\n");
    return -1;
  }
  buf = g_kii_data.rcvdBuf;

  p1 = strstr(buf, "HTTP/1.1 204");
  p2 = strstr(buf, "HTTP/1.1 409");

  if (p1 != NULL || p2 != NULL) {
    return 0;
  }
  else {
    return -1;
  }
}
~~~
在成功订阅Bucket之后，安全监视器需要获取MQTT连接，并做好接收推送消息的准备。
#####C代码
~~~c
/*****************************************************************************
*
*  KiiPush_init
*
*  \param: recvMsgtaskPrio - the priority of task for receiving message
*               pingReqTaskPrio - the priority of task for "PINGREQ" task
*               callback - the call back function for processing the push message received
*
*  \return 0:success; -1: failure
*
*  \brief  Initializes push
*
*****************************************************************************/
int KiiPush_init(unsigned int recvMsgtaskPrio, unsigned int 
	pingReqTaskPrio, kiiPush_recvMsgCallback callback)
{
  kiiPush_endpointState_e endpointState;
  memset(&g_kii_push, 0, sizeof(g_kii_push));
  g_kii_push.mqttSocket = -1;
  if (kiiPush_install() != 0) {
    KII_DEBUG("kii-error: push installation failed !\r\n");
    return -1;
  }  
  do {
    kiiHal_delayMs(1000);
    endpointState = kiiPush_retrieveEndpoint() ;
  }while((endpointState == KIIPUSH_ENDPOINT_UNAVAILABLE));

  if (endpointState == KIIPUSH_ENDPOINT_READY) {
    kiiHal_taskCreate(NULL,
        kiiPush_recvMsgTask,
        (void *)callback,
        (void *)mKiiPush_taskStk,
        KIIPUSH_TASK_STK_SIZE * sizeof(unsigned char), 
        recvMsgtaskPrio);
      
    kiiHal_taskCreate(NULL,
        kiiPush_pingReqTask,
        NULL,
        (void *)mKiiPush_pingReqTaskStk,
        KIIPUSH_PINGREQ_TASK_STK_SIZE * sizeof(unsigned char), 
        pingReqTaskPrio);
    return 0;
  } else {
    return -1;
  }
}
~~~
##其他
本文完整地介绍了Kii Cloud在典型物联网应用场景中的功能和设计/使用方法。读者可以自行从[GitHub](https://github.com/leonardean/Kii-IOT-Demo-Access-System)中下载源代码进行编译和调试研究。该版本库中存有三部分代码，分别是安卓应用(Android studio project)、Server Extension及Hook配置文件、用于模拟安全监视器的命令行C程序。具体编译和调试方法请参见READ.ME文件