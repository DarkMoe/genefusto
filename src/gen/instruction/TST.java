package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class TST implements GenInstructionHandler {

	final Gen68 cpu;
	
	public TST(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	TeST operand for zero
//
//	NAME
//		TST -- Test operand for zero
//
//	SYNOPSIS
//		TST	<ea>
//
//		Size = (Byte, Word, Long)
//
//	FUNCTION
//		Operand is compared with zero. Flags are set according to the result.
//
//	FORMAT
//		-----------------------------------------------------------------
//		|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//		|---|---|---|---|---|---|---|---|-------|-----------|-----------|
//		| 0 | 1 | 0 | 0 | 1 | 0 | 1 | 0 | SIZE  |   MODE    |  REGISTER |
//		----------------------------------------=========================
//	                                                          <ea>
//
//	SIZE
//		00->one Byte operation
//		01->one Word operation
//		10->one Long operation
//
//	REGISTER
//		<ea> is destination, if size is 16 or 32 bits then all addressing
//		modes are allowed. If size is 8 bits, allowed addressing modes are:
//		--------------------------------- -------------------------------
//		|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//		|-------------------------------| |-----------------------------|
//		|      Dn       |000 |N° reg. Dn| |    Abs.W      |111 |  000   |
//		|-------------------------------| |-----------------------------|
//		|      An       | -  |    -     | |    Abs.L      |111 |  001   |
//		|-------------------------------| |-----------------------------|
//		|     (An)      |010 |N° reg. An| |   (d16,PC)    |111 |  010   |
//		|-------------------------------| |-----------------------------|
//		|     (An)+     |011 |N° reg. An| |   (d8,PC,Xi)  |111 |  011   |
//		|-------------------------------| |-----------------------------|
//		|    -(An)      |100 |N° reg. An| |   (bd,PC,Xi)  |111 |  011   |
//		|-------------------------------| |-----------------------------|
//		|    (d16,An)   |101 |N° reg. An| |([bd,PC,Xi],od)|111 |  011   |
//		|-------------------------------| |-----------------------------|
//		|   (d8,An,Xi)  |110 |N° reg. An| |([bd,PC],Xi,od)|111 |  011   |
//		|-------------------------------| |-----------------------------|
//		|   (bd,An,Xi)  |110 |N° reg. An| |    #data      | -  |   -    |
//		|-------------------------------| -------------------------------
//		|([bd,An,Xi]od) |110 |N° reg. An|
//		|-------------------------------|
//		|([bd,An],Xi,od)|110 |N° reg. An|
//		---------------------------------
//
//	RESULT
//		X - Not affected.
//		N - Set if the result is negative. Cleared otherwise.
//		Z - Set if the result is zero. Cleared otherwise.
//		V - Always cleared.
//		C - Always cleared.

	@Override
	public void generate() {
		GenInstruction ins = null;
		Size size = null;
		int base = 0;
		
		for (int s = 0; s < 3; s++) {
			if (s == 0) {
				base = 0x4A00;
				size = Size.byt;
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						TSTByte(opcode);
					}
				};
				
			} else if (s == 0b01) {
				size = Size.word;
				base = 0x4A40;
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						TSTWord(opcode);
					}
				};
				
			} else if (s == 0b10) {
				size = Size.longW;
				base = 0x4A80;
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						TSTLong(opcode);
					}
				};
			}
			
			for (int m = 0; m < 8; m++) {
				for (int r = 0; r < 8; r++) {
					if (size == Size.byt) {
						if (m == 1) {
							continue;
						}
						if (m == 0b111 && r > 0b011) {
							continue;
						}
					}

					int opcode = base + ((m << 3) | r);
					cpu.addInstruction(opcode, ins);
				}
			}
		}
		
	}
	
	private void TSTByte(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.byt, mode, register);
		long data = o.getAddressingMode().getByte(o);
		
		calcFlags(data, Size.byt.getMsb());
	}
	
	private void TSTWord(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.word, mode, register);
		long data = o.getAddressingMode().getWord(o);
		
		calcFlags(data, Size.word.getMsb());
	}
	
	private void TSTLong(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.longW, mode, register);
		long data = o.getAddressingMode().getLong(o);
		
		calcFlags(data, Size.longW.getMsb());
	}
	
	void calcFlags(long data, long msb) {
		if (data == 0) {
			cpu.setZ();
		} else {
			cpu.clearZ();
		}
		if ((data & msb) > 0) {
			cpu.setN();
		} else {
			cpu.clearN();
		}
		
		cpu.clearV();
		cpu.clearC();
	}
	
}
