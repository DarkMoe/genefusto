package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;

public class SWAP implements GenInstructionHandler {

	final Gen68 cpu;
	
	public SWAP(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	SWAP -- Swap register upper and lower words
//
//SYNOPSIS
//        SWAP	Dn
//
//        Size = (Word)
//
//FUNCTION
//        Swaps between 16 low bits and 16 high bits of register.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|---|---|---|-----------|
//	| 0 | 1 | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 1 | 0 | 0 | 0 | REGISTER  |
//	-----------------------------------------------------------------
//
//	"REGISTER" indicates the number of register on which swap is made.
//
//RESULT
//	X - Not affected
//	N - Set if the most-significant bit of the result was set. Cleared
//	    otherwise.
//	Z - Set if the 32 bits result was zero. Cleared otherwise.
//	V - Always cleared.
//	C - Always cleared.
	
	@Override
	public void generate() {
		int base = 0x4840;
		GenInstruction ins;
		
		ins = new GenInstruction() {
			
			@Override
			public void run(int opcode) {
				SWAPWord(opcode);
			}

		};
		
		for (int r = 0; r < 8; r++) {
			int opcode = base | r;
			cpu.addInstruction(opcode, ins);
		}	
	}
	
	private void SWAPWord(int opcode) {
		int register = (opcode & 0x7);
		long data = cpu.getDLong(register);
		
		long res = ((data & 0xFFFF) << 16) | ((data & 0xFFFF_0000L) >> 16);
				
		cpu.setDLong(register, res);
				
		calcFlags(res);
	}

	void calcFlags(long data) {
		if (data == 0) {
			cpu.setZ();
		} else {
			cpu.clearZ();
		}
		if ((data & 0x8000_0000L) > 0) {
			cpu.setN();
		} else {
			cpu.clearN();
		}
		
		cpu.clearV();
		cpu.clearC();
	}
	
}
