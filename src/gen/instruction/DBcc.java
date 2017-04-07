package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class DBcc implements GenInstructionHandler {

	final Gen68 cpu;
	
	public DBcc(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	DBcc -- Decrement and branch conditionally
//
//SYNOPSIS
//	DBcc	Dn,<label>
//
//	Size of offset = (Word)
//
//FUNCTION
//	Controls a loop of instructions. The parameters are: a
//	condition code, a data register (counter), and an offset value.
//	The instruction first tests the condition (for termination); if it
//	is true, no operation is performed. If the termination condition is
//	not true, the low-order 16 bits of the counter are decremented by
//	one. If the result is -1, execution continues at the next
//	instruction, otherwise, execution continues at the specified
//	address.
//
//	Condition code 'cc' specifies one of the following:
//0000 F  False            Z = 1      1000 VC oVerflow Clear   V = 0
//0001 T  True             Z = 0      1001 VS oVerflow Set     V = 1
//0010 HI HIgh             C + Z = 0  1010 PL PLus             N = 0
//0011 LS Low or Same      C + Z = 1  1011 MI MInus            N = 1
//0100 CC Carry Clear      C = 0      1100 GE Greater or Equal N (+) V = 0
//0101 CS Carry Set        C = 1      1101 LT Less Than        N (+) V = 1
//0110 NE Not Equal        Z = 0      1110 GT Greater Than     Z + (N (+) V) = 0
//0111 EQ EQual            Z = 1      1111 LE Less or Equal    Z + (N (+) V) = 1
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---------------|---|---|---|---|---|-----------|
//	| 0 | 1 | 0 | 1 |   CONDITION   | 1 | 1 | 0 | 0 | 1 | REGISTER  |
//	|---------------------------------------------------------------|
//	|                      16 BITS OFFSET (d16)                     |
//	-----------------------------------------------------------------
//
//	"CONDITION" is one of the condition code given some lines before.
//	"REGISTER" is the number of data register.
//	Offset is the relative gap in byte to do branching.
//
//RESULT
//	Not affected.
	
	@Override
	public void generate() {
		int base = 0x50C8;
		GenInstruction ins = null;
		
		for (int cc = 0; cc < 16; cc++) {
			ins = new GenInstruction() {
				@Override
				public void run(int opcode) {
					DBccWord(opcode);
				}
			};
			for (int register = 0; register < 8; register++) {
				int opcode = base + ((cc << 8) | (register));
				cpu.addInstruction(opcode, ins);
			}
		}
	}
	
	private void DBccWord(int opcode) {
		int condition = (opcode >> 8) & 0xF;
		int register = opcode & 0x7;
	
		long offset = cpu.bus.read(cpu.PC + 2) << 8;
		offset 	   |= cpu.bus.read(cpu.PC + 3);
	
//		cpu.PC += 2;
		
		long counter = cpu.getD(register) & 0xFFFF;
		
		if (condition == 0b0001) {	//	override para que no haga un jump
			if (counter != 0) {
				if ((offset & 0x8000) > 0) {
					offset = offset - 0xFFFF - 1;	// para que sea signed, TODO arreglar esto
				}
				cpu.PC += offset;
			} else {
				cpu.PC += 2;
			}
			counter = (counter - 1) & 0xFFFF;
			cpu.setDWord(register, counter);
		} else {
			boolean condTrue = cpu.evaluateBranchCondition(condition, Size.WORD);
			if (condTrue) {
				cpu.PC += 2;
			} else {
				if (counter != 0) {
					if ((offset & 0x8000) > 0) {
						offset = offset - 0xFFFF - 1;	// para que sea signed, TODO arreglar esto
					}
					cpu.PC += offset;
				} else {
					cpu.PC += 2;
				}
				counter = (counter - 1) & 0xFFFF;
				cpu.setDWord(register, counter); 
			}
		}
		
	}
	
}
