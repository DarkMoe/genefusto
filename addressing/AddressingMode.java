package gen.addressing;

import gen.instruction.Operation;

public interface AddressingMode {

	void setByte(Operation o);
	void setWord(Operation o);
	void setLong(Operation o);
	
	long getByte(Operation o);
	long getWord(Operation o);
	long getLong(Operation o);
}
