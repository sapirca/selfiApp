#include "Arduino.h"
#include "SoftwareSerial.h"
#include "Servo.h"

//SoftwareSerial btSerial(8, 9); // RX, TX
SoftwareSerial btSerial(7, 8); // RX, TX

int servoPin_LR  =  5;    // control pin for servo motor
int servoPin_UD  =  6;    // control pin for servo motor

// The Arduino will calculate these values for you

long lastCommand;

// (Up-Down servo)
Servo servo1;
Servo servo2;

int panUD		= 0;
int degreeUD    = 110;
int delayUD		= 0;

int tiltLR 		= 0;
int degreeLR	= 90;

#define MAX_RANGE_UD         135
#define MIN_RANGE_UD         85

#define MAX_RANGE_LR         155
#define MIN_RANGE_LR         25

#define CONSTANT_DEGREE_JUMP 1

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
	Serial.begin(115200);
	btSerial.begin(9600);

//	pinMode(servoPin_LR, OUTPUT);  // Set servo pin as an output pin
//	pulseWidth = PW_STOP;   // Give the servo a stop command

	lastCommand = millis();

	servo1.attach(servoPin_UD);
	servo1.write(degreeUD);

	servo2.attach(servoPin_LR);
	servo2.write(degreeLR);

	Serial.println("Selfi v2 Started!");
}

byte command;
int intCommand;

void loop()
{
//	if (Serial.available()) {
//		String data = Serial.readString();
//		Serial.println(data);
//		servo1.write(data.toInt());
//	}
//
//	return;

	// Wait for data...
	if (btSerial.available()) {
//	if (Serial.available()) {

		command = btSerial.read();
//		command = Serial.readString().toInt();

		Serial.print("Command: ");
		Serial.println(command);

		switch (command)
		{
			case PAN_UP_START:    panUD =  1; break;
			case PAN_DOWN_START:  panUD = -1; break;

			case PAN_UP_STOP:
			case PAN_DOWN_STOP:   panUD =  0; break;

			case PAN_RIGHT_START: tiltLR =  1; break;
			case PAN_LEFT_START:  tiltLR = -1; break;

			case PAN_RIGHT_STOP:
			case PAN_LEFT_STOP :  tiltLR = 0; break;

//			case PAN_SELFI_STOP:
//			case PAN_RESET_STOP:
//			case PAN_RIGHT_STOP:
//			case PAN_LEFT_STOP:   pulseWidth = PW_STOP;   break;
//			case PAN_RIGHT_START: pulseWidth = PW_RIGHT;  break;
//			case PAN_LEFT_START:  pulseWidth = PW_LEFT;   break;
//			case PAN_SELFI_START: pulseWidth = PW_SELFI;  break;
//			case PAN_RESET_START: pulseWidth = PW_RESET;  break;
			default: break;
		}
	}

	long currTime = millis();

	// Execute commands every 10ms
	if (currTime - lastCommand > 10) {

		lastCommand = currTime;

		// Slow down the UD servo by 5 times
		if (++delayUD >= 5) {

			delayUD = 0;

			if(panUD == 1) {
				degreeUD += CONSTANT_DEGREE_JUMP;

				if (degreeUD >= MAX_RANGE_UD) {
					degreeUD = MAX_RANGE_UD;
					panUD = 0;
				}

				Serial.print("Up: ");
				Serial.println(degreeUD);

				servo1.write(degreeUD);
			}
			else if (panUD == -1) {
				degreeUD -= CONSTANT_DEGREE_JUMP;

				if (degreeUD <= MIN_RANGE_UD) {
					degreeUD = MIN_RANGE_UD;
					panUD = 0;
				}

				Serial.print("Down: ");
				Serial.println(degreeUD);

				servo1.write(degreeUD);
			}
		}

		if(tiltLR == 1) {
			degreeLR += CONSTANT_DEGREE_JUMP;

			if (degreeLR >= MAX_RANGE_LR) {
				degreeLR = MAX_RANGE_LR;
				tiltLR = 0;
			}

			Serial.print("Right: ");
			Serial.println(degreeLR);

			servo2.write(degreeLR);
		}
		else if (tiltLR == -1) {
			degreeLR -= CONSTANT_DEGREE_JUMP;

			if (degreeLR <= MIN_RANGE_LR) {
				degreeLR = MIN_RANGE_LR;
				tiltLR = 0;
			}

			Serial.print("Left: ");
			Serial.println(degreeLR);

			servo2.write(degreeLR);
		}
	}
}
