import lejos.hardware.BrickFinder;
import lejos.hardware.Button;
import lejos.hardware.Keys;
import lejos.hardware.ev3.EV3;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.robotics.SampleProvider;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.navigation.MovePilot;
import lejos.robotics.subsumption.Arbitrator;
import lejos.robotics.subsumption.Behavior;
import lejos.utility.Delay;

public class MainClass
{
	static EV3LargeRegulatedMotor	LEFT_MOTOR	= new EV3LargeRegulatedMotor(MotorPort.C);
	static EV3LargeRegulatedMotor	RIGHT_MOTOR	= new EV3LargeRegulatedMotor(MotorPort.B);

	static Wheel					wheel1		= WheeledChassis.modelWheel(LEFT_MOTOR, 56).offset(65);
	static Wheel					wheel2		= WheeledChassis.modelWheel(RIGHT_MOTOR, 56).offset(-65);

	static Chassis					chassis		= new WheeledChassis(new Wheel[] { wheel1, wheel2 }, WheeledChassis.TYPE_DIFFERENTIAL);

	static MovePilot				pilot		= new MovePilot(chassis);

	static IRSensor					irsensor	= new IRSensor();

	public static void main(String[] args)
	{
		Stop stop = new Stop();
		stop.setDaemon(true);
		stop.start();

		EV3 ev3brick = (EV3) BrickFinder.getLocal();

		Keys buttons = ev3brick.getKeys();

		irsensor.setDaemon(true);
		irsensor.start();
		
		Behavior b1 = new FollowLine();
		Behavior b2 = new DetectsObject();
		
		Behavior[] behaviorList = { b1, b2 };
		Arbitrator arbitrator = new Arbitrator(behaviorList);

		buttons.waitForAnyPress();
		
		arbitrator.go();
	}

}

class Stop extends Thread
{

	Stop()
	{

	}

	public void run()
	{

		while (!Button.ESCAPE.isDown())
		{

		}
		System.exit(0);

	}

}

class IRSensor extends Thread
{
	EV3IRSensor		ir			= new EV3IRSensor(SensorPort.S1);
	SampleProvider	sp			= ir.getDistanceMode();
	float[]			sample		= new float[sp.sampleSize()];
	protected float	distance	= 70;

	//Constructor
	IRSensor()
	{

	}

	public void run()
	{
		while (!Button.ESCAPE.isDown())
		{
			sp.fetchSample(sample, 0);
			distance = sample[0];

		}
		System.exit(0);

	}

}

class FollowLine implements Behavior
{
	static EV3ColorSensor	colorSensorR	= new EV3ColorSensor(SensorPort.S4);

	static EV3ColorSensor	colorSensorL	= new EV3ColorSensor(SensorPort.S3);

	static SampleProvider	colorR			= colorSensorR.getRGBMode();

	static SampleProvider	colorL			= colorSensorL.getRGBMode();

	static float[]			sampleR			= new float[colorR.sampleSize()];

	static float[]			sampleL			= new float[colorL.sampleSize()];

	static float[]			previousSampleR	= new float[colorR.sampleSize()];

	static float[]			previousSampleL	= new float[colorL.sampleSize()];

	private boolean			_suppressed		= false;

	@Override
	public boolean takeControl()
	{
		return true;
	}

