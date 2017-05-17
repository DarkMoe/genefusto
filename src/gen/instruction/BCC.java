package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class BCC implements GenInstructionHandler {

	final Gen68 cpu;
	
	public BCC(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	Bcc -- Conditional branch
//
//SYNOPSIS
//	Bcc	<label>
//
//	Size = (Byte, Word, Long*)
//
//	* 68020+ only
//
//FUNCTION
//	If condition true then program execution continues at:
//	(PC) + offset.
//	PC value is instruction address more two.
//	Offset is the relative value in bytes which separate Bcc instruction
//	of mentioned label.
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
//	|---|---|---|---|---------------|-------------------------------|
//	| 0 | 1 | 1 | 0 |   CONDITION   |         8 BITS OFFSET         |
//	|---------------------------------------------------------------|
//	|            16 BITS OFFSET, IF 8 BITS OFFSET = $00             |
//	|---------------------------------------------------------------|
//	|            32 BITS OFFSET, IF 8 BITS OFFSET = $FF             |
//	-----------------------------------------------------------------
//
//RESULT
//	None.
	
	@Override
	public void generate() {
		int base = 0x6000;
		GenInstruction insB = new GenInstruction() {
			@Override
			public void run(int opcode) {
				bccByte(opcode);
			}
		};
		GenInstruction insW = new GenInstruction() {
			@Override
			public void run(int opcode) {
				bccWord(opcode);
			}
		};
		for (int cc = 0; cc < 16; cc++) {
			int opcode = base + (cc << 8);
			cpu.addInstruction(opcode, insW);
			
			for (int offset = 1; offset < 256; offset++) {
				opcode = base + (cc << 8) + offset;
				cpu.addInstruction(opcode, insB);
			}
		}
		
	}
	
	private void bccByte(int opcode) {
		int cc = (opcode >> 8) & 0xF;

		boolean taken = cpu.evaluateBranchCondition(cc, Size.BYTE);

		long offset = opcode & 0xFF;
		if ((offset & 0x80) == 0x80) {
			offset |= 0xFFFF_FF00;
		}
		
		if (taken) {
			cpu.PC += offset;
		} else {
			// nada, el offset es un byte
		}
	}
	
	private void bccWord(int opcode) {
		int cc = (opcode >> 8) & 0xF;
		
		boolean taken = cpu.evaluateBranchCondition(cc, Size.WORD);

		long offset = cpu.bus.read(cpu.PC + 2, Size.WORD);
		
		if ((offset & 0x8000) == 0x8000) {
			offset |= 0xFFFF_0000;
		}
		
		if (taken) {
			cpu.PC += offset;
		} else {
			cpu.PC += 2;
		}
	}
	
}
