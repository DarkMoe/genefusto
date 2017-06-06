package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class ANDI_SR implements GenInstructionHandler {

	final Gen68 cpu;
	
	public ANDI_SR(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	ANDI to SR -- Logical AND immediate to status register (privileged)
//
//SYNOPSIS
//	ANDI	#<data>,SR
//
//	Size = (Word)
//
//FUNCTION
//	Performs a bit-wise AND operation with the immediate data and
//	the status register. All implemented bits of the status register are
//	affected.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
//	| 0 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | 0 | 1 | 1 | 1 | 1 | 1 | 0 | 0 |
//	|---------------------------------------------------------------|
//	|                     16 BITS IMMEDIATE DATA                    |
//	-----------------------------------------------------------------
//
//RESULT
//	X - Cleared if bit 4 of immed. operand is zero. Unchanged otherwise.
//	N - Cleared if bit 3 of immed. operand is zero. Unchanged otherwise.
//	Z - Cleared if bit 2 of immed. operand is zero. Unchanged otherwise.
//	V - Cleared if bit 1 of immed. operand is zero. Unchanged otherwise.
//	C - Cleared if bit 0 of immed. operand is zero. Unchanged otherwise.

	@Override
	public void generate() {
		int opcode = 0x027C;
		GenInstruction ins = null;
		
		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				ANDISR(opcode);
			}
		};
		
		cpu.addInstruction(opcode, ins);
	}
	
	private void ANDISR(int opcode) {
		long toAnd = cpu.bus.read(cpu.PC + 2, Size.WORD);
		
	 	cpu.PC += 2;
		 	 
		long res = cpu.SR & toAnd;
		
		if (((cpu.SR & 0x2000) ^ (res & 0x2000)) != 0) {	//	si cambio el supervisor bit
			if ((res & 0x2000) == 0x2000) {
				cpu.setALong(7, cpu.SSP);
			} else {
				cpu.setALong(7, cpu.USP);
			}	
		}

		cpu.SR = (int) res;
	}
	
}
