import java.util.*;
import redis.clients.jedis.Jedis;

enum EQuery {
    PATIENTS(0), STUDIES(1), DICOMS(2), AUTHENTICATE(3);    

    private int value;
    private static Map map = new HashMap<>();

    private EQuery(int value) {
        this.value = value;
    }

    static {
        for (EQuery eQuery : EQuery.values()) {
            map.put(eQuery.value, eQuery);
        }
    }

    public static EQuery valueOf(int eQuery) {
        return (EQuery) map.get(eQuery);
    }

    public int getValue() {
        return value;
    }
};

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
            System.out.println("EQuery of AUTHENTICATE key: " + EQuery.AUTHENTICATE.toString());
            System.out.println("EQuery of AUTHENTICATE key: " + EQuery.AUTHENTICATE.ordinal());
            System.out.println("EQuery of PATIENTS key: " + EQuery.valueOf(2));
        } catch (ClassNotFoundException e) {
            System.err.println(e);
            System.exit(-1);
        }
    }
}