package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class NEG implements GenInstructionHandler {

	final Gen68 cpu;
	
	public NEG(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	NEG -- Negate
//
//SYNOPSIS
//	NEG	<ea>
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//	The operand specified by <ea> is subtracted from
//	zero. The result is stored in <ea>.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|-------|-----------|-----------|
//	| 0 | 1 | 0 | 0 | 0 | 1 | 0 | 0 | SIZE  |    MODE   | REGISTER  |
//	----------------------------------------=========================
//	                                                   <ea>
//
//SIZE
//	00->Byte.
//	01->Word.
//	10->Long.
//
//REGISTER
//	<ea> specifies destination operand, addressing modes allowed are:
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn       |000 |N° reg. Dn| |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An       | -  |    -     | |    Abs.L      |111 |  001   |
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
//RESULT
//	X - Set the same as the carry bit.
//	N - Set if the result is negative, otherwise cleared.
//	Z - Set if the result is zero, otherwise cleared.
//	V - Set if overflow, otherwise cleared.
//	C - Cleared if the result is zero, otherwise set.
	
	@Override
	public void generate() {
		int base = 0x4400;
		GenInstruction ins = null;
		
		for (int s = 0; s < 3; s++) {
			if (s == 0b00) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						NEGByte(opcode);
					}
					
				};
			} else if (s == 0b01) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						NEGWord(opcode);
					}
					
				};
			} else if (s == 0b10) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						NEGLong(opcode);
					}
					
				};
			}
			
			for (int m = 0; m < 8; m++) {
				if (m == 1) {
					continue;
				}
				for (int r = 0; r < 8; r++) {
					if (m == 0b111 && r > 0b001) {
						continue;
					}
					int opcode = base | (s << 6) | (m << 3) | r;
					cpu.addInstruction(opcode, ins);
				}
			}	
		}
	}
	
	private void NEGByte(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.BYTE, mode, register);
		long data = o.getAddressingMode().getByte(o);
		
		long res = 0 - data;

		boolean overflow = false;
		if (((res & 0x80) & (data & 0x80)) > 0) {	//	solo hay overflow si ambos parametros son negativos
			overflow = true;
		}
		
		cpu.writeKnownAddressingMode(o, res, Size.BYTE);
				
		calcFlags(res, Size.BYTE.getMsb(), 0xFF, overflow);
	}
	
	private void NEGWord(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long data = o.getAddressingMode().getWord(o);
		
		long res = 0 - data;

		boolean overflow = false;
		if (((res & 0x8000) & (data & 0x8000)) > 0) {	//	solo hay overflow si ambos parametros son negativos
			overflow = true;
		}
		
		cpu.writeKnownAddressingMode(o, res, Size.WORD);
				
		calcFlags(res, Size.WORD.getMsb(), 0xFFFF, overflow);
	}

	private void NEGLong(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long data = o.getAddressingMode().getWord(o);
		
		long res = 0 - data;

		boolean overflow = false;
		if (((res & 0x8000_0000) & (data & 0x8000_0000)) > 0) {	//	solo hay overflow si ambos parametros son negativos
			overflow = true;
		}
		
		cpu.writeKnownAddressingMode(o, res, Size.LONG);
				
		calcFlags(res, Size.LONG.getMsb(), 0xFFFF_FFFFL, overflow);
	}
	
	void calcFlags(long data, long msb, long max, boolean overflow) {
		long wrap = data & max;
		if (wrap == 0) {
			cpu.setZ();
			cpu.clearC();
			cpu.clearX();
		} else {
			cpu.clearZ();
			cpu.setC();
			cpu.setX();
		}
		if ((data & msb) > 0) {
			cpu.setN();
		} else {
			cpu.clearN();
		}
		
		if (overflow) {
			cpu.setV();
		} else {
			cpu.clearV();
		}
	}
	
}
