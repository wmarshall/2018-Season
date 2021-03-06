package org.team2399.robot.subsystems;

import java.util.Arrays;

import org.team2399.robot.RobotMap;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.IMotorController;
import com.ctre.phoenix.motorcontrol.IMotorControllerEnhanced;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;

import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.hal.PDPJNI;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Lift extends Subsystem {

	public static final int LIFT_MAX_HEIGHT_ENCODERS = 60000;
	public static final int LIFT_MAX_HEIGHT_INCHES = 79;
	private static final int PID_IDX = 0;
	private static final double CLOSED_LOOP_VOLTAGE_SATURATION = 10;
	private double fuzz = 0.001;

	private PowerDistributionPanel pdp;
	private IMotorControllerEnhanced talon;
	private IMotorController victor;
	private double desiredHeight;
	private boolean isManual;
	private boolean zeroed;
	
	private double[] filter;
	private int filterIndex;

	//change can timeout
	private static final int CAN_TIMEOUT = 10;
	private static final double LIFT_KP = 0.15;
	private static final double LIFT_KI = 0.0001;
	private static final double LIFT_KD = 2.5;
	private static final double LIFT_KF = 0;
	
	public Lift(PowerDistributionPanel pdp) {
		desiredHeight = 0;
		isManual = true;
		zeroed = false;
		
		this.pdp = pdp;
		
		talon = new TalonSRX(23);
		victor = new VictorSPX(24);
		
		talon.configVoltageCompSaturation(CLOSED_LOOP_VOLTAGE_SATURATION, CAN_TIMEOUT);
		talon.enableVoltageCompensation(true);
		
		talon.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, PID_IDX, CAN_TIMEOUT);
		talon.setSensorPhase(true);
		
		talon.configNominalOutputForward(0, CAN_TIMEOUT);
		talon.configNominalOutputReverse(0, CAN_TIMEOUT);
		talon.configPeakOutputForward(1, CAN_TIMEOUT);
		talon.configPeakOutputReverse(-1, CAN_TIMEOUT);
		
		victor.follow(talon);
		victor.setInverted(true);
		
		talon.configForwardSoftLimitThreshold(LIFT_MAX_HEIGHT_ENCODERS, CAN_TIMEOUT);
		talon.configForwardSoftLimitEnable(true, CAN_TIMEOUT);
		
		setConstants(LIFT_KP, LIFT_KI, LIFT_KD, LIFT_KF);
		
		filter = new double[20];
		filterIndex = 0;
		
		for(int i = 0; i < filter.length; i++)
			filter[i] = 0.0;
	}
	
	 @Override
	public void periodic() {
		if(!zeroed) {
			talon.setSelectedSensorPosition(0, PID_IDX, CAN_TIMEOUT);
			zeroed = true;
		}
		
		flipFuzz();
		setHeight();
		double[] currentArr = {talon.getOutputCurrent(), victor.getOutputCurrent()};
		
		double[] inputCurrentArr = {pdp.getCurrent(3), pdp.getCurrent(12), fuzz};
		double inputVoltage = pdp.getVoltage();
		double[] outputVoltage = {talon.getMotorOutputVoltage(), victor.getMotorOutputVoltage(), fuzz};
		
		double[] liftPosArr = {getHeight(), desiredHeight, fuzz};
		double[] percentArr = {talon.getMotorOutputPercent(), victor.getMotorOutputPercent(), fuzz};
		
		double[] outputCurrentCalc = new double[3];
		for(int i = 0; i < outputCurrentCalc.length - 1; i++) {
			double d = inputCurrentArr[i] * inputVoltage / outputVoltage[i];
			outputCurrentCalc[i] = Double.isNaN(d) ? inputCurrentArr[i] : d;
		}
		outputCurrentCalc[2] = fuzz;
		
		//SmartDashboard.putNumberArray("inputLiftCurrent", currentArr);
		SmartDashboard.putNumberArray("liftPos", liftPosArr);
		//SmartDashboard.putNumberArray("outputLiftCurrent", outputCurrentCalc);
		SmartDashboard.putNumberArray("percent", percentArr);
		
//		System.out.println(isManual);
	}
	 
	private void setHeight() {
		
		 filter[filterIndex] = desiredHeight;
		 filterIndex = (filterIndex + 1) % filter.length;
		 
		 double filterOut = 0;
		 
		 for(double filterBin : filter)
			 filterOut += filterBin;
		
		 filterOut /= (double)filter.length;
		 
		 if(!isManual) {
			 talon.set(ControlMode.Position, inchesToEncoderTicks(filterOut));
		 }
		 else
		 {
			 for(int i = 0; i < filter.length; i++)
				 filter[i] = encoderTicksToInches(desiredHeight);
		 }
	}
	
	public void setVarHeight(double desiredHeight) {
		this.desiredHeight = desiredHeight;
		isManual = false;
	}
	
	public void manualControl(double percent) {
		isManual = true;
		talon.set(ControlMode.PercentOutput, percent * RobotMap.Physical.Lift.LIFT_UP);
	}
	
	public boolean isManual() {
		return isManual;
	}

	public void setConstants(double p, double i, double d, double f) {
		talon.config_kP(PID_IDX, p, CAN_TIMEOUT);
		talon.config_kI(PID_IDX, i, CAN_TIMEOUT);
		talon.config_kD(PID_IDX, d, CAN_TIMEOUT);
		talon.config_kF(PID_IDX, f, CAN_TIMEOUT);
	}
	
	public void defaultCommand(Command c) {
	    	setDefaultCommand(c);
	}
	
	public double getHeight() {
		return encoderTicksToInches(talon.getSelectedSensorPosition(PID_IDX));
	}
	
	private void flipFuzz() {
    	fuzz *= -1;
    }
	
	private double inchesToEncoderTicks(double inches) {
		return inches * LIFT_MAX_HEIGHT_ENCODERS / LIFT_MAX_HEIGHT_INCHES;
	}
	
	private double encoderTicksToInches(double ticks) {
		return ticks * LIFT_MAX_HEIGHT_INCHES / LIFT_MAX_HEIGHT_ENCODERS;
	}
	
	@Override
	protected void initDefaultCommand() {
		// TODO Auto-generated method stub
		
	}
	 
}
