package com.funbridge.server.ws.event;

public class EventField implements Cloneable {
    public String name;
    public String value;
    public String data;

    public EventField() {
    }

    public EventField(String n, String v, String d) {
        this.name = n;
        this.value = v;
        this.data = d;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return "(" + name + ", " + value + ", " + data + ")";
    }


}
