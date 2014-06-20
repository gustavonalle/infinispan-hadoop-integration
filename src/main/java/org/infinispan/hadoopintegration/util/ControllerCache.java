package org.infinispan.hadoopintegration.util;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.SocketAddress;
import java.util.*;

/**
 * // TODO: Document this
 *
 * @author Pedro Ruivo
 * @since 7.0
 */
public class ControllerCache {

    public static void main(String[] args) throws Exception {
        Map<Argument, String> map = new EnumMap<Argument, String>(Argument.class);
        Queue<String> queue = new LinkedList<String>(Arrays.asList(args));
        Argument.setDefaultValues(map);
        while (!queue.isEmpty()) {
            Argument.parse(queue, map);
        }

        if (map.containsKey(Argument.HELP)) {
            System.out.println("The following arguments are allowed:");
            for (Argument argument : Argument.values()) {
                System.out.println(Argument.help(argument));
            }
            System.exit(0);
        } else if (map.containsKey(Argument.POPULATE)) {
            String filePath = map.get(Argument.FILE);
            if (filePath == null) {
                System.err.println(Argument.FILE.getArg() + " is missing!");
                System.exit(1);
            }

            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("File '" + filePath + "' not found!");
                System.exit(1);
            }
            BufferedReader reader = new BufferedReader(new FileReader(file));

            RemoteCacheManager remoteCacheManager = new RemoteCacheManager(map.get(Argument.HOST),
                    Integer.parseInt(map.get(Argument.PORT)));
            RemoteCache<Integer, String> remoteCache = remoteCacheManager.getCache(map.get(Argument.CACHE_NAME));

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                remoteCache.put(lineNumber++, line);
                if (lineNumber % 100 == 0) {
                    System.out.println("Line " + lineNumber + " added!");
                }
            }

            reader.close();
            remoteCache.stop();
            remoteCacheManager.stop();
            System.exit(0);
        } else if (map.containsKey(Argument.DUMP)) {
            RemoteCacheManager remoteCacheManager = new RemoteCacheManager(map.get(Argument.HOST),
                    Integer.parseInt(map.get(Argument.PORT)));
            RemoteCache<Object, Object> remoteCache = remoteCacheManager.getCache(map.get(Argument.CACHE_NAME));

            Set<SocketAddress> cluster = new HashSet<SocketAddress>();
            for (SocketAddress[] members : remoteCache.getConsistentHash().getSegmentOwners()) {
                cluster.addAll(Arrays.asList(members));
            }
            for (int i = 0; i< cluster.size(); ++i) {
                for (Map.Entry<Object, Object> entry : remoteCache.getBulk().entrySet()) {
                    System.out.println(entry.getKey() + "\t" + entry.getValue());
                }
            }
            remoteCache.stop();
            remoteCacheManager.stop();
            System.exit(0);
        } else if (map.containsKey(Argument.CLEAR)) {
            RemoteCacheManager remoteCacheManager = new RemoteCacheManager(map.get(Argument.HOST),
                    Integer.parseInt(map.get(Argument.PORT)));
            RemoteCache<Object, Object> remoteCache = remoteCacheManager.getCache(map.get(Argument.CACHE_NAME));
            remoteCache.clear();
            remoteCache.stop();
            remoteCacheManager.stop();
            System.exit(0);
        }


    }
}
