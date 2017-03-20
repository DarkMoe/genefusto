package gen.instruction;

public enum AddressingMode {

	dataRegisterDirect,
	addressRegisterDirect,
	addressRegisterInderect,
	addressRegisterIndirectPostIncrement,
	addressRegisterIndirectPreIncrement,
	addressWithDisplacement,
	NOSE,
	
	absoluteWord,
	absoluteLong,
	PCIndirectDisplacement,
	NOsee,
	directData;
	
	public static AddressingMode getAddressingMode(int mode, int register) {
		if (mode < 7) {
			return AddressingMode.values()[mode];
		} else {
			return AddressingMode.values()[mode + register];
		}
	}
}
