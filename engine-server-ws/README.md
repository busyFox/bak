# INSTALLATION

## engine-server-ws

engine-server-ws is the game engine server. 
It's a gateway between china-bridge-online and the multiples engines.
It uses redis to cache results of the engines.

engine-server-ws is a Maven project producing a .war application to deploy to Tomcat.


### Environment variables

Add environment variable :

```
ENGINE_SERVER_HOME=<path of engine server project>
```

This path is where the conf-*env* file must be stored along with the other files you'll find in project's root : 
* ArgineConventions.json
* ArgineConventionsCards.json
* dll folder
* install folder
* engine_report.html
* engine_checkAlert.html

**CAUTION** : you may need to add these environment variables in /etc/default/tomcat7 to make sure the tomcat7 user can access these variables.

### Config file

Modify conf-*env*.properties in this project and replace all CHINA_BRIDGE_TODO keys :

```
## Conf for alert mails
general.alert.mail.smtphost=CHINA_BRIDGE_TODO
general.alert.mail.from=CHINA_BRIDGE_TODO
general.alert.mail.recipients=CHINA_BRIDGE_TODO
general.alert.mailEngineNoResult.subject=DEV EngineNoResult
general.alert.mailEngineNoResult.smtphost=CHINA_BRIDGE_TODO
general.alert.mailEngineNoResult.from=CHINA_BRIDGE_TODO
general.alert.mailEngineNoResult.recipients=CHINA_BRIDGE_TODO

user.chinabridge.password=CHINA_BRIDGE_TODO

user.updateDownloadURL=http://CHINA_BRIDGE_TODO/engine/install/install-3.19.exe
user.updateArgineDownloadURL=http://CHINA_BRIDGE_TODO/engine/dll/

queue.sendResult.urlFBSetResult=http://CHINA_BRIDGE_TODO:8080/china-bridge-online/rest/engine/setResult
```

### MongoDB and Redis

As listed in the README_GLOBAL.md file, make sure you have created a chinabridge user able to read/write on all databases.

Modify each resources.properties file in resources-*env* folders to fulfill your needs. The properties in these files are used in ServerBeans.xml to access MongoDB and Redis databases. 
By default MongoDB is configured for a replica set configuration, not standalone databases. This is a good practice but feel free to change it if needed.
By default, in the *dev* environment MongoDB and Redis are hosted locally. Make sure to replace all CHINA_BRIDGE_TODO values for the other environments.

## FBMoteurs.exe

FBMoteurs is the engine program which does the calculation. The "AI" is named Argine.

FBmoteurs.exe is a Windows program. You can also put it on a Linux system and launch it with Wine.

### Installation

- Execute install-3.19.exe located in install directory of engine-server-ws project.
- Copy content engine-server-ws dll directory into Engine folder of FbMoteurs installation
- Modify content in fbMoteurs.ini so it looks **exactly** like this (don't forget to change the URL of your engine-server-ws) :

```
[Cloud]
Threads=10
websocket=1
Verbose=1
Host=ws://CHINA_BRIDGE_TODO:8080/engine-server-ws/wsengine
clientID=engine
ComputePassword=rundll
Sleep=0
```

The clientID and ComputePassword fields can be changed but it has to be changed also in the 
USER SETTINGS part of the config-*env*.properties files

- Execute fbMoteurs.exe

It is recommended to have multiple instances of the engine deployed on servers with performing CPU.