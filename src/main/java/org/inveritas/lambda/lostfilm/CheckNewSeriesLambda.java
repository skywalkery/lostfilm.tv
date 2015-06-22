package org.inveritas.lambda.lostfilm;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CheckNewSeriesLambda implements RequestHandler<SNSEvent, String> {
    private static final String RSS_LINK = "http://www.lostfilm.tv/rssdd.xml";
    private static final String BROWSE_LINK = "http://www.lostfilm.tv/browse.php";

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public String handleRequest(SNSEvent snsEvent, Context context) {
        for (SNSEvent.SNSRecord record : snsEvent.getRecords()) {
            try {
                ChimeMessage msg = mapper.readValue(record.getSNS().getMessage(), ChimeMessage.class);
                if (!msg.getMinute().equals("00")) {
                    return "ok";
                }

                CompletableFuture<ZonedDateTime> amazonDateTimePromise = getLastBuildDateFromAmazon();
                HttpClient httpClient = HttpClientFactory.getDefaultClient();
                InputStreamResponseListener listener = new InputStreamResponseListener();
                httpClient.newRequest(RSS_LINK).send(listener);
                Response response = listener.get(5, TimeUnit.SECONDS);
                if (response.getStatus() == HttpStatus.OK_200) {
                    XMLStreamReader xmlp = null;
                    try (InputStream is = listener.getInputStream()) {
                        XMLInputFactory xmlFactory = XMLInputFactory.newFactory();
                        xmlp = xmlFactory.createXMLStreamReader(is, "windows-1251");
                        ZonedDateTime dateTime = getLastBuildDate(xmlp);
                        ZonedDateTime amazonDateTime = amazonDateTimePromise.get(5, TimeUnit.SECONDS);
                        if (amazonDateTime == null) {
                            saveLastDate(dateTime);
                            amazonDateTime = dateTime;
                        }
                        if (!dateTime.isAfter(amazonDateTime)) {
                            return "ok";
                        }

                        CompletableFuture<Document> browsePagePromise = getLostfilmBrowsePage();
                        List<Series> newSeries = new ArrayList<>();
                        Optional<Series> nextSeries;
                        while ((nextSeries = getNextSeries(xmlp, amazonDateTime)).isPresent()) {
                            newSeries.add(nextSeries.get());
                        }
                        newSeries = cleanFromDuplicates(newSeries);
                        setPictures(newSeries, browsePagePromise.get(2, TimeUnit.SECONDS));
                    } finally {
                        if (xmlp != null) {
                            xmlp.close();
                        }
                    }
                }
            } catch (Exception e) {
                context.getLogger().log(e.getMessage());
            }
        }
        return "ok";
    }

    private ZonedDateTime getLastBuildDate(XMLStreamReader xmlp) throws XMLStreamException {
        while (xmlp.hasNext()) {
            xmlp.next();
            if (xmlp.isStartElement() && xmlp.getName().toString().equals("lastBuildDate")) {
                String lastDate = xmlp.getElementText();
                return ZonedDateTime.parse(lastDate, DateTimeFormatter.RFC_1123_DATE_TIME);
            }
        }
        throw new XMLStreamException("There is no lastBuildDate");
    }

    private CompletableFuture<ZonedDateTime> getLastBuildDateFromAmazon() throws IOException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AmazonS3 s3Client = AmazonS3ClientFactory.getInstance().getClient();
                S3Object s3Object;
                try {
                    s3Object = s3Client.getObject(new GetObjectRequest("lostfilm", "lastBuildDate"));
                    try (S3ObjectInputStream s3is = s3Object.getObjectContent();
                         Scanner s = new Scanner(s3is)) {
                        String savedDateTimeStr = s.useDelimiter("\\A").hasNext() ? s.next() : "";
                        savedDateTimeStr = "Sun, 21 Jun 2015 17:07:38 +0000";
                        return ZonedDateTime.parse(savedDateTimeStr, DateTimeFormatter.RFC_1123_DATE_TIME);
                    }
                } catch (AmazonS3Exception e) {
                    if (e.getStatusCode() == 404) {
                        return null;
                    }
                    throw e;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    private void saveLastDate(ZonedDateTime lastDate) {
        AmazonS3 s3Client = AmazonS3ClientFactory.getInstance().getClient();
        s3Client.putObject(new PutObjectRequest("lostfilm",
                "lastBuildDate",
                new ByteArrayInputStream(lastDate.format(DateTimeFormatter.RFC_1123_DATE_TIME).getBytes(StandardCharsets.UTF_8)),
                null));
    }

    private Optional<Series> getNextSeries(XMLStreamReader xmlp, ZonedDateTime fromDate) throws XMLStreamException {
        Series series = new Series();
        while (xmlp.hasNext()) {
            xmlp.next();
            if (xmlp.isStartElement() && xmlp.getName().toString().equals("title")) {
                series.setTitle(xmlp.getElementText().replace(" [MP4]", "").replace(" [1080p]", ""));
            } else if (xmlp.isStartElement() && xmlp.getName().toString().equals("pubDate")) {
                String pubDate = xmlp.getElementText();
                ZonedDateTime pubDateTime = ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME);
                if (pubDateTime.isAfter(fromDate)) {
                    return Optional.of(series);
                } else {
                    break;
                }
            }
        }
        return Optional.empty();
    }

    private List<Series> cleanFromDuplicates(List<Series> newSeries) {
        Series current = null;
        List<Series> clean = new ArrayList<>();
        for (Series series : newSeries) {
            if (current != null) {
                String currentName = current.getTitle().substring(0, current.getTitle().indexOf(").") + 2);
                String seriesName = series.getTitle().substring(0, series.getTitle().indexOf(").") + 2);
                if (currentName.equals(seriesName)) {
                    continue;
                }
            }
            current = series;
            clean.add(series);
        }
        return clean;
    }

    private CompletableFuture<Document> getLostfilmBrowsePage() {
        return CompletableFuture.supplyAsync(() -> {
            HttpClient httpClient = HttpClientFactory.getDefaultClient();
            InputStreamResponseListener listener = new InputStreamResponseListener();
            httpClient.newRequest(BROWSE_LINK).send(listener);
            return listener;
        }).thenApplyAsync(inputStreamResponseListener -> {
            try (InputStream is = inputStreamResponseListener.getInputStream()) {
                return Jsoup.parse(is, "windows-1251", "http://lostfilm.tv");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    private void setPictures(List<Series> seriesList, Document browsePage) {
        Elements images = browsePage.getElementsByClass("category_icon");
        for (Series series : seriesList) {
            String name = series.getTitle().substring(0, series.getTitle().indexOf(" ("));
            for (Element img : images) {
                if (img.attr("title") != null && img.attr("title").equals(name)) {
                    series.setImg("http://www.lostfilm.tv" + img.attr("src"));
                    break;
                }
            }
        }
    }
}
