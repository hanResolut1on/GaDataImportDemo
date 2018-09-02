import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by hanchristmas on 2018/9/1.
 */
public class RedisDemo {

    public static Jedis jedis = new Jedis("localhost");

    private static boolean execute = true;

    public static void main(String[] args) {
//        Thread tast1 = new Thread(new TaskThread1());
//        Thread task2 = new Thread(new TaskThread2());
//        tast1.start();
//        task2.start();
    }

    private static void scheduledTask() {
        while (execute) {
            long page = jedis.incr("counter");
            if (page == -999L) {
                break;
            }
            processPageData(page);
        }
    }

    private static void processPageData(long page) {
        JsfProductResult result = queryProductInfo((int) page);
        if (result.getCode() == -1) {
            //query data from jsf error
            jedis.lpush("failedList", String.valueOf(page));
        } else if (result.getCode() == 1) {
            //all paged queried once, set the counter, then iterate failedList
            jedis.set("counter", "-999");

            while (true) {
                String failedValue = jedis.lpop("failedList");
                if (failedValue == null) {
                    execute = false;
                    break;
                }
                long failedPage = Long.valueOf(failedValue);
                processPageData(failedPage);
            }
        } else if (result.getCode() == 0) {
            //got data from jsf
            try {
                queryRelatedInfo(result);
                populateData2Csv(result.getProductInfo());
                uploadDataCsv("data.csv");
            } catch (IOException e) {
                jedis.lpush("failedList", String.valueOf(page));
            }
        }
    }

    private static JsfProductResult queryProductInfo(int page) {
        JsfProductResult result = new JsfProductResult();
        return result;
    }

    private static void queryRelatedInfo(JsfProductResult jsfProductResult) {
        //set product properties
    }

    private static int populateData2Csv(Object productInfo) throws IOException {
        File file = new File("data.csv");
        FileWriter fileWriter = new FileWriter(file, false);
        try {
            fileWriter.write("productName,brandName,categoryName\r\n");
            fileWriter.write("sku01,brand01,category01\r\n");
        } catch (IOException e) {
            return -1;
        } finally {
            fileWriter.close();
        }

        return 0;
    }

    private static void uploadDataCsv(String csvFile) {

    }

}


class TaskThread1 implements Runnable {

    Jedis jedis = new Jedis("localhost");

    public void run() {
        while (true) {
            Long counter = jedis.incr("counter");
            System.out.println("task1: " + counter);
        }
    }
}

class TaskThread2 implements Runnable {

    Jedis jedis = new Jedis("localhost");

    public void run() {
        while (true) {
            Long counter = jedis.incr("counter");
            System.out.println("task2: " + counter);
        }
    }
}
