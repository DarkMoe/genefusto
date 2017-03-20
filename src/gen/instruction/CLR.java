package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class CLR implements GenInstructionHandler {

	final Gen68 cpu;
	
	public CLR(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	CLR -- Clear
//
//SYNOPSIS
//	CLR	<ea>
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//	Clears the destination operand to zero.
//
//	On an MC68000 and MC68HC000, a CLR instruction does both a
//	read and a write to the destination. Because of this, this
//	instruction should never be used on custom chip registers.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|-------|-----------|-----------|
//	| 0 | 1 | 0 | 0 | 0 | 0 | 1 | 0 |  SIZE |    MODE   | REGISTER  |
//	----------------------------------------=========================
//	                                                  <ea>
//
//REGISTER
//	<ea> specifies destination operand, addressing modes allowed are:
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn       |000 |N° reg. Dn| |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An       | -  |     -    | |    Abs.L      |111 |  001   |
//	|-------------------------------| |-----------------------------|
//	|     (An)      |010 |N° reg. An| |   (d16,PC)    | -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|     (An)+     |011 |N° reg. An| |   (d8,PC,Xi)  | -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|    -(An)      |100 |N° reg. An| |   (bd,PC,Xi)  | -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|    (d16,An)   |101 |N° reg. An| |([bd,PC,Xi],od)| -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|   (d8,An,Xi)  |110 |N° reg. An| |([bd,PC],Xi,od)| -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|   (bd,An,Xi)  |110 |N° reg. An| |    #data      | -  |   -    |
//	|-------------------------------| -------------------------------
//	|([bd,An,Xi]od) |110 |N° reg. An|
//	|-------------------------------|
//	|([bd,An],Xi,od)|110 |N° reg. An|
//	---------------------------------
//
//SIZE
//	00->one Byte operation
//	01->one Word operation
//	10->one Long operation
//
//RESULT
//	X - Not affected
//	N - Always cleared
//	Z - Always set
//	V - Always cleared
//	C - Always cleared
	
	@Override
	public void generate() {
		int base = 0x4200;
		GenInstruction ins = null;
		
		for (int s = 0; s < 3; s++) {
			if (s == 0b00) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						CLRByte(opcode);
					}

				};
			} else if (s == 0b01) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						CLRWord(opcode);
					}

				};
			} else if (s == 0b10) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						CLRLong(opcode);
					}

				};
			}
			
			for (int m = 0; m < 8; m++) {
				if (m == 0b001) {
					continue;
				}
				for (int r = 0; r < 8; r++) {
					if (m == 0b111 && r > 0b001) {
						continue;
					}
					int opcode = base | (s << 6) | (m <<3) | r;
					cpu.addInstruction(opcode, ins);
				}
			}
		}
	}
	
	private void CLRByte(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = opcode & 0x7;
		
		cpu.writeAddressingMode(Size.byt, cpu.PC + 2, 0, mode, register);
		
		calcFlags();
	}
	
	private void CLRWord(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = opcode & 0x7;
		
		//	lectura dummy
		Operation o = cpu.resolveAddressingMode(Size.word, mode, register);
		long data = o.getAddressingMode().getWord(o);
		cpu.writeKnownAddressingMode(o, 0, Size.word);
		
		calcFlags();
	}
	
	private void CLRLong(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = opcode & 0x7;
		
		//	lectura dummy
		Operation o = cpu.resolveAddressingMode(Size.longW, mode, register);
		long data = o.getAddressingMode().getLong(o);
		cpu.writeKnownAddressingMode(o, 0, Size.longW);
		
		calcFlags();
	}

	void calcFlags() {
		cpu.setZ();
		cpu.clearN();
		cpu.clearV();
		cpu.clearC();
	}
	
}
