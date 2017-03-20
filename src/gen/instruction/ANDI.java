package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class ANDI implements GenInstructionHandler {

	final Gen68 cpu;
	
	public ANDI(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	ANDI -- Logical AND immediate
//
//SYNOPSIS
//	ANDI	#<data>,<ea>
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//	Performs a bit-wise AND operation with the immediate data and
//	the destination operand and stores the result in the destination. The
//	size of ther operation can be specified as byte, word, or long. The
//	size of the immediate data matches the operation size.
//
//FORMAT
//                                                          <ea>
//	----------------------------------------=========================
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|-------|-----------|-----------|
//	| 0 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | SIZE  |   MODE    |  REGISTER |
//	|---------------------------------------------------------------|
//	| 16 BITS DATA (with last Byte) |          8 BITS DATA          |
//	|---------------------------------------------------------------|
//	|             32 BITS DATA (included last Word)                 |
//	-----------------------------------------------------------------
//
//SIZE
//	00->one Byte operation
//	01->one Word operation
//	10->one Long operation
//
//REGISTER
//	<ea> is always destination, addressing modes are the followings:
//
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
//RESULT
//	X - Not affected
//	N - Set if the most-significant bit of the result was set. Cleared
//	    otherwise.
//	Z - Set if the result was zero. Cleared otherwise.
//	V - Always cleared.
//	C - Always cleared.

	@Override
	public void generate() {
		int base = 0x0200;
		GenInstruction ins = null;
		
		for (int s = 0; s < 3; s++) {
			if (s == 0b00) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ANDIByte(opcode);
					}
				};
			} else if (s == 0b01) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ANDIWord(opcode);
					}
				};
			} else if (s == 0b10) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ANDILong(opcode);
					}
				};
			}
			for (int m = 0; m < 8; m++) {
				if (m == 1) {
					continue;
				}
				for (int r = 0; r < 8; r++) {
					if (m == 0b111 & r > 0b001) {
						continue;
					}
					int opcode = base + ((s << 6) | (m << 3) | r);
					cpu.addInstruction(opcode, ins);
				}
			}
		}
		
	}
	
	private void ANDIByte(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.byt, mode, register);
		long data = o.getAddressingMode().getByte(o);
		
		long toAnd  = (cpu.bus.read(cpu.PC + 2)) << 8;
		 	 toAnd |= (cpu.bus.read(cpu.PC + 3));
		 	 toAnd = toAnd & 0xFF;	//	ocupa 2 bytes, pero solo se toma el ultimo
		
		long res = data & toAnd;
		cpu.writeAddressingMode(Size.byt, 0, res, mode, register);
		 	 
		cpu.PC += 2;
		
		calcFlags(res, Size.byt.getMsb());
	}
	
	private void ANDIWord(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.word, mode, register);
		long data = o.getAddressingMode().getWord(o);
		
		long toAnd  = (cpu.bus.read(cpu.PC + 2)) << 8;
		 	 toAnd |= (cpu.bus.read(cpu.PC + 3));
		
		long res = data & toAnd;
		cpu.writeAddressingMode(Size.word, 0, res, mode, register);
		 	 
		cpu.PC += 2;
		
		calcFlags(res, Size.word.getMsb());
	}
	
	private void ANDILong(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.longW, mode, register);
		long data = o.getAddressingMode().getLong(o);
		
		long toAnd  = (cpu.bus.read(cpu.PC + 2)) << 24;
		 	 toAnd |= (cpu.bus.read(cpu.PC + 3)) << 16;
		     toAnd |= (cpu.bus.read(cpu.PC + 4)) << 8;
		     toAnd |= (cpu.bus.read(cpu.PC + 5));
		
		long res = data & toAnd;
		cpu.writeAddressingMode(Size.longW, 0, res, mode, register);
		 	 
		cpu.PC += 4;
		
		calcFlags(res, Size.longW.getMsb());
	}
	
	void calcFlags(long data, int msb) {
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
