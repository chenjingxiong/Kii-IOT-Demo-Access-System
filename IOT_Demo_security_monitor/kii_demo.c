#include <string.h>
#include <stdio.h>
#include<stdlib.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <netinet/if_ether.h>
#include <net/if.h>
//#include <linux/sockios.h>
#include <ctype.h>  


#include "kii.h"
#include "kii_demo.h"

volatile int toStop = 0;

void kiiDemo_callback(char* jsonBuf, int rcvdCounter)
{
    char * p1;
    char * p2;

    char objectID[KII_OBJECTID_SIZE+1];
    char bucketName[KII_BUCKET_NAME_SIZE+1];
    char jsonObject[512];
    char userName[256];
    char authority[10];

    p1 = strstr(jsonBuf, "objectID");
    if (p1 != NULL)
    {
        p1 +=8+3;
        p2 = strstr(p1, "\"");
        memset(objectID, 0 ,sizeof(objectID));
        memcpy(objectID, p1, p2-p1);
    }

    p1 = strstr(jsonBuf, "bucketID");
    if (p1 != NULL)
    {
        p1 +=8+3;
        p2 = strstr(p1, "\"");
        memset(bucketName, 0 ,sizeof(bucketName));
        memcpy(bucketName, p1, p2-p1);
    }

    p1 = strstr(jsonBuf, "\"type\":\"DATA_OBJECT_CREATED\"");
    if (p1 != NULL)
    {
	//retrieve object
	memset(jsonObject, 0, sizeof(jsonObject));
	if (kiiObj_retrieve(bucketName, objectID, jsonObject, sizeof(jsonObject)) < 0)
	{
		printf("Retrieve object failed, objectID:\"%s\"\r\n", objectID);
	}
	else
	{
            //printf("\r\n%s\r\n",jsonObject);
            p1 = strstr(jsonObject, "\"username\":\"");
	     if (p1 == NULL)
	     {
	         printf("invalid username\r\n");
	         return;
	     }
	     p1 +=12;
	     p2 = strstr(p1, "\"");
	     if (p2 == NULL)
	     {
	         printf("invalid username\r\n");
	         return;
	     }
	     memset(userName, 0, sizeof(userName));
	     memcpy(userName, p1, p2-p1);
	     p1 = strstr(jsonObject, "\"authority\":");
	     if (p1 == NULL)
	     {
	         printf("invalid authority\r\n");
	         return;
	     }
	     p1 +=12;
	     p2 = strstr(p1, "\"");
	     if (p2 == NULL)
	     {
	         printf("invalid authority\r\n");
	         return;
	     }
	     memset(authority, 0, sizeof(authority));
	     memcpy(authority, p1, p2-p1);
            printf("\r\n");
            printf("%s", userName);
            printf("  ---------> ");
	     if (strcmp(authority, "true,") == 0)
	     {
                printf("authorized");
	     }
	     else
	     {
                printf("unauthorized");
	     }
            printf("\r\n");
	     fflush(stdout);
	}
    }
}


int kiiDemo_initPush(void)
{
    if (kiiPush_subscribeThingBucket(STR_BUCKET_NAME) < 0)
    {
	return -1;
    }
	
    if (KiiPush_init(0, 0, kiiDemo_callback) < 0)
    {
	return -1;
    }
    else
    {
	return 0;
    }
}

#if 0
int kiiDemo_getMacAddr(char *device, char *mac_addr)
{
    struct ifreq req;
    int socketNum;
    int err;
    int i;
	 
    socketNum = socket(AF_INET,SOCK_STREAM,0);
    if (socketNum < 0)
    {
        return -1;
    }
    strcpy(req.ifr_name,device);
    if (ioctl(socketNum,SIOCGIFHWADDR,&req) < 0)
    {
        close(socketNum);
        return -1;
    }
    else
    {
        close(socketNum);
	for(i=0; i<6; i++)
	{
		sprintf(mac_addr+strlen(mac_addr), "%02x", (unsigned	char)req.ifr_hwaddr.sa_data[i]);
	}
        return 0;    
    }
}
#endif

int kiiDemo_onBoarding(void)
{
    if (kiiDev_getToken(STR_DEVICE_VENDOR_ID, STR_DEVICE_PASSWORD) != 0)
    {
        if (kiiDev_register(STR_DEVICE_VENDOR_ID, STR_DEVICE_TYPE, STR_DEVICE_PASSWORD) != 0)
        {
            printf("Onboarding failed\r\n");
	    return -1;
        }
	else
	{
            printf("Register thing success\r\n");
	    return 0;
	}
    }
    else
    {
        printf("Get thing token success\r\n");
	return 0;
    }
}


void cfinish(int sig)
{
	signal(SIGINT, NULL);
	toStop = 1;
}


int main(int argc, char** argv)
{
	signal(SIGINT, cfinish);
	signal(SIGTERM, cfinish);

    if (kii_init(STR_SITE, STR_APPID, STR_APPKEY) < 0)
    {
       printf("Initialize kii failed\r\n");
       return 0;
    }

    if (kiiDemo_onBoarding() != 0)
    {
    	    printf("Device onbording failed\r\n");
	    return 0;
    }

    if (kiiDemo_initPush() < 0)
    {
        printf("Initialize push failed\r\n");
    }
    else
    {
	printf("Initialize push success\r\n");
    }

    while (!toStop)
    {
        sleep(1);	
    }

    return 0;
}


