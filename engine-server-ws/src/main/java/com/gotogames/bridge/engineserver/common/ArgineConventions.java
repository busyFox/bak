package com.gotogames.bridge.engineserver.common;

import java.util.List;

public class ArgineConventions {
    public List<ArgineConvention> profiles;

    public ArgineConvention getConvention(int id) {
        if (profiles != null) {
            for (ArgineConvention c : profiles) {
                if (c.id == id) {
                    return c;
                }
            }
        }
        return null;
    }
}
