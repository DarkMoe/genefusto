package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;

public class ORI_CCR implements GenInstructionHandler {

	final Gen68 cpu;
	
	public ORI_CCR(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	ORI to CCR -- Logical OR immediate to the condition code register
//
//SYNOPSIS
//	ORI	#<data>,CCR
//
//	Size = (Byte)
//
//FUNCTION
//	Performs an OR operation on the condition codes
//	register with the source operand.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
//	| 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 1 | 1 | 1 | 1 | 0 | 0 |
//	|---|---|---|---|---|---|---|---|-------------------------------|
//	| 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |     8 BITS IMMEDIATE DATA     |
//	-----------------------------------------------------------------
//
//RESULT
//	X - Set if bit 4 of the source is set, cleared otherwise.
//	N - Set if bit 3 of the source is set, cleared otherwise.
//	Z - Set if bit 2 of the source is set, cleared otherwise.
//	V - Set if bit 1 of the source is set, cleared otherwise.
//	C - Set if bit 0 of the source is set, cleared otherwise.
	
	@Override
	public void generate() {
		int opcode = 0x003C;
		GenInstruction ins = null;
		
		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				ORICCR(opcode);
			}
		};
		
		cpu.addInstruction(opcode, ins);
	}
	
	private void ORICCR(int opcode) {
		long toOr  = (cpu.bus.read(cpu.PC + 2)) << 8;
		 	 toOr |= (cpu.bus.read(cpu.PC + 3));
		
		toOr &= 0xFF;	//	8 bits
		 	 
	 	cpu.PC += 2;
		 	 
	 	int flags = (int) (toOr & 0x1F);	// solo se usa el byte inferior con los 5 flags
		
		cpu.SR = (cpu.SR & 0xFFE0) | flags;
	}
	
}