	@Override
	public void action()
	{
		_suppressed = false;
		MainClass.RIGHT_MOTOR.setSpeed(60);
		MainClass.LEFT_MOTOR.setSpeed(60);

		MainClass.LEFT_MOTOR.forward();
		MainClass.RIGHT_MOTOR.forward();

		int i;

		while (!_suppressed)
		{
			colorR.fetchSample(sampleR, 0);

			colorL.fetchSample(sampleL, 0);

			if ((getColorR(sampleR) == 1) && (getColorR(previousSampleR) == 0)) //Right sensor sees Green and previously saw Black
			{
				LCD.drawString("R Sensor Green Black", 0, 3);
				Delay.msDelay(2000);
				LCD.clear();

				for (i = 0; i < sampleR.length; i++)
				{
					sampleR[i] = 1;
					sampleL[i] = 1;
				}
			}

			if ((getColorL(sampleL) == 1) && (getColorL(previousSampleL) == 0)) //Left sensor sees Green and previously saw Black
			{
				LCD.drawString("L Sensor Green Black", 0, 3);
				Delay.msDelay(2000);
				LCD.clear();

				for (i = 0; i < sampleR.length; i++)
				{
					sampleR[i] = 1;
					sampleL[i] = 1;
				}
			}

			if ((getColorR(sampleR) == 1) && (getColorL(sampleL) == 1)) //Both Sensors see Green (Turn Around)
			{
				LCD.drawString("Turn Around 1", 0, 3);

				MainClass.LEFT_MOTOR.stop();
				MainClass.RIGHT_MOTOR.stop();

				MainClass.pilot.rotate(200);

				MainClass.RIGHT_MOTOR.setSpeed(60);
				MainClass.LEFT_MOTOR.setSpeed(60);

				MainClass.LEFT_MOTOR.forward();
				MainClass.RIGHT_MOTOR.forward();

				LCD.clear();

				for (i = 0; i < sampleR.length; i++)
				{
					sampleR[i] = 1;
					sampleL[i] = 1;
				}

			}

			if (getColorR(sampleR) == 0) //Right Sensor sees Black
			{
				LCD.drawString("Black", 0, 3);

				MainClass.RIGHT_MOTOR.stop();
				MainClass.LEFT_MOTOR.stop();

				MainClass.RIGHT_MOTOR.setSpeed(95);
				MainClass.LEFT_MOTOR.setSpeed(105);

				MainClass.RIGHT_MOTOR.backward();
				MainClass.LEFT_MOTOR.forward();

				Delay.msDelay(300);

				MainClass.RIGHT_MOTOR.stop();
				MainClass.LEFT_MOTOR.stop();

				colorL.fetchSample(sampleL, 0);

				if (getColorL(sampleL) == 1)
				{
					MainClass.RIGHT_MOTOR.setSpeed(60);
					MainClass.LEFT_MOTOR.setSpeed(60);

					MainClass.LEFT_MOTOR.forward();
					MainClass.RIGHT_MOTOR.forward();

					LCD.drawString("L Sensor Green Black", 0, 3);

					Delay.msDelay(2000); //3000

					colorL.fetchSample(sampleL, 0);

					LCD.clear();

				}
				else
				{
					MainClass.RIGHT_MOTOR.setSpeed(60);
					MainClass.LEFT_MOTOR.setSpeed(60);

					MainClass.RIGHT_MOTOR.forward();
					MainClass.LEFT_MOTOR.forward();

					LCD.clear();
				}

			}

			if (getColorL(sampleL) == 0) //Left Sensor sees Black
			{
				LCD.drawString("Black", 0, 3);

				MainClass.LEFT_MOTOR.stop();
				MainClass.RIGHT_MOTOR.stop();

				MainClass.RIGHT_MOTOR.setSpeed(105);
				MainClass.LEFT_MOTOR.setSpeed(95);

				MainClass.LEFT_MOTOR.backward();
				MainClass.RIGHT_MOTOR.forward();

				Delay.msDelay(300);

				MainClass.LEFT_MOTOR.stop();
				MainClass.RIGHT_MOTOR.stop();

				colorR.fetchSample(sampleR, 0);

				if (getColorR(sampleR) == 1)
				{
					MainClass.RIGHT_MOTOR.setSpeed(60);
					MainClass.LEFT_MOTOR.setSpeed(60);

					MainClass.LEFT_MOTOR.forward();
					MainClass.RIGHT_MOTOR.forward();

					LCD.drawString("R Sensor Green Black", 0, 3);

					Delay.msDelay(2000); //3000

					colorR.fetchSample(sampleR, 0);

					LCD.clear();
				}
				else
				{
					MainClass.RIGHT_MOTOR.setSpeed(60);
					MainClass.LEFT_MOTOR.setSpeed(60);

					MainClass.LEFT_MOTOR.forward();
					MainClass.RIGHT_MOTOR.forward();

					LCD.clear();

				}
			}

			if ((getColorR(sampleR) == 2) && (getColorL(sampleL) == 2)) //Both Sensors see Red (Stop)
			{
				LCD.drawString("Red = Stop", 0, 3);

				MainClass.pilot.stop();
				
				System.exit(0);
			}

			if ((getColorR(previousSampleR) == 1) && ((getColorL(sampleL) != 2) && (getColorL(previousSampleL) != 2))) //Right Sensor saw Green before
			{
				LCD.drawString("R Sensor Green", 0, 3);

				Delay.msDelay(100);

				colorL.fetchSample(sampleL, 0);

				Delay.msDelay(3300); //3300		

				if (getColorL(sampleL) == 1)
				{
					LCD.clear();

					LCD.drawString("Turn Around 2", 0, 3);

					MainClass.LEFT_MOTOR.stop();
					MainClass.RIGHT_MOTOR.stop();

					MainClass.pilot.rotate(200);

					MainClass.RIGHT_MOTOR.setSpeed(60);
					MainClass.LEFT_MOTOR.setSpeed(60);

					MainClass.LEFT_MOTOR.forward();
					MainClass.RIGHT_MOTOR.forward();

					LCD.clear();

					for (i = 0; i < sampleR.length; i++)
					{
						sampleR[i] = 1;
						sampleL[i] = 1;
					}
				}
				else if (getColorL(sampleL) != 2)
				{

					MainClass.RIGHT_MOTOR.stop();
					MainClass.LEFT_MOTOR.stop();

					MainClass.pilot.rotate(-100); //-110

					MainClass.RIGHT_MOTOR.setSpeed(60);
					MainClass.LEFT_MOTOR.setSpeed(60);

					MainClass.RIGHT_MOTOR.forward();
					MainClass.LEFT_MOTOR.forward();

					Delay.msDelay(1500);

					LCD.clear();

					colorR.fetchSample(sampleR, 0);

					colorL.fetchSample(sampleL, 0);
				}

			}

			if ((getColorL(previousSampleL) == 1) && ((getColorR(sampleR) != 2) && (getColorL(previousSampleR) != 2))) //Left Sensor saw Green before
			{
				LCD.drawString("L Sensor Green", 0, 3);

				Delay.msDelay(100);

				colorR.fetchSample(sampleR, 0);

				Delay.msDelay(3300);

				if (getColorR(sampleR) == 1)
				{
					LCD.clear();

					LCD.drawString("Turn Around 2", 0, 3);

					MainClass.LEFT_MOTOR.stop();
					MainClass.RIGHT_MOTOR.stop();

					MainClass.pilot.rotate(200);

					MainClass.RIGHT_MOTOR.setSpeed(60);
					MainClass.LEFT_MOTOR.setSpeed(60);

					MainClass.LEFT_MOTOR.forward();
					MainClass.RIGHT_MOTOR.forward();

					LCD.clear();

					for (i = 0; i < sampleR.length; i++)
					{
						sampleR[i] = 1;
						sampleL[i] = 1;
					}
				}
				else if (getColorR(sampleR) != 2)
				{
					MainClass.LEFT_MOTOR.stop();
					MainClass.RIGHT_MOTOR.stop();

					MainClass.pilot.rotate(100);

					MainClass.RIGHT_MOTOR.setSpeed(60);
					MainClass.LEFT_MOTOR.setSpeed(60);

					MainClass.LEFT_MOTOR.forward();
					MainClass.RIGHT_MOTOR.forward();

					Delay.msDelay(1500);

					LCD.clear();

					colorR.fetchSample(sampleR, 0);

					colorL.fetchSample(sampleL, 0);
				}
			}

			//Delay for Sensors
			Delay.msDelay(50);

			//Save the Previous Samples
			for (i = 0; i < sampleR.length; i++)
			{
				previousSampleR[i] = sampleR[i];
				previousSampleL[i] = sampleL[i];
			}
		}
		MainClass.LEFT_MOTOR.stop();
		MainClass.RIGHT_MOTOR.stop();
	}

