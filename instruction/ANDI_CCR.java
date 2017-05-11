package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;

public class ANDI_CCR implements GenInstructionHandler {

	final Gen68 cpu;
	
	public ANDI_CCR(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	ANDI to CCR -- Logical AND immediate to condition code register
//
//SYNOPSIS
//	ANDI	#<data>,CCR
//
//	Size = (Byte)
//
//FUNCTION
//	Performs a bit-wise AND operation with the immediate data and
//	the lower byte of the status register.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
//	| 0 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | 0 | 0 | 1 | 1 | 1 | 1 | 0 | 0 |
//	|---|---|---|---|---|---|---|---|-------------------------------|
//	| 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |     8 BITS IMMEDIATE DATA     |
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
		int opcode = 0x023C;
		GenInstruction ins = null;
		
		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				ANDICCR(opcode);
			}
		};
		
		cpu.addInstruction(opcode, ins);
	}
	
	private void ANDICCR(int opcode) {
		long toAnd  = (cpu.bus.read(cpu.PC + 2)) << 8;
		 	 toAnd |= (cpu.bus.read(cpu.PC + 3));
		toAnd &= 0xFF;
		
	 	cpu.PC += 2;
		 	 
	 	int res = (int) ((cpu.SR & 0xFFE0) | toAnd);
		cpu.SR = res;
	}
	
}
