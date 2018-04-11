import java.util.*;
import redis.clients.jedis.Jedis;
public class ExaRedis {
    static Jedis redis;
    public static void main(String[] argv) {
        try {
            Class.forName("org.postgresql.Driver");
            redis = new Jedis("127.0.0.1");
            redis.flushAll();
            redis.lpush("tutorial-list", "Redis");
            redis.lpush("tutorial-list", "Mongodb");
            redis.lpush("tutorial-list", "Mysql");
            List<String> list = redis.lrange("tutorial-list", 0, 5);
            //List<String> list = redis.keys("*");
            for (int i = 0; i < list.size(); i++) {
                System.out.println("List of stored keys:: " + list.get(i));
            }
        } catch (ClassNotFoundException e) {
            System.err.println(e);
            System.exit(-1);
        }
    }
}