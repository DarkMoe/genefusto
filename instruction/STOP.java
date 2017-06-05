package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class STOP implements GenInstructionHandler {

	final Gen68 cpu;
	
	public STOP(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	STOP -- Stop processor execution (PRIVILEGED)
//
//SYNOPSIS
//	STOP	#<data:16>
//
//FUNCTION
//	Immediate data is moved to SR. PC is set to next instruction,
//	and the processor stops fetch and execution of instruction.
//	Execution restarts if if a TRACE exception, an interruption, or
//	a RESET takes place.
//	When STOP is executed, a TRACE exception is generated (if T = 1).
//	An interruption is allowed if it level is higher than current one.
//	An external RESET always will generate a RESET exception.
//	If bit S is set to zero by the immediate data, execution of this
//	instruction will generate a "privilege violation".
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
//	| 0 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 0 | 1 | 0 |
//	|---------------------------------------------------------------|
//	|                    16 BITS IMMEDIATE DATA                     |
//	-----------------------------------------------------------------
//
//RESULT
//	SR is set according to immediate data.
	
	@Override
	public void generate() {
		int base = 0x4E72;
		GenInstruction ins;
		
		ins = new GenInstruction() {
			
			@Override
			public void run(int opcode) {
				STOPOp(opcode);
			}
		};
		
		cpu.addInstruction(base, ins);
	}
	
	private void STOPOp(int opcode) {
		long data = cpu.bus.read(cpu.PC + 2, Size.WORD);
		
	 	cpu.PC += 2;
	 	
	 	if ((data & 0x2000) == 0) {
	 		//	TODO generate violation exception
	 		throw new RuntimeException("VIOLATION !");
	 	}
	 	
	 	cpu.SR = (int) (data & 0xFFFF);
	 	cpu.stop = true;
	}

}
