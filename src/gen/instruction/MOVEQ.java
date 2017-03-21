package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class MOVEQ implements GenInstructionHandler {

	final Gen68 cpu;
	
	public MOVEQ(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	MOVEQ -- Move signed 8-bit data quick
//
//SYNOPSIS
//	MOVEQ	#<data:8>,Dn
//
//	Size = (Long)
//
//FUNCTION
//	Move signed 8-bit data to the specified data register.
//	The specified data is sign extended to 32-bits before
//	it is moved to the register.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|---|-------------------------------|
//	| 0 | 1 | 1 | 1 | REGISTER  | 0 |        IMMEDIATE DATA         |
//	-----------------------------------------------------------------
//
//	"REGISTER" is one of the 8 datas registers. 
//
//RESULT
//	X - Not affected.
//	N - Set if the result is negative. Cleared otherwise.
//	Z - Set if the result is zero. Cleared otherwise.
//	V - Always cleared.
//	C - Always cleared.
	
	@Override
	public void generate() {
		int base = 0x7000;
		GenInstruction ins = null;
		
		ins = new GenInstruction() {
			
			@Override
			public void run(int opcode) {
				MOVEQLong(opcode);
			}
		};
		for (int r = 0; r < 8; r++) {
			for (int immData = 0; immData < 256; immData++) {
				int opcode = base + (r << 9) | (immData);
				cpu.addInstruction(opcode, ins);
			}
		}
	}
	
	private void MOVEQLong(int opcode) {
		int register = (opcode >> 9) & 0x7;
		int immData = opcode & 0xFF;
		long data = immData;
		if ((immData & 0x80) > 0) {
			data |= 0xFFFF_FF00L;
		}

		cpu.setDLong(register, data);
		
		calcFlags(data, Size.longW.getMsb());
	}
	
	void calcFlags(long data, long msb) {
		if (data == 0) {
			cpu.setZ();
		} else {
			cpu.clearZ();
		}
		if ((data & msb) > 0) {
			cpu.setN();
		} else {
			cpu.clearN();
		}
		
		cpu.clearV();
		cpu.clearC();
	}

}
