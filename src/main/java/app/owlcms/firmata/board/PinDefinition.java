package app.owlcms.firmata;

public class PinDefinition {
	String pin;
	
	public int getPinNumber() {
		Integer val;
		try {
			val = Integer.valueOf(pin);
		} catch (NumberFormatException e) {
			val = 0;
		}
		return val;
	}

}