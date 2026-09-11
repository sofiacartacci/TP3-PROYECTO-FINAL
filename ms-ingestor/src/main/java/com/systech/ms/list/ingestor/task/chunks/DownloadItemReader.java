package com.systech.ms.list.ingestor.task.chunks;

import java.io.BufferedInputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DownloadItemReader<T> implements ItemReader<byte[]> {

    @Value("${mapping.src-url}")
    private String src;

    @Value("${download.timeout}")
    private int timeout;

    private String user, pass, url;

    @Value("${download.buffersize:65536}")
    private int commitcount;

    @Value("${mapping.name}")
    String name;

    byte data[];
    int count;
    long totalRead;
    long totalAvailable;
    int cc;
    long reads;

    BufferedInputStream in;

    boolean inited = false;

    private void init() throws Exception {
        totalRead = 0;
        count = 0;
        log.info("Downloading provider: " + name);

        if (src.toLowerCase().startsWith("https://") && src.contains("@")) { // es https y requiere autenticaci?n
            String tmp = src.replace("https://", "");
            tmp = tmp.substring(0, tmp.indexOf("@"));
            String[] userAndPass = tmp.split(":");
            user = userAndPass[0];
            pass = userAndPass[1];
            url = src.replace(user + ":" + pass + "@", "");

            Authenticator au = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass.toCharArray());
                }
            };
            Authenticator.setDefault(au);
        } else {
            url = src;
        }

        if (commitcount < Integer.MAX_VALUE) {
            cc = Integer.parseInt(String.valueOf(commitcount));
        }

        connect();
        inited = true;
    }

    private void connect() throws Exception {
        URLConnection urlc = new URL(url).openConnection();
        urlc.setConnectTimeout(timeout);
        urlc.setReadTimeout(timeout);
        in = new java.io.BufferedInputStream(urlc.getInputStream());

        totalAvailable = in.available();
    }

    @Override
    public byte[] read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!inited) {
            init();
        }
        if ((count = in.read(data = new byte[cc], 0, cc)) >= 0) {
            totalRead = totalRead + count;

            if (reads++ % 1000 == 0) {
                log.info("Total bytes leidos: " + totalRead);
            } else {
                log.debug("Bytes leidos: " + count);
            }

            return Arrays.copyOf(data, count);
        } else {
            in.close();

            return null;
        }

    }

    @AfterStep
    public void close() throws Exception {
        inited = false;
    }

}
