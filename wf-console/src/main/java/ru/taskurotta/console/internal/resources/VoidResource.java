package ru.taskurotta.console.internal.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * VoidResource for test
 * User: dimadin
 * Date: 17.05.13 17:09
 */
@Path("/hello")
public class VoidResource {

    @GET
    public String doVoid() {
        return "Hello!";
    }

}