	private static int getColorR(float[] color)
	{

		if ((Math.floor(color[0] * 1e2) / 1e2 <= 0.02) && (Math.floor(color[1] * 1e2) / 1e2 <= 0.02)
				&& (Math.floor(color[2] * 1e2) / 1e2 <= 0.02))
			return 0; //Black
		else if ((Math.floor(color[0] * 1e2) / 1e2 >= 0.1) && (Math.floor(color[1] * 1e2) / 1e2 >= 0.1)
				&& (Math.floor(color[2] * 1e2) / 1e2 >= 0.1))
			return -1; //White
		else if ((Math.floor(color[0] * 1e2) / 1e2 <= 0.01) && (Math.floor(color[1] * 1e2) / 1e2 == 0.04)
				&& (Math.floor(color[2] * 1e2) / 1e2 == 0.02))
			return 1; //Green		
		else if ((Math.floor(color[0] * 1e2) / 1e2 >= 0.09) && (Math.floor(color[1] * 1e2) / 1e2 <= 0.01)
				&& (Math.floor(color[2] * 1e2) / 1e2 <= 0.01))
			return 2; //Red
		else
			return -1; //Any other Color
	}

	private static int getColorL(float[] color)
	{

		if ((Math.floor(color[0] * 1e2) / 1e2 <= 0.02) && (Math.floor(color[1] * 1e2) / 1e2 <= 0.02)
				&& (Math.floor(color[2] * 1e2) / 1e2 <= 0.02))
			return 0; //Black
		else if ((Math.floor(color[0] * 1e2) / 1e2 >= 0.08) && (Math.floor(color[1] * 1e2) / 1e2 >= 0.08)
				&& (Math.floor(color[2] * 1e2) / 1e2 >= 0.1))
			return -1; //White
		else if ((Math.floor(color[0] * 1e2) / 1e2 <= 0.01) && (Math.floor(color[1] * 1e2) / 1e2 == 0.03)
				&& (Math.floor(color[2] * 1e2) / 1e2 == 0.02))
			return 1; //Green		
		else if ((Math.floor(color[0] * 1e2) / 1e2 >= 0.07) && (Math.floor(color[1] * 1e2) / 1e2 <= 0.01)
				&& (Math.floor(color[2] * 1e2) / 1e2 <= 0.01))
			return 2; //Red
		else
			return -1; //Any other Color
	}

	@Override
	public void suppress()
	{
		_suppressed = true;
	}

}

class DetectsObject implements Behavior
{

	@Override
	public boolean takeControl()
	{
		float distance = MainClass.irsensor.distance;

		if (distance <= 5)
		{
			Button.LEDPattern(2);
			return true;
		}

		else
		{
			Button.LEDPattern(1);
			return false;
		}

	}

	@Override
	public void action()
	{
		MainClass.pilot.rotate(90);
		
		//MainClass.pilot.setAngularSpeed(100);
		
		MainClass.pilot.forward();
		Delay.msDelay(1000);
		
		MainClass.pilot.rotate(-100);
		
		MainClass.pilot.forward();
		Delay.msDelay(1800);
		
		MainClass.pilot.rotate(-100);
		
		MainClass.pilot.forward();
		Delay.msDelay(900);
		
		MainClass.pilot.rotate(90);
		
		MainClass.pilot.stop();
	}

	@Override
	public void suppress()
	{
	}

}