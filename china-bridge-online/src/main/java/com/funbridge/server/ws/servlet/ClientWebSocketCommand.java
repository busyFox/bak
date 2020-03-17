package com.funbridge.server.ws.servlet;

public class ClientWebSocketCommand {
    public String service;
    public String command;
    public String parameters;
    public String id;

    public String toString() {
        return "service="+service+" - command="+command+" - parameters="+parameters+" - id="+id;
    }
}
