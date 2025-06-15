
const int RELAY_PINS[] = {7, 6, 5, 4};
const int NUM_RELAYS = sizeof(RELAY_PINS) / sizeof(RELAY_PINS[0]);

void setup() {
  Serial.begin(9600);

  while (!Serial) {
    
  }

  Serial.println("Arduino Ready. Waiting for commands from computer.");

  for (int i = 0; i < NUM_RELAYS; i++) {
    pinMode(RELAY_PINS[i], OUTPUT);
    digitalWrite(RELAY_PINS[i], HIGH);
  }
}

void loop() {

  if (Serial.available()) {

    String command = Serial.readStringUntil('\n');
    command.trim();

    Serial.print("Received command: ");
    Serial.println(command); 
    if (command.startsWith("R") && command.length() >= 3) { 
      int relayNum = command.substring(1, 2).toInt();

      String action = command.substring(2);

      if (relayNum >= 1 && relayNum <= NUM_RELAYS) {

        int pinToControl = RELAY_PINS[relayNum - 1];

        if (action.equals("ON")) {
          digitalWrite(pinToControl, LOW); 
          Serial.print("Relay ");
          Serial.print(relayNum);
          Serial.println(" ON");
          Serial.println("OK"); 
        } else if (action.equals("OFF")) {
          digitalWrite(pinToControl, HIGH); 
          Serial.print("Relay ");
          Serial.print(relayNum);
          Serial.println(" OFF");
          Serial.println("OK"); 
        } else if (action.equals("STATUS")) {

          if (digitalRead(pinToControl) == LOW) {
            Serial.println("R" + String(relayNum) + "IS_ON");
          } else {
            Serial.println("R" + String(relayNum) + "IS_OFF");
          }
        } else {
          Serial.println("ERROR: Invalid Action for Relay");
        }
      } else {
        Serial.println("ERROR: Invalid Relay Number");
      }
    } else {
      Serial.println("ERROR: Unknown Command Format");
    }
  }
}