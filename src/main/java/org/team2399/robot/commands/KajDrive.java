package org.team2399.robot.commands;

import org.team2399.robot.OI;
import org.team2399.robot.Utility;
import org.team2399.robot.subsystems.DriveTrain;

import edu.wpi.first.wpilibj.command.Command;

/**
 *
 */
public class KajDrive extends Command {

	DriveTrain dt;
	OI oi;
	
	public KajDrive(DriveTrain dt, OI oi) {
        // Use requires() here to declare subsystem dependencies
        // eg. requires(chassis);
    	this.dt = dt;
    	this.oi = oi;
    	requires(this.dt);
		setInterruptible(true);
    }
    // Called just before this Command runs the first time
    protected void initialize() {
    	dt.disableVoltageComp();
    	dt.brakeMode();
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
	    	double forward = oi.getLeftStickY();
	    	double turn = oi.getRightStickX();
	    	
	    	double leftSideSpeed = (forward + turn * (Math.abs(forward)));
		double rightSideSpeed = (forward - turn * (Math.abs(forward)));
		
		if(Utility.inRange(forward, 0, OI.DEADBAND_WIDTH * 2))
		{
			leftSideSpeed = turn/ 2;
			rightSideSpeed = -turn / 2;
		}
			
		
		if (oi.getLeftShoulder()) {
			leftSideSpeed = -1;
			rightSideSpeed = 1;
		}
		
		if (oi.getRightShoulder()) {
			leftSideSpeed = 1;
			rightSideSpeed = -1;
		}
		
		dt.drivePercent(leftSideSpeed, rightSideSpeed);
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
        return false;
    }

    // Called once after isFinished returns true
    protected void end() {
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    }
}
