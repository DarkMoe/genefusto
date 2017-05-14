package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;

public class RTS implements GenInstructionHandler {

	final Gen68 cpu;
	
	public RTS(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	RTS -- Return from subroutine
//
//SYNOPSIS
//	RTS
//
//FUNCTION
//	PC is restored by SP.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
//	| 0 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 1 | 0 | 1 |
//	-----------------------------------------------------------------
//
//RESULT
//	None.
	
	@Override
	public void generate() {
		int base = 0x4E75;
		GenInstruction ins;
		
		ins = new GenInstruction() {
			
			@Override
			public void run(int opcode) {
				RSTpc(opcode);
			}
		};
		
		cpu.addInstruction(base, ins);
	}
	
	private void RSTpc(int opcode) {
		long newPC;
		
		if ((cpu.SR & 0x2000) == 0x2000) {
			newPC = cpu.bus.read(cpu.SSP) << 24;
			cpu.SSP++;
			newPC |= cpu.bus.read(cpu.SSP) << 16;
			cpu.SSP++;
			newPC |= cpu.bus.read(cpu.SSP) << 8;
			cpu.SSP++;
			newPC |= cpu.bus.read(cpu.SSP);
			cpu.SSP++;
			
			cpu.setALong(7, cpu.SSP);
			
			cpu.PC = newPC - 2;
		} else {
			newPC = cpu.bus.read(cpu.USP) << 24;
			cpu.USP++;
			newPC |= cpu.bus.read(cpu.USP) << 16;
			cpu.USP++;
			newPC |= cpu.bus.read(cpu.USP) << 8;
			cpu.USP++;
			newPC |= cpu.bus.read(cpu.USP);
			cpu.USP++;
			
			cpu.setALong(7, cpu.USP);
			
			cpu.PC = newPC - 2;
		}
		
	}

}
