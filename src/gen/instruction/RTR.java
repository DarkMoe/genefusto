package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;

public class RTR implements GenInstructionHandler {

	final Gen68 cpu;
	
	public RTR(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	RTR -- Return and restore condition code register
//
//SYNOPSIS
//	RTR
//
//FUNCTION
//	CCR and PC are restored by SP.
//	Supervisor byte of SR isn't affected.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
//	| 0 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 1 | 1 | 1 |
//	-----------------------------------------------------------------
//
//RESULT
//	CCR is set following to the restored word taken from SP.
	
	@Override
	public void generate() {
		int base = 0x4E77;
		GenInstruction ins;
		
		ins = new GenInstruction() {
			
			@Override
			public void run(int opcode) {
				RTRpc(opcode);
			}
		};
		
		cpu.addInstruction(base, ins);
	}
	
	private void RTRpc(int opcode) {
		long newPC;
		long newSR;
		
		if ((cpu.SR & 0x2000) == 0x2000) {
			newSR = cpu.bus.read(cpu.SSP) << 16;
			cpu.SSP++;
			newSR |= cpu.bus.read(cpu.SSP);
			cpu.SSP++;
			
			int flags = (int) (newSR & 0x1F);	// solo se usa el byte inferior con los 5 flags
			cpu.SR = (int) ((cpu.SR & 0xFFE0) | flags);
			
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
			newSR = cpu.bus.read(cpu.USP) << 16;
			cpu.USP++;
			newSR |= cpu.bus.read(cpu.USP);
			cpu.USP++;
			
			int flags = (int) (newSR & 0x1F);	// solo se usa el byte inferior con los 5 flags
			cpu.SR = (int) ((cpu.SR & 0xFFE0) | flags);
			
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
