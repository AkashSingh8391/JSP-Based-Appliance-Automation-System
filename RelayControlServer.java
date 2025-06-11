package Controller; // यदि आप इसे किसी पैकेज में रखते हैं, तो इसे रखें। अन्यथा, इसे हटा दें।

import com.fazecast.jSerialComm.SerialPort;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class RelayControlServer {

    private static SerialPort arduinoPort = null;

    // --- IMPORTANT: CHANGE THIS TO YOUR ARDUINO'S ACTUAL COM PORT! ---
    // यह Arduino IDE में Tools > Port के नीचे मिलेगा।
    private static final String ARDUINO_PORT_NAME = "COM9"; // <-- यहां आपका COM पोर्ट!

    private static final int BAUD_RATE = 9600;
    private static final int SERVER_PORT = 8080; // JSP पेज के JavaScript में javaAppUrl से मैच करता है

    public static void main(String[] args) throws IOException {
        // --- Step 1: Initialize Serial Communication with Arduino ---
        System.out.println("Searching for available COM ports...");
        SerialPort[] comPorts = SerialPort.getCommPorts();

        if (comPorts.length == 0) {
            System.err.println("No COM ports found. Make sure Arduino is connected and drivers are installed.");
            return;
        }

        for (SerialPort port : comPorts) {
            System.out.println("Found port: " + port.getSystemPortName() + " - " + port.getDescriptivePortName());
            if (port.getSystemPortName().equalsIgnoreCase(ARDUINO_PORT_NAME)) {
                arduinoPort = port;
                break;
            }
        }

        if (arduinoPort == null) {
            System.err.println("Error: Arduino port '" + ARDUINO_PORT_NAME + "' not found! Please check the port name.");
            return;
        }

        arduinoPort.setBaudRate(BAUD_RATE);
        // Set timeouts for reading and writing to avoid blocking indefinitely.
        arduinoPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 2000, 2000); // 2 second timeout

        if (arduinoPort.openPort()) {
            System.out.println("Arduino port " + arduinoPort.getSystemPortName() + " opened successfully.");
            // Add a small delay for Arduino to reset after serial connection
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Thread interrupted during sleep: " + e.getMessage());
            }
        } else {
            System.err.println("Error: Could not open Arduino port '" + ARDUINO_PORT_NAME + "'! Make sure it's not in use by other programs (e.g., Arduino IDE Serial Monitor).");
            return;
        }

        // --- Step 2: Start HTTP Server to listen for JSP requests ---
        HttpServer server = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);
        server.createContext("/control", new ControlHandler());
        server.setExecutor(Executors.newFixedThreadPool(5));
        server.start();
        System.out.println("HTTP Server started on port " + SERVER_PORT);
        System.out.println("Ready to receive commands from JSP (e.g., http://localhost:" + SERVER_PORT + "/control?command=R1ON)");

        // Add a shutdown hook to close the serial port when the Java app closes
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (arduinoPort != null && arduinoPort.isOpen()) {
                arduinoPort.closePort();
                System.out.println("Arduino port closed.");
            }
            System.out.println("Server shutting down.");
        }));
    }

    static class ControlHandler implements com.sun.net.httpserver.HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String responseToJSP = "ERROR: Unknown issue";
            int httpStatus = 500;

            try {
                if ("GET".equals(exchange.getRequestMethod())) {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.startsWith("command=")) {
                        String command = query.substring("command=".length());
                        System.out.println("Received command from JSP: " + command);

                        if (arduinoPort != null && arduinoPort.isOpen()) {
                            byte[] commandBytes = (command + "\n").getBytes(StandardCharsets.US_ASCII);
                            arduinoPort.writeBytes(commandBytes, commandBytes.length);
                            System.out.println("Sent to Arduino: '" + command + "'");

                            byte[] readBuffer = new byte[100];
                            int numRead = arduinoPort.readBytes(readBuffer, readBuffer.length);
                            if (numRead > 0) {
                                responseToJSP = new String(readBuffer, 0, numRead, StandardCharsets.US_ASCII).trim();
                                System.out.println("Received from Arduino: '" + responseToJSP + "'");
                                httpStatus = 200;
                            } else {
                                responseToJSP = "OK (No specific response from Arduino)";
                                httpStatus = 200;
                                System.out.println("No immediate response from Arduino.");
                            }
                        } else {
                            responseToJSP = "ERROR: Arduino port is not open or not initialized.";
                            httpStatus = 500;
                            System.err.println(responseToJSP);
                        }
                    } else {
                        responseToJSP = "ERROR: Missing or invalid 'command' parameter in URL.";
                        httpStatus = 400;
                        System.err.println(responseToJSP);
                    }
                } else {
                    responseToJSP = "ERROR: Only GET requests are supported.";
                    httpStatus = 405;
                    System.err.println(responseToJSP);
                }
            } catch (Exception e) {
                responseToJSP = "ERROR: Internal server error: " + e.getMessage();
                httpStatus = 500;
                System.err.println("Exception in ControlHandler: " + e.getMessage());
                e.printStackTrace();
            } finally {
                exchange.sendResponseHeaders(httpStatus, responseToJSP.length());
                OutputStream os = exchange.getResponseBody();
                os.write(responseToJSP.getBytes(StandardCharsets.UTF_8));
                os.close();
            }
        }
    }
}