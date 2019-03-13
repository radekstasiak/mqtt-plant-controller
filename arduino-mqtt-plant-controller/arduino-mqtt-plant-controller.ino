#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <Ticker.h>

#define PIN_PUMP BUILTIN_LED //D0 // nodemcu built in LED

int DELAY_LOW=1000;

int MQTT_PUBLISH_MOISTURE_TIMEOUT=30;
int MQTT_LOOP_TIMEOUT=1;

int WATER_PUMP_SAFE_LOCK_TIMEOUT=2;
int AUTO_WATERING_MODE_TIMEOUT=15;

char* MQTT_TOPIC_WATER_PUMP_CMD = "waterPump";
char* MQTT_TOPIC_GET_WATER_PUMP_STATUS = "getWaterPumpStatus";
char* MQTT_TOPIC_SEND_WATER_PUMP_STATUS = "sendWaterPumpStatus";
char* MQTT_TOPIC_HUMIDITY_LEVEL="hmdtLevel";
char* MQTT_TOPIC_HUMIDITY_LEVEL_CMD="hmdtLevelCmd";
char* MQTT_TOPIC_AUTOWATERING_CMD="autoWateringCmd";
char* MQTT_TOPIC_AUTOWATERING_STATUS="autoWateringStatus";

char* MQTT_TOPIC_CURRENT_STATE="currentState";

const char* ssid="SKY5D769";
const char* wifi_pass="XPXXSTPR";
const char* mqtt_server="m24.cloudmqtt.com";
const char* mqtt_user="vlfnelyd";
const char* mqtt_pass="nXE1FVBVSJHn";
const int mqtt_port=17236;

WiFiClient espClient;
PubSubClient client(espClient);
Ticker mqttLoopTicker;
Ticker mqttPublishMoistureTicker;
Ticker waterPumpSafeLockMechanism;
Ticker autoWateringModeTicker;
long lastMsg=0;
char msg[50];

int waterPumpStatus=LOW;
int autoWateringModeStatus=0;
//TODO - one channel for all waterPump communication
void setup(){
  Serial.begin(9600);
  pinMode(PIN_PUMP,OUTPUT);
  digitalWrite(PIN_PUMP,LOW);
 
  setup_wifi();
  
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);
  reconnect();
  
  mqttLoopTicker.attach(MQTT_LOOP_TIMEOUT,mqttLoop);
  mqttPublishMoistureTicker.attach(MQTT_PUBLISH_MOISTURE_TIMEOUT,mqttPublishMoisture);
  waterPumpSafeLockMechanism.attach(WATER_PUMP_SAFE_LOCK_TIMEOUT,waterPumpSafeLock);
  autoWateringModeTicker.attach(AUTO_WATERING_MODE_TIMEOUT,autoWateringMode);
}

void startWaterPump(){
  Serial.println("Starting water pump");
  digitalWrite(PIN_PUMP,HIGH);
  Serial.println("Water pump started");
  publishWaterPumpStatus();
}

void stopWaterPump(){
  Serial.println("Stopping water pump...");
  digitalWrite(PIN_PUMP,LOW);
  Serial.println("Water pump stopped");
  publishWaterPumpStatus();
}

void starAutoWateringMode(){
  autoWateringModeStatus = 1;
  Serial.println("!!! Autowatering mode is on !!!");
  publishAutoWateringStatus();
}

void stopAutoWateringMode(){
  autoWateringModeStatus = 0;
  Serial.println("!!! Autowatering mode is off !!!");
  publishAutoWateringStatus();
}



void waterPumpSafeLock(){
  int sensorValue = analogRead(A0);
  int numh = map(sensorValue, 1024, 0, 0, 100);
  if(digitalRead(PIN_PUMP) == 1 && numh > 70){
    Serial.println("!!! Water pump safe lock !!!");
    stopWaterPump();
    mqttPublishMoisture();
  }
}

void autoWateringMode(){
  if(autoWateringModeStatus==1){
      Serial.println("Autowatering starts...");
      Serial.println("Starting reading moisture level...");
      int sensorValue = analogRead(A0);
      delay(DELAY_LOW);
      int numh = map(sensorValue, 1024, 0, 0, 100);
      Serial.print("Current value: ");
      Serial.print(numh);
      Serial.println("");
      if(numh < 20 && digitalRead(PIN_PUMP) == 0){
        startWaterPump();
      }
      
  }
}
void loop() {
  if(!client.connected()){
    reconnect();
  }
 }


void setup_wifi(){
  Serial.begin(9600);
  Serial.println("");

  WiFi.begin(ssid, wifi_pass);

  Serial.print("Connecting");
  while (WiFi.status() != WL_CONNECTED)
  {
    delay(500);
    Serial.print(".");
  }
  Serial.println();

  Serial.print("Connected, IP address: ");
  Serial.println(WiFi.localIP());
}

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  String cmd="";
  for (int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
    cmd+=(char)payload[i];
  }

  Serial.println("Received cmd: ");
  Serial.print(cmd);
  if(String(topic).equals(MQTT_TOPIC_WATER_PUMP_CMD)){
    if(cmd.equals("start")) {
      startWaterPump();
      mqttPublishMoisture();
    }

    if(cmd.equals("stop")) {
      stopWaterPump();
      mqttPublishMoisture();
    }
  }else if(String(topic).equals(MQTT_TOPIC_HUMIDITY_LEVEL_CMD)){
    if(cmd.equals("status")){
      mqttPublishMoisture();
    }
  }else if(String(topic).equals(MQTT_TOPIC_GET_WATER_PUMP_STATUS)){
    publishWaterPumpStatus();
  }else if(String(topic).equals(MQTT_TOPIC_AUTOWATERING_CMD)){
    if(cmd.equals("start")){
      starAutoWateringMode();
    }else if(cmd.equals("stop")){
      stopAutoWateringMode();
    }else if(cmd.equals("status")){
      publishAutoWateringStatus();
    }
  }else if(String(topic).equals(MQTT_TOPIC_CURRENT_STATE)){
    if(cmd.equals("status")){
      publishCurrentState();
    }
  }
  Serial.println("");

}

