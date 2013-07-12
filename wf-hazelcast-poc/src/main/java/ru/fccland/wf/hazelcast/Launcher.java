package ru.fccland.wf.hazelcast;

import ru.fccland.wf.ws.request.RequestService;
import ru.fccland.wf.ws.usersession.UserSessionService;

import java.util.Arrays;

/**
 * Created by void 14.03.13 12:37
 *
 * java -jar target/wf-hazelcast-0.1.0-SNAPSHOT.jar UserSession server
 * java -Ddw.http.port=8090 -Ddw.http.adminPort=8090 -jar target/wf-hazelcast-0.1.0-SNAPSHOT.jar Request server
 */
public class Launcher {
    public static void main(String[] args) throws Exception {
		if ("UserSession".equals(args[0])) {
			new UserSessionService().run(Arrays.copyOfRange(args, 1, args.length));
		} else if ("Request".equals(args[0])) {
			new RequestService().run(Arrays.copyOfRange(args, 1, args.length));
		} else {
			System.err.println("Unknown service: "+ args[0]);
		}

    }
}
