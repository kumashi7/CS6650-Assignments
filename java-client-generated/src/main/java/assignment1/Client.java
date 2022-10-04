package assignment1;

import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import static assignment1.Constants.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.DoubleStream;

/**
 * Client for Assignment 1
 */
public class Client {
    public static LinkedBlockingQueue<SkierPostRequest> requestObjList = new LinkedBlockingQueue<>();
    private static final LinkedBlockingQueue<LatencyInfo> latencies = new LinkedBlockingQueue<>();

    private static final LinkedBlockingQueue<LatencyInfo> postProcessing = new LinkedBlockingQueue<>();

    public static boolean csvWriting = true;

    /**
     * Client main method
     * @param args command-line arguments
     * @throws InterruptedException for thread exceptions
     * @throws ExecutionException for executor exceptions
     */
    public static void main(String args[]) throws InterruptedException, ExecutionException {
        long startTime = System.nanoTime();
        final Client client = new Client();

        // Sending 32000 posts
        // with 32 threads and 1000 posts each
        ExecutorService executorService = Executors.newFixedThreadPool(NUMOFTHREADS + 1);
        Callable<Integer> callable = () -> {
            ThreadLocal<Integer> localPostCounter = new ThreadLocal<>();
            localPostCounter.set(0);
            client.sendRequests(localPostCounter);
            return localPostCounter.get();
        };
        // Generate 200k request objects
        // in 1 dedicated thread
        Callable<Integer> generator = () -> {
            client.generateReqObj();
            return -1;
        };
        // Write results to CSV
        Runnable csvWriter = () -> {
            try {
                File tmp = new File(CSV_FILE_NAME);
                tmp.createNewFile();
                client.writeToCsv();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        Thread t = new Thread(csvWriter);
        t.start();
        // Add callables to array list and initiate
        List<Callable<Integer>> callables = new ArrayList<>();
        for (int i = 0; i < NUMOFTHREADS; i++) {
            callables.add(callable);
        }
        callables.add(generator);
        List<Future<Integer>> result = executorService.invokeAll(callables);
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        // Sending 168000 posts
        // with 168 threads and 1000 posts each
        ExecutorService restService = Executors.newFixedThreadPool(NUMOFRESTTHREADS);
        Callable<Integer> restCallable = () -> {
            ThreadLocal<Integer> localPostCounter = new ThreadLocal<>();
            localPostCounter.set(0);
            client.sendRequests(localPostCounter);
            return localPostCounter.get();
        };
        List<Callable<Integer>> newCallables = new ArrayList<>();
        for (int i = 0; i < NUMOFRESTTHREADS; i++) {
            newCallables.add(restCallable);
        }
        result.addAll(restService.invokeAll((newCallables)));
        restService.shutdown();
        restService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        // Start analysis and terminate threads
        long endTime = System.nanoTime();
        double durationInSec = (endTime - startTime) / DURATION_BASE;
        int totalNumSuccess = 0;
        for (Future<Integer> success : result) {
            if (success.get() != -1) {
                totalNumSuccess += success.get();
            }
        }
        int totalNumFail = NUMOFOBJECTS - totalNumSuccess;
        System.out.println("totalNumSuccess: " + totalNumSuccess);
        System.out.println("totalNumFail: " + totalNumFail);
        System.out.println("durationInSec: " + durationInSec);
        System.out.println("throughput(rps): " + (totalNumSuccess + totalNumFail) / durationInSec);
        while (!latencies.isEmpty()) {
           csvWriting = true;
        }
        csvWriting = false;
        t.join();
        client.printAnalysis();
    }

    /**
     * Print analysis data to terminal
     */
    private void printAnalysis() {
        DoubleStream latencies = postProcessing.stream().mapToDouble(LatencyInfo::getLatency);
        System.out.println("mean: " +
                latencies.average().orElse(Double.NaN));

        DoubleStream sortedLatency = postProcessing.stream().mapToDouble(LatencyInfo::getLatency).sorted();
        double median = postProcessing.size()%2 == 0?
                sortedLatency.skip(postProcessing.size()/2-1).limit(2).average().getAsDouble():
                sortedLatency.skip(postProcessing.size()/2).findFirst().getAsDouble();

        System.out.println("median: " + median);
        sortedLatency = postProcessing.stream().mapToDouble(LatencyInfo::getLatency).sorted();
        System.out.println("max: " + sortedLatency.max().getAsDouble());
        sortedLatency = postProcessing.stream().mapToDouble(LatencyInfo::getLatency).sorted();
        System.out.println("min: " + sortedLatency.min().getAsDouble());
        sortedLatency = postProcessing.stream().mapToDouble(LatencyInfo::getLatency).sorted();
        System.out.println("throughput: " + postProcessing.size() / (sortedLatency.sum() / 1000));
        sortedLatency = postProcessing.stream().mapToDouble(LatencyInfo::getLatency).sorted();
        int pos = (int) (postProcessing.size() * 0.99);
        System.out.println("p99: " + sortedLatency.toArray()[pos]);
    }

    /**
     * Write analysis to CSV file
     * @throws FileNotFoundException if file name can not be found
     */
    private void writeToCsv() throws FileNotFoundException {
        File csvOutputFile = new File(CSV_FILE_NAME);
        while (csvWriting) {
            LatencyInfo latencyInfo = latencies.poll();
            if (latencyInfo == null) {
                continue;
            }
            postProcessing.add(latencyInfo);
            List<String[]> dataLines = new ArrayList<>();
            dataLines.add(new String[]
                    {"" + latencyInfo.startTime, latencyInfo.requestType,
                            "" + latencyInfo.latency, "" + latencyInfo.responseCode});

            try (PrintWriter pw = new PrintWriter(new FileOutputStream(csvOutputFile,true))) {
                dataLines.stream()
                        .map(this::convertToCSV)
                        .forEach(pw::println);
            }
        }

    }

    /**
     * Convert array of strings to one string
     * @param data array of strings
     * @return String
     */
    public String convertToCSV(String[] data) {
        return String.join(",", data);
    }

    /**
     * Generate 200k request objects
     */
    protected void generateReqObj() {
        for (int i = 0; i < NUMOFOBJECTS; i++) {
            SkierPostRequest skierPostRequest = new SkierPostRequest();
            requestObjList.add(skierPostRequest);
        }
    }

    /**
     * Send 1000 request
     * @param localPostCounter local thread counter
     */
    protected void sendRequests(ThreadLocal<Integer> localPostCounter) {
        Timer timer = new Timer();
        int responseCode = 200;
        for (int i = 0; i < NUMOFPOSTS; i++) {
            SkiersApi skiersApi = new SkiersApi();
            skiersApi.getApiClient().setBasePath(BASE_URL);
            SkierPostRequest reqObject = requestObjList.poll();
            if (reqObject == null) {
                i--;
                continue;
            }
            int current = 0;
            // Send out 5 retries
            while (current < NUM_OF_RETRIES) {
                try {
                    timer.startTimer();
                    skiersApi.writeNewLiftRide(
                            reqObject.liftRide,
                            reqObject.resortID,
                            reqObject.seasonID,
                            reqObject.dayID,
                            reqObject.skierID
                    );
                    timer.endTimer();
                    break;
                } catch (ApiException e) {
                    responseCode = e.getCode();
                    if (e.getCode() >= 400 && e.getCode() < 600) {
                        // retry
                        current++;
                        continue;
                    }
                    timer.endTimer();
                }
            }
            if (current == NUM_OF_RETRIES) {
                // add back the request object to the queue
                requestObjList.add(reqObject);
                timer.endTimer();
            }
            timer.recordToQueue(latencies, new LatencyInfo(timer.startTime, "POST",
                    timer.getLatencyInMs(), responseCode));
            // success counter
            localPostCounter.set(localPostCounter.get() + 1);
        }
    }

}
