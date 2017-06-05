package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class EORI_CCR implements GenInstructionHandler {

	final Gen68 cpu;
	
	public EORI_CCR(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	EORI to CCR -- Exclusive OR immediate to the condition code register
//
//SYNOPSIS
//	EORI	#<data>,CCR
//
//	Size = (Byte)
//
//FUNCTION
//	Performs an exclusive OR operation on the condition codes
//	register with the source operand.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
//	| 0 | 0 | 0 | 0 | 1 | 0 | 1 | 0 | 0 | 0 | 1 | 1 | 1 | 1 | 0 | 0 |
//	|---|---|---|---|---|---|---|---|-------------------------------|
//	| 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |     8 BITS IMMEDIATE DATA     |
//	-----------------------------------------------------------------
//
//RESULT
//	X - Changed if bit 4 of the source is set, cleared otherwise.
//	N - Changed if bit 3 of the source is set, cleared otherwise.
//	Z - Changed if bit 2 of the source is set, cleared otherwise.
//	V - Changed if bit 1 of the source is set, cleared otherwise.
//	C - Changed if bit 0 of the source is set, cleared otherwise.
	
	@Override
	public void generate() {
		int opcode = 0x0A3C;
		GenInstruction ins = null;
		
		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				EORICCR(opcode);
			}
		};
		
		cpu.addInstruction(opcode, ins);
	}
	
	private void EORICCR(int opcode) {
		long data = cpu.bus.read(cpu.PC + 2, Size.WORD);
		data = data & 0x1F;
		
	 	cpu.PC += 2;
		 	 
	 	long res = ((cpu.SR & 0x1F) ^ data) & 0x1F;
	 	
	 	cpu.SR = (int) ((cpu.SR & 0xFFE0) | res);
	}
	
}
