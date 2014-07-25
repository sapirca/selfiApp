#include <Servo.h> 

int servoPin_LR  =  9;    // control pin for servo motor
int servoPin_UD  =  10;    // control pin for servo motor

// 360 servo (Right-Left rotation)

// Adjust these values for your servo and setup, if necessary 
int minPulse     =  1170;  // maximum servo speed clockwise
int maxPulse     =  1770; // maximum servo speed anticlockwise
int turnRate     =  5;  // servo turn rate increment (larger value, faster rate)
int refreshTime  =  20;   // time (ms) between pulses (50Hz)
 
// Pulse width constants
#define PW_RIGHT  1475
#define PW_LEFT   1500
#define PW_STOP   1490
#define PW_SELFI  1520
#define PW_RESET  1455

// The Arduino will calculate these values for you 

int pulseWidth;          // servo pulse width
long lastPulse   = 0;    // recorded time (ms) of the last pulse


// (Up-Down servo)
Servo servo1; 

int panUD        = 0;
int degreeUD     = 30;
int loopCounter  = 0;

#define MAX_RANGE_UD         90
#define MIN_RANGE_UD         0
#define CONSTANT_DEGREE_JUMP 3

// Android-Arduino command

#define PAN_UP_START      25
#define PAN_UP_STOP       26
#define PAN_DOWN_START    27
#define PAN_DOWN_STOP     28
#define PAN_LEFT_START    29
#define PAN_LEFT_STOP     30
#define PAN_RIGHT_START   31
#define PAN_RIGHT_STOP    32

#define PAN_SELFI_START   33
#define PAN_SELFI_STOP    34
#define PAN_RESET_START   35
#define PAN_RESET_STOP    36

void setup()
{
  Serial.begin(9600);
  
  pinMode(servoPin_LR, OUTPUT);  // Set servo pin as an output pin

  pulseWidth = PW_STOP;   // Give the servo a stop command

  servo1.attach(servoPin_UD);
  servo1.write(90); 
}

byte command;
int intCommand;

void loop()
{
         if (Serial.peek() != -1) {
               command = Serial.read();
    //            String val = Serial.readString();
    //            intCommand = val.toInt();
    //            Serial.println("Command: " + intCommand);
         
     
         switch (command)
         {
          case PAN_UP_START:    panUD=1;                break;
          case PAN_DOWN_START:  panUD=-1;               break;
          case PAN_UP_STOP:
          case PAN_DOWN_STOP:   panUD=0;                break;
          case PAN_SELFI_STOP:
          case PAN_RESET_STOP:  
          case PAN_RIGHT_STOP:  
          case PAN_LEFT_STOP:   pulseWidth = PW_STOP;   break;      
          case PAN_RIGHT_START: pulseWidth = PW_RIGHT;  break;
          case PAN_LEFT_START:  pulseWidth = PW_LEFT;   break;
          case PAN_SELFI_START: pulseWidth = PW_SELFI;  break;
          case PAN_RESET_START: pulseWidth = PW_RESET;  break;
          default: break;
        }
        
        // Run every 5 times
        if (loopCounter==5) {
          loopCounter = 0;  
          
          if(panUD == 1) {
            degreeUD += CONSTANT_DEGREE_JUMP;
            if (degreeUD >= MAX_RANGE_UD)
               degreeUD = MAX_RANGE_UD;
               servo1.write(degreeUD+60);
          }
          else if (panUD == -1) {
            degreeUD -= CONSTANT_DEGREE_JUMP;
            if (degreeUD <= MIN_RANGE_UD)
               degreeUD = MIN_RANGE_UD;
            servo1.write(degreeUD+60);
          }
        }
        else {
          loopCounter += 1;
        }
        
    }
    
    if(millis() - lastPulse >= 20)//refreshTime
    {
        digitalWrite(servoPin_LR, HIGH);      // start the pulse
        delayMicroseconds(pulseWidth);        // pulse width
        digitalWrite(servoPin_LR, LOW);      // stop the pulse
        lastPulse = millis();                // save the time of the last pulse
    }
}
