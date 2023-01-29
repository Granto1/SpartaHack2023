//#include <VirtualWire.h>
#include <SoftwareSerial.h>
#include <Servo.h>

SoftwareSerial Blue(2, 3);  //HM-18 Bluetooth (RXD, TXD)
const int trigPin = 7;      //Funny pin stuff
const int echoPin = 6;
const int MAX = 90;  //Maximum angle
bool isConnected = false; //connection?
long int data;

long duration;      //duration for distance calc
int distance;       //distance calc
int pos = 0;        //Position
Servo myservo;      //Servo motor
int isDanger = 50;  //Minimum distance to warn

int count = 0;       //to check if a cycle has been reached
String output = "";  //output to android application
int minDist = 9999;
int minPos = 9999;

void setup() {
  myservo.attach(8);         //Servo init
  pinMode(trigPin, OUTPUT);  //stuff
  pinMode(echoPin, INPUT);
  Serial.begin(9600);  //start stuff
  Blue.begin(9600);
}
void loop() {
  int x;

  if (!isConnected) {            //if it's not connected
                                 //Serial.print("Connected");
    if (Blue.available() > 0) {  //attempt connection
                                 //Serial.print("Connected2");
      if (Blue.readString().substring(0, Blue.readString().length() - 1).equals("-2")) {
        isConnected = true;
        // Serial.print("Connected3");
      }
    }
  } else {
    minDist = 9999;

    for (pos = 0; pos <= MAX; pos += 1)  //first pass
    {
      if (pos == MAX || pos == MAX / 4 || pos == MAX / 2 || pos == 3 * MAX / 4) see();
      else delay(5);
      myservo.write(pos);  //servo position
    }
    if (count == 4) {
      count = 0;
      if (minDist < isDanger / 2) {
        x = Blue.write(2);
        Serial.print(x);
      } else if (minDist < isDanger && minPos > MAX / 2) {
        x = Blue.write(1);
        Serial.print(x);
      } else if (minDist < isDanger && minPos < MAX / 2) {
        x = Blue.write(3);
        Serial.print(x);
      }
    }

    minDist = 9999;

    for (pos = MAX; pos >= 0; pos -= 1)  //reverse pass
    {
      if (pos == 0 || pos == MAX / 4 || pos == MAX / 2 || pos == 3 * MAX / 4) see();
      else delay(5);
      myservo.write(pos);  //servo position
    }

    if (count == 4) {
      count = 0;
      if (minDist < isDanger / 2) {
        x = Blue.write(2);
        Serial.print(x);
      } else if (minDist < isDanger && minPos > MAX / 2) {
        x = Blue.write(1);
        Serial.print(x);

      } else if (minDist < isDanger && minPos < MAX / 2) {
        x = Blue.write(3);
        Serial.print(x);
      }
    }
  }
}

void see() {
  //ultrasonic input
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(2);
  digitalWrite(trigPin, LOW);
  duration = pulseIn(echoPin, HIGH);

  distance = duration * 0.034 / 2;  //Distance calculation

  //Printing information to the console
  // Serial.print("Distance: ");
  // Serial.println(distance);
  count++;

  if (distance < minDist && distance > 0) {
    minDist = distance;
    minPos = pos;
  }
}