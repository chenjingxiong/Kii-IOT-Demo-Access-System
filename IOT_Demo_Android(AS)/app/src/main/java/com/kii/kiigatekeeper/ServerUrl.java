package com.kii.kiigatekeeper;

import com.kii.cloud.storage.Kii;

/**
 * Created by yue on 14/12/4.
 */
public class ServerUrl {

    public String url = "https://api.kii.com/api/apps/"+Constants.KII_APP_ID+"/things/VENDOR_THING_ID:access_system/buckets/access_log/query";

    public String determineURl(){
        if(Constants.KIIS_SITE == Kii.Site.JP){
            url = "https://api-jp.kii.com/api/apps/"+Constants.KII_APP_ID+"/things/VENDOR_THING_ID:access_system/buckets/access_log/query";
        }else if(Constants.KIIS_SITE == Kii.Site.CN){
            url = "https://api-cn.kii.com/api/apps/"+Constants.KII_APP_ID+"/things/VENDOR_THING_ID:access_system/buckets/access_log/query";
        }

        return url;
    }
}