package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;

public class NOP implements GenInstructionHandler {

	final Gen68 cpu;
	
	public NOP(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	NOP -- No operation
//
//SYNOPSIS
//	NOP
//
//FUNCTION
//	Nothing happens! This instruction will basically wait until
//	all pending bus activity is completed. This allows
//	synchronization of the pipeline	and prevents instruction overlap.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
//	| 0 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 0 | 0 | 1 |
//	-----------------------------------------------------------------
//
//RESULT
//	None.
	
	@Override
	public void generate() {
		int base = 0x4E71;
		GenInstruction ins;
		
		ins = new GenInstruction() {
			
			@Override
			public void run(int opcode) {
				NOPop(opcode);
			}
		};
		
		cpu.addInstruction(base, ins);
	}
	
	private void NOPop(int opcode) {
		// TODO sincronizar pipelines
	}

}
