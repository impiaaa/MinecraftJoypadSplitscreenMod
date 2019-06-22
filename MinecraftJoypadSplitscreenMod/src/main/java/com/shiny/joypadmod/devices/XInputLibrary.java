package com.shiny.joypadmod.devices;

import java.util.LinkedList;

import com.github.strikerx3.jxinput.XInputAxesDelta;
import com.github.strikerx3.jxinput.XInputButtonsDelta;
import com.github.strikerx3.jxinput.XInputComponentsDelta;
import com.github.strikerx3.jxinput.XInputDevice;
import com.github.strikerx3.jxinput.XInputDevice14;
import com.github.strikerx3.jxinput.XInputLibraryVersion;
import com.github.strikerx3.jxinput.enums.XInputAxis;
import com.github.strikerx3.jxinput.enums.XInputButton;
import com.github.strikerx3.jxinput.exceptions.XInputNotLoadedException;
import com.github.strikerx3.jxinput.listener.SimpleXInputDeviceListener;
import com.github.strikerx3.jxinput.listener.XInputDeviceListener;
import com.github.strikerx3.jxinput.natives.XInputNatives;
import com.shiny.joypadmod.helpers.LogHelper;
import com.shiny.joypadmod.inputevent.AxisInputEvent;
import com.shiny.joypadmod.inputevent.ControllerInputEvent;

public class XInputLibrary extends InputLibrary {
	
	private Boolean created = false;
	float axisSignalThreshold = 0.5f; // the point at which we will trigger an axis event if it meets the threshold	
	protected Boolean xInput14 = false;
	
	public Boolean recentlyDisconnected = false;
	public Boolean recentlyConnected = false;
	
	XInputDeviceWrapper theDevice;
	
	static LinkedList events = new LinkedList();
	
	// used for event notification for polling
	int lastEvent = -1;
	int lastEventControlIndex = -1;
	
	public class InputEvent {
		int eventType = -1; // 0 = axis event 1 = button press
		int eventControlIndex = -1; // button or axis number
		public InputEvent(int event, int controlIndex)
		{
			eventType = event;
		    eventControlIndex = controlIndex;
		}
	};

	@Override
	public void create() throws Exception {
		if (XInputDevice14.isAvailable())
			xInput14 = true;
		else if (!XInputDevice.isAvailable())
		{
			if (!XInputNatives.isLoaded())
			{
				LogHelper.Error("XInput native libraries failed to load with error: " + XInputNatives.getLoadError().toString());
			}
			else
			{
				LogHelper.Error("XInputNatives were loaded but XInputDevice reports it is not available");
			}
			
			throw new Exception("XInput is not available on system.");
		}
		
		theDevice = new XInputDeviceWrapper(0);
		theDevice.setIndex(0, xInput14);
		created = true;
		LogHelper.Info("XInput is available on system.  Using version: " + XInputLibraryVersion.values()[XInputNatives.getLoadedLibVersion()].toString());
	}

	@Override
	public Boolean isCreated() {
		return created;
	}


	@Override
	public void clearEvents() {
		events.clear();
	}

	@Override
	public InputDevice getController(int index) {
		if (index != theDevice.theDevice.getPlayerNum())
		{
			theDevice.theDevice.removeListener(listener);
			theDevice.setIndex(index, xInput14);
			theDevice.theDevice.addListener(listener);			
		}
		return theDevice;
	}

	@Override
	public int getControllerCount() {
		return 4;
	}

	@Override
	public InputDevice getEventSource() {
		return theDevice;
	}

	@Override
	public int getEventControlIndex() {
		return lastEventControlIndex;
	}

	@Override
	public Boolean isEventButton() {
		return lastEvent == 0;
	}

	@Override
	public Boolean isEventAxis() {
		return lastEvent == 1;
	}

	@Override
	public Boolean isEventPovX() {
		return false;
	}

	@Override
	public Boolean isEventPovY() {
		return false;
	}

	@Override
	public Boolean next() {
	
		if (events.isEmpty())
			return false;
		InputEvent event = (InputEvent)events.removeFirst();
		lastEvent = event.eventType;
		lastEventControlIndex = event.eventControlIndex;
		return true;
	}


	@Override
	public void poll() {
		theDevice.theDevice.poll();
				
		for (int i = 0; i < 6; i++)
		{
			if (Math.abs(theDevice.getAxisValue(i)) > axisSignalThreshold)
			{
				events.add(new InputEvent(1, i));
			}
		}			
		
	}
	
	XInputDeviceListener listener = new SimpleXInputDeviceListener() {
	    @Override
	    public void connected() {
	        LogHelper.Info("Connection message received");
	        recentlyConnected = true;
	    }

	    @Override
	    public void disconnected() {
	    	LogHelper.Info("Disconnection message received");
	    	recentlyDisconnected = true;
	    }

	    @Override
	    public void buttonChanged(final XInputButton button, final boolean pressed) {
	    	events.add(new InputEvent(0, button.ordinal()));
	    	LogHelper.Info(button.name() + (pressed ? " pressed" : " released"));
	        // the given button was just pressed (if pressed == true) or released (pressed == false)
	    }
	};
	
	@Override
	public Boolean wasDisconnected() {
		if (recentlyDisconnected)
		{
			recentlyDisconnected = false;
			return true;		
		}
		return false;
	}

	@Override
	public Boolean wasConnected() {
		if (recentlyConnected)
		{
			recentlyConnected = false;
			return true;		
		}
		return false;
	}

	@Override
	public InputDevice getCurrentController() {

		return theDevice;
	}

}
