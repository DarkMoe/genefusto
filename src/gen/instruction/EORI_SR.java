package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class EORI_SR implements GenInstructionHandler {

	final Gen68 cpu;
	
	public EORI_SR(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	EORI to SR -- Exclusive OR immediated to the status register (PRIVILEGED)
//
//SYNOPSIS
//	EORI	#<data>,SR
//
//	Size = (Word)
//
//FUNCTION
//	Performs an exclusive OR operation on the status register
//	with the source operand.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
//	| 0 | 0 | 0 | 0 | 1 | 0 | 1 | 0 | 0 | 1 | 1 | 1 | 1 | 1 | 0 | 0 |
//	|---------------------------------------------------------------|
//	|                     16 BITS IMMEDIATE DATA                    |
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
		int opcode = 0x0A7C;
		GenInstruction ins = null;
		
		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				EORISR(opcode);
			}
		};
		
		cpu.addInstruction(opcode, ins);
	}
	
	private void EORISR(int opcode) {
		long data = cpu.bus.read(cpu.PC + 2, Size.WORD);
		
	 	cpu.PC += 2;
		 	 
	 	long res = cpu.SR ^ data;
		res &= 0xFFFF;
	 	
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
