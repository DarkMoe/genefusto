package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;

public class MOVE_TO_FROM_USP implements GenInstructionHandler {

	final Gen68 cpu;
	
	public MOVE_TO_FROM_USP(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	MOVE USP -- Move to/from user stack pointer (privileged)
//
//SYNOPSIS
//	MOVE	USP,An
//	MOVE	An,USP
//
//	Size = (Long)
//
//FUNCTION
//	The contents of the user stack pointer are transfered either to
//	or from the specified address register.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|---|---|---|-----------|
//	| 0 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 0 | 1 | 1 | 0 |dr | REGISTER  |
//	-----------------------------------------------------------------
//
//	"REGISTER" indicates the number of address register.
//
//	dr specifies move direction:
//	0->An to USP
//	1->USP to An
//
//RESULT
//	None.
	
	@Override
	public void generate() {
		int base = 0x4E60;
		GenInstruction ins = null;
		
		for (int dr = 0; dr < 2; dr++) {
			if (dr == 0) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						MOVEToUSP(opcode);
					}
				};
			} else {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						MOVEFromUSP(opcode);
					}
				};
			}
			
			for (int r = 0; r < 8; r++) {
				int opcode = base + (dr << 3) | (r);
				cpu.addInstruction(opcode, ins);
			}
		}
	}
	
	private void MOVEToUSP(int opcode) {
		if ((cpu.SR & 0x2000) != 0x2000) {
			throw new RuntimeException("NO PRIVI");
		}
		
		int register = opcode & 0x7;

		cpu.USP = cpu.getA(register);
	}
	
	private void MOVEFromUSP(int opcode) {
		if ((cpu.SR & 0x2000) != 0x2000) {
			throw new RuntimeException("NO PRIVI");
		}
		
		int register = opcode & 0x7;

		cpu.setALong(register, cpu.USP);
	}
	
}