void mqttLoop(){
  if(client.connected()){
    Serial.println("MQTT looping...");
    client.loop();
  }else {
    Serial.println("MQTT not looping, not connected...");
  }
}

void subscribeForWaterPumpStatus(){
      Serial.println("Subscribe to topic: ");
      Serial.println(MQTT_TOPIC_GET_WATER_PUMP_STATUS);
      client.subscribe(MQTT_TOPIC_GET_WATER_PUMP_STATUS);
      Serial.println("Subscribed");
}

void subscribeForWaterPumpControl(){
      Serial.println("Subscribe to topic: ");
      Serial.println(MQTT_TOPIC_WATER_PUMP_CMD);
      client.subscribe(MQTT_TOPIC_WATER_PUMP_CMD);
      Serial.println("Subscribed");
}

void subscribeForHmdtLvlControl(){
      Serial.println("Subscribe to topic: ");
      Serial.println(MQTT_TOPIC_HUMIDITY_LEVEL_CMD);
      client.subscribe(MQTT_TOPIC_HUMIDITY_LEVEL_CMD);
      Serial.println("Subscribed");
}

void subscribeForAutoWateringCmd(){
      Serial.println("Subscribe to topic: ");
      Serial.println(MQTT_TOPIC_AUTOWATERING_CMD);
      client.subscribe(MQTT_TOPIC_AUTOWATERING_CMD);
      Serial.println("Subscribed");
}


void subscribeForCurrentState(){
      Serial.println("Subscribe to topic: ");
      Serial.println(MQTT_TOPIC_CURRENT_STATE);
      client.subscribe(MQTT_TOPIC_CURRENT_STATE);
      Serial.println("Subscribed");
}
void mqttPublishMoisture(){
  Serial.println("Starting reading moisture level...");
  int sensorValue = analogRead(A0);
  delay(DELAY_LOW);
  Serial.print("Current value: ");
  Serial.print(sensorValue);
  Serial.println("");
  String msg=String(sensorValue);
  Serial.println("Publishing message: ");
  Serial.print(msg);
  Serial.println("");
  int numh = map(sensorValue, 1024, 0, 0, 100);
  char cshr[16];
  itoa(numh,cshr,10);
  client.publish(MQTT_TOPIC_HUMIDITY_LEVEL,cshr);
  Serial.print("Published");
}


 void publishWaterPumpStatus(){
  Serial.println("Publishing water pump satus: ");
  int waterPumpStatus = digitalRead(PIN_PUMP);
  Serial.print(waterPumpStatus);
  Serial.println("");
  char cshr[16];
  itoa(waterPumpStatus,cshr,10);
  client.publish(MQTT_TOPIC_SEND_WATER_PUMP_STATUS,cshr);
  Serial.print("Published");
 }

  void publishAutoWateringStatus(){
  Serial.println("Publishing autowatering mode satus: ");
  Serial.print(autoWateringModeStatus);
  Serial.println("");
  char cshr[16];
  itoa(autoWateringModeStatus,cshr,10);
  client.publish(MQTT_TOPIC_AUTOWATERING_STATUS,cshr);
  Serial.print("Published");
 }

 void publishCurrentState(){
  Serial.println("Publishing current state: ");
  Serial.println("");
  int sensorValue = analogRead(A0);
  
//  String hmdtRawVal = "{\"hmdt_raw_value\": " + String(sensorValue);
//  String hmdtPercentageVal = ", \"hmdt_percentage_value\": " + String(map(sensorValue, 1024, 0, 0, 100));
//  String waterPumpStatus = ", \"water_pump_status\": " + String(waterPumpStatus);
//  String autoWateringModeStatus = ", \"auto_watering_mode_status\": " + String(autoWateringModeStatus)+"}";

  String jsonValue = "{\"hmdt_raw_value\": " + String(sensorValue);
   jsonValue += ", \"hmdt_percentage_value\": " + String(map(sensorValue, 1024, 0, 0, 100));
   jsonValue += ", \"water_pump_status\": " + String(waterPumpStatus);
   jsonValue += ", \"auto_watering_mode_status\": " + String(autoWateringModeStatus)+"}";
  
  char data[300];
  Serial.println("xxx");
  Serial.println(jsonValue);
  Serial.println("xxx");
  jsonValue.toCharArray(data,(jsonValue.length()+1));
  
  Serial.println("");
  client.publish(MQTT_TOPIC_CURRENT_STATE,data);
 }


void reconnect() {
  // Loop until we're reconnected
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    // Create a random client ID
    String clientId = "ESP8266Client-";
    clientId += String(random(0xffff), HEX);
    // Attempt to connect
    if (client.connect(clientId.c_str(),mqtt_user,mqtt_pass)) {
      Serial.println("connected");
      // Once connected, publish an announcement...
      client.publish("outTopic", "hello world");

      // ... and resubscribe
    subscribeForWaterPumpStatus();
    subscribeForWaterPumpControl();
    subscribeForHmdtLvlControl();
    
    subscribeForAutoWateringCmd();
    subscribeForCurrentState();
    
//    publishWaterPumpStatus();
//    publishAutoWateringStatus();
    publishCurrentState();
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}
