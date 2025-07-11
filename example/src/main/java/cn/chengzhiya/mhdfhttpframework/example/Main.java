package cn.chengzhiya.mhdfhttpframework.example;

import cn.chengzhiya.mhdfhttpframework.client.HttpClient;
import cn.chengzhiya.mhdfhttpframework.client.exception.ConnectionException;
import cn.chengzhiya.mhdfhttpframework.client.exception.RequestException;
import cn.chengzhiya.mhdfhttpframework.client.exception.URLException;
import cn.chengzhiya.mhdfhttpframework.server.HttpServer;
import cn.chengzhiya.mhdfhttpframework.server.entity.SSLConfig;
import cn.chengzhiya.mhdfhttpframework.server.enums.ServerStatus;

public class Main {
    public static void main(String[] args) {
        HttpServer server = new HttpServer(8080, new SSLConfig());
        server.start();

        while (true) {
            System.out.println(server.getStatus());
            if (server.getStatus() == ServerStatus.RUNNING) {
                HttpClient client = new HttpClient();
                try {
                    System.out.println(client.get("http://127.0.0.1:8080/hello"));
                } catch (RequestException | ConnectionException | URLException e) {
                    throw new RuntimeException(e);
                }

                System.exit(0);
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
