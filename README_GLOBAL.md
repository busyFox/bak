# INSTALLATION

## Prerequisites

### Technos
* Java 8
* Maven
* MySQL 5.7
* MongoDB 3.4
* Tomcat 7
* Nginx
* Redis

### GOTO Games java projects
* gotogames-common 1.2.26 (jar dependency)
* bridge-common 1.3.7 (jar dependency)
* china-bridge-online (war)
* engine-server-ws (war)
* FBMoteurs.exe

## Configuration

### MySQL

This project contains a chinabridge.mwb model you can open in MySQL Workbench in order to synchronize your database to match model.
If you prefer you can also use the database.sql script to initialize the schema.

Create user chinabridge with password chinabridge (config in resources-*env*/META-INF/persistence.xml if you want to change it) and give it access to chinabridge database.

### MongoDB

Create user chinabridge with password chinabridge with access on all databases 
(config in resources.properties if you want to change it).

### Tomcat

Update max opened files limit in production. Add at the end of /etc/default/tomcat7 file :
> ulimit -n 8192

### Nginx

Add location in conf file for the main route to china-bridge-online (don't forget to add future services to this route) :

``` json
location ~ ^/china-bridge-online/rest/(event|game|player|presence|result|store|tournament|team|message)/ {
    proxy_pass http://localhost:8080;
    proxy_set_header Host $host;
}
```


Add location in conf file for the avatar management (change the path of the project) :

``` json
location /china-bridge-player/ {
    # same directory as player.file.path in conf-env.properties
    alias <path of the project>/player/; 
}
```

Don't forget to create this new folder who will host the players' avatars.

## GOTO Games java projects

For GOTO Games projects documentation, please read each project's README.md.
It may be a good idea to begin with china-bridge-online and then move on to engine-server-ws, but not mandatory.

bridge-common and gotogames-common don't have readme files as they are just simple dependencies that should not be modified or configured.