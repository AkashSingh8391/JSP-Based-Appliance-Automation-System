<%-- index.jsp --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Offline Home Automation (USB Serial)</title>
<style>
    body {
        font-family: Arial, sans-serif;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        min-height: 100vh;
        background-color: #f0f0f0;
        margin: 0;
        padding: 20px;
        box-sizing: border-box;
    }
    .container {
        background-color: #fff;
        padding: 30px;
        border-radius: 10px;
        box-shadow: 0 6px 12px rgba(0, 0, 0, 0.15);
        text-align: center;
        width: 100%;
        max-width: 600px;
    }
    h1 {
        color: #333;
        margin-bottom: 25px;
    }
    p {
        color: #666;
        margin-bottom: 30px;
        font-size: 0.9em;
    }
    .relay-control {
        margin-bottom: 25px;
        border: 1px solid #e0e0e0;
        padding: 20px;
        border-radius: 8px;
        background-color: #fafafa;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }
    .relay-control h2 {
        margin: 0;
        color: #555;
        font-size: 1.4em;
    }
    .button-group {
        display: flex;
        gap: 10px; /* Space between buttons */
    }
    .button {
        background-color: #28a745; /* Green for ON */
        color: white;
        padding: 12px 25px;
        border: none;
        border-radius: 5px;
        cursor: pointer;
        font-size: 1em;
        transition: background-color 0.3s ease, transform 0.1s ease;
        flex-grow: 1; /* Make buttons expand evenly */
    }
    .button:hover {
        background-color: #218838;
        transform: translateY(-2px);
    }
    .button.off {
        background-color: #dc3545; /* Red for OFF */
    }
    .button.off:hover {
        background-color: #c82333;
    }
    #statusMessage {
        margin-top: 30px;
        padding: 15px;
        border-radius: 8px;
        background-color: #e9ecef;
        color: #343a40;
        font-weight: bold;
        text-align: left;
        font-size: 0.95em;
        word-wrap: break-word; /* Ensure long messages wrap */
    }
</style>
<script>
    function controlRelay(relayNumber, action) {
        // The Java Desktop Application runs on localhost at SERVER_PORT (e.g., 8080).
        // This URL must match the port and context path defined in your Java application.
        var javaAppUrl = "http://localhost:8080/control"; // <<<--- IMPORTANT: Ensure this matches your Java App's port

        // Construct the command string (e.g., "R1ON", "R2OFF").
        var command = "R" + relayNumber + action.toUpperCase();

        // Create the full URL with the command as a query parameter.
        var url = javaAppUrl + "?command=" + command;

        // Update the status message on the JSP page to show command is being sent.
        var statusDiv = document.getElementById('statusMessage');
        statusDiv.innerText = "Sending command: " + command + " to Java application...";
        statusDiv.style.backgroundColor = "#fff3cd"; // Light yellow for pending
        statusDiv.style.color = "#856404";

        // Use the Fetch API to send an HTTP GET request to the Java Desktop Application.
        fetch(url)
            .then(response => {
                // Check if the HTTP request itself was successful (e.g., HTTP 200 OK).
                if (!response.ok) {
                    throw new Error(`HTTP error! Status: ${response.status}`);
                }
                return response.text(); // Read the response body (which is the Arduino's reply) as text.
            })
            .then(data => {
                // This 'data' variable contains the response sent back by the Java application
                // (which in turn received it from the Arduino).
                console.log("Response from Java App:", data);
                statusDiv.innerText = "Java App Response: " + data;
                if (data.includes("ERROR")) {
                    statusDiv.style.backgroundColor = "#f8d7da"; // Light red for error
                    statusDiv.style.color = "#721c24";
                } else {
                    statusDiv.style.backgroundColor = "#d4edda"; // Light green for success
                    statusDiv.style.color = "#155724";
                }
            })
            .catch(error => {
                // Handle any errors during the fetch operation (e.g., Java app not running, network issues).
                console.error("Wait for the running because port has to be busy :", error);
                statusDiv.innerText = "CONNECTION should take time because of usb serial Port  " + error.message + ")";
                statusDiv.style.backgroundColor = "#f8d7da"; // Light red for error
                statusDiv.style.color = "#721c24";
            });
    }
</script>
</head>
<body>
    <div class="container">
        <h1>Home Automation via USB Serial</h1>
        <p>This system controls relays connected to an Arduino Uno via your computer's USB serial port, using a Java application as an intermediary for JSP web page commands.</p>

        <% // Loop to generate buttons for 4 relays. %>
        <% for (int i = 1; i <= 4; i++) { %>
            <div class="relay-control">
                <h2>Relay <%= i %></h2>
                <div class="button-group">
                    <%-- ON button for the current relay --%>
                    <button class="button" onclick="controlRelay(<%= i %>, 'on')">Turn ON</button>
                    <%-- OFF button for the current relay --%>
                    <button class="button off" onclick="controlRelay(<%= i %>, 'off')">Turn OFF</button>
                </div>
            </div>
        <% } %>
        <div id="statusMessage">Awaiting commands...</div>
    </div>
</body>
</html>