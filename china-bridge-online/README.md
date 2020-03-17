# INSTALLATION

## china-bridge-online

china-bridge-online is the core of the project. It manages the players, the game table, the games modes...
Players and transactions are stored in MySQL database while all game modes and other stuffs are stored in MongoDB database.
It connects to engine-server-ws to make CPU play.

china-bridge-online is a Maven project producing a .war application to deploy to Tomcat.

### Environment variables

Add environment variable :

```
CHINA_BRIDGE_HOME=<path of the project>
```

This path is where the conf-*env* file must be stored along with the other files you'll find in project's root : 
* message.properties
* .dat files
* GenTrainingPartner.ini
* texts folder
* argine folder
* learningTour folder

Also add an environment variable for the log folder :

```
SERVER_LOG=<path of the log folder>
```

**CAUTION** : you may need to add these environment variables in /etc/default/tomcat7 to make sure the tomcat7 user can access these variables.

### Config file

The conf-*env*.properties file is where various properties go in order to be easily changed without having to restart the server.
Modify it and replace all *CHINA_BRIDGE_TODO* keys with appropriate values :

```
# Conf for alert mails
general.alert.mail.smtphost=CHINA_BRIDGE_TODO
general.alert.mail.from=CHINA_BRIDGE_TODO
general.alert.mail.recipients=CHINA_BRIDGE_TODO

# Conf tournaments Funbridge Points
tournament.TOUR_PF.reportFile.recipients=CHINA_BRIDGE_TODO # mail recipient for report
tournament.TOUR_PF.federationURL=CHINA_BRIDGE_TODO

# Conf Engine Server
enginerest.host=CHINA_BRIDGE_TODO # host where engine-server-ws is installed
enginerest.serviceWebsocket=ws://CHINA_BRIDGE_TODO:8080/engine-server-ws/wsfbserver # URL of engine-server-ws
enginerest.serviceWebsocketClientID=CHINA_BRIDGE_TODO
enginerest.password=CHINA_BRIDGE_TODO # password of the user defined in engine-server-ws conf-env.properties
enginerest.urlSetResult=http://CHINA_BRIDGE_TODO:8080/china-bridge-online/rest/engine/setResult # URL used by engine server to send result to china-bridge-online
```

### MySQL database

As listed in the README_GLOBAL.md file, make sure you have created a chinabridge database, synchronized the database model and set a chinabridge user.

Modify each persistence.xml file in resources-*env*/META-INF folders to fulfill your needs. By default, in the *dev* environment the MySQL server is hosted on localhost. 
Make sure to replace all CHINA_BRIDGE_TODO values for the other environments and at least the user's password in the *prod* environment.

``` xml
<property name="hibernate.connection.username" value="chinabridge"/>
<property name="hibernate.connection.password" value="chinabridge"/>
<property name="hibernate.connection.url" value="jdbc:mysql://CHINA_BRIDGE_TODO/chinabridge?autoReconnect=true"/>
```

### MongoDB database

As listed in the README_GLOBAL.md file, make sure you have created a chinabridge user able to read/write on all databases.

Modify each resources.properties file in resources-*env* folders to fulfill your needs. The properties in these files are used in ServerBeans.xml to access MongoDB databases. 
By default it is configured for a replica set configuration, not standalone databases. This is a good practice but feel free to change it if needed.
By default, in the *dev* environment MongoDB is hosted locally. Make sure to replace all CHINA_BRIDGE_TODO values for the other environments


