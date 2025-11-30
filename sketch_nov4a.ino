#include <WiFi.h>
#include <HTTPClient.h>
#include <PubSubClient.h>
#include <Adafruit_NeoPixel.h>

const char* ssid = "WIFI_NAME";
const char* password = "12345678";

const char* AIO_KEY = "AIO_KEY";
const char* AIO_USERNAME = "serhaterdgn1";

const char* MQTT_SERVER = "io.adafruit.com";
const int MQTT_PORT = 1883;

String TOPIC_LIGHT = String(AIO_USERNAME) + "/feeds/light";
String TOPIC_INFOLIGHT = String(AIO_USERNAME) + "/feeds/infolight";


#define LED_PIN 48
#define NUM_LEDS 1
Adafruit_NeoPixel pixels(NUM_LEDS, LED_PIN, NEO_GRB + NEO_KHZ800);

WiFiClient espClient;
//mqtt protokol bağlantısı oluşturan class. wificlient variable'ı ile oluşturuyoruz. Wifi ile haberleşeceğimizi netleştirmis oluyoruz
PubSubClient client(espClient);

void callback(char* topic, byte* payload, unsigned int lenght);
void connect();


void setup() {
  Serial.begin(115200);

  pixels.begin();
  pixels.setBrightness(30);
  pixels.show();

  Serial.println("WiFi'ye baglaniyor...");
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(300);
  }

  Serial.println("\nBaglandi");
  
  //hangi broker a bağlanacağını belirliyoruz
  client.setServer(MQTT_SERVER, MQTT_PORT);
  //brokerda bir değişiklik olduğunda hangi metodun çağrılacağını belirliyoruz.
  client.setCallback(callback);

}

void loop() {

  if (!client.connected()) {
    connect();
  }
  //bağlantıyı her zaman açık tutar
  client.loop();
}

void connect(){

  Serial.println("MQTT'ye bağlanılıyor...");
  //benzersiz id oluşturmak için. her istemcinin unıque olması gerek
  String clientId = "Esp32s3-"+String(random(0xffff),HEX);

  if(client.connect(clientId.c_str(),AIO_USERNAME,AIO_KEY)){
    
      Serial.println("MQQT Serverine Baglandi");
      client.subscribe(TOPIC_LIGHT.c_str());
      Serial.println("Abone olundu: " + TOPIC_LIGHT);

  }else{
    Serial.println("Baglantı başarısız tekrar denenecek");
    Serial.print("baglantı hatası= ");
    Serial.println(client.state());
    delay(2000);
  }

}

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Mesaj gelen Topic: ");
  Serial.println(topic);
  String message;
  for(int i=0; i<length; i++){
    message+=(char)payload[i];
  }

  if(message=="on-beyaz"){ 
    pixels.setPixelColor(0,pixels.Color(255,255,255));
    client.publish(TOPIC_INFOLIGHT.c_str(), "Beyaz ışık yakıldı.");
  }
  else if (message=="on-sari"){
    pixels.setPixelColor(0,pixels.Color(255,255,0));
    client.publish(TOPIC_INFOLIGHT.c_str(), "Sarı ışık yakıldı.");
  } 
  else if (message=="on-yesil"){
    pixels.setPixelColor(0,pixels.Color(0,255,0));
    client.publish(TOPIC_INFOLIGHT.c_str(), "Yeşil ışık yakıldı.");
  } 
  else if (message=="on-kirmizi"){
    pixels.setPixelColor(0,pixels.Color(255,0,0));
    client.publish(TOPIC_INFOLIGHT.c_str(), "Kırmızı ışık yakıldı.");
  } 
  else if (message=="off"){
    pixels.setPixelColor(0,pixels.Color(0,0,0));
    client.publish(TOPIC_INFOLIGHT.c_str(), "Işık kapatıldı.");
  } 

  pixels.show();

//client.publish(TOPIC_INFOLIGHT.c_str(), message.c_str());

}
