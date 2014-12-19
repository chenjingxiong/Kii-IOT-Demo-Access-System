/*
 ** On user creation, add the profile of the new user to group scope bucket
 ** "secure_profile"
 */
var SECURITY_GROUP_ID = "6fya1mdi88fehboc1kvw2nnv7";
var ACCESS_SYSTEM_TOKEN = "akWPSVXNtsEX7cNDAjfEEhnD0FemtkLaTw5crEPdYw4";
var SECURITY_GROUP_NAME = "security";
var EMPLOYEE_GROUP_NAME = "employee";

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

/*
 ** On user removed from group "employee", revode access authority
 */
function revoke_authority(params, context, done) {
  var admin = context.getAppAdminContext();
  //get the removed user from params
  var user = admin.userWithID(params.members);

  user.refresh({
    success: function (user) {
      //revoke authority
      authority_modify(context, params, user, false, done);
    },
    failure: function () {
      done("failed get user info");
    }
  });
}

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
/*
 ** On thing scope bucket access_log is inserted with data, modify the ACL of
 ** objects for both individual employee and security group
 */
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