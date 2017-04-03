package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class EORI implements GenInstructionHandler {

	final Gen68 cpu;
	
	public EORI(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	EORI -- Exclusive OR immediate
//
//SYNOPSIS
//	EORI	#<data>,<ea>
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//	Performs an exclusive OR operation on the destination operand
//	with the source operand.
//
//FORMAT
//	                                                  <ea>
//	----------------------------------------=========================
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|-------|-----------|-----------|
//	| 0 | 0 | 0 | 0 | 1 | 0 | 1 | 0 | SIZE  |    MODE   | REGISTER  |
//	|-------------------------------|-------------------------------|
//	|   16 BITS IMMEDIATE DATA      |     8 BITS IMMEDIATE DATA     |
//	|---------------------------------------------------------------|
//	|                     32 BITS IMMEDIATE DATA                    |
//	-----------------------------------------------------------------
//
//SIZE
//	00->8 bits operation.
//	01->16 bits operation.
//	10->32 bits operation.
//
//REGISTER
//	Immediate data is placed behind the word of operating code of
//	the instruction on 8, 16 or 32 bits.
//
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
//	X - Not Affected
//	N - Set to the value of the most significant bit.
//	Z - Set if the result is zero.
//	V - Always cleared
//	C - Always cleared
	
	@Override
	public void generate() {
		int base = 0x0A00;
		GenInstruction ins;
		
		for (int s = 0; s < 3; s++) {
			if (s == 0b00) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						EORIByte(opcode);
					}

				};
			} else if (s == 0b01) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						EORIWord(opcode);
					}

				};
			} else {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						EORILong(opcode);
					}

				};
			}
		
			for (int m = 0; m < 8; m++) {
				if (m == 1) {
					continue;
				}
				for (int r = 0; r < 8; r++) {
					if ((m == 7) && r > 0b001) {
						continue;
					}
					
					int opcode = base | (s << 6) | (m << 3) | r;
					cpu.addInstruction(opcode, ins);
				}
			}
		}
	}
	
	private void EORIByte(int opcode) {
		long data = cpu.bus.read(cpu.PC + 2) << 8;
			data |= cpu.bus.read(cpu.PC + 3);
		data = data & 0xFF;
		
		int destReg = (opcode & 0x7);
		int destMode = (opcode >> 3) & 0x7;
		
		data = (cpu.getD(destReg) & 0xFF) ^ data;
		data &= 0xFFFF_FFFFL;
				
		cpu.writeAddressingMode(Size.BYTE, cpu.PC + 2, data, destMode, destReg);
		
		cpu.PC += 2;		
		
		calcFlags(data, Size.BYTE.getMsb());
	}

	private void EORIWord(int opcode) {
		long data = cpu.bus.read(cpu.PC + 2) << 8;
			data |= cpu.bus.read(cpu.PC + 3);
		
		int destReg = (opcode & 0x7);
		int destMode = (opcode >> 3) & 0x7;
		
		data = (cpu.getD(destReg) & 0xFFFF) ^ data;
		data &= 0xFFFF_FFFFL;
				
		cpu.writeAddressingMode(Size.WORD, cpu.PC + 2, data, destMode, destReg);
		
		cpu.PC += 2;
		
		calcFlags(data, Size.WORD.getMsb());
	}
	
	private void EORILong(int opcode) {
		long data  = (cpu.bus.read(cpu.PC + 2)) << 24;
	 	 	 data |= (cpu.bus.read(cpu.PC + 3)) << 16;
	 	 	 data |= (cpu.bus.read(cpu.PC + 4)) << 8;
	 	 	 data |= (cpu.bus.read(cpu.PC + 5));
		
		int destReg = (opcode & 0x7);
		int destMode = (opcode >> 3) & 0x7;
		
		data = cpu.getD(destReg) ^ data;
		data &= 0xFFFF_FFFFL;
		
		cpu.writeAddressingMode(Size.LONG, cpu.PC + 2, data, destMode, destReg);
				
		cpu.PC += 4;
		
		calcFlags(data, Size.LONG.getMsb());
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
