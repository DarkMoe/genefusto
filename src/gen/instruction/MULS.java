package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class MULS implements GenInstructionHandler {

	final Gen68 cpu;
	
	public MULS(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	MULS -- Signed multiply
//	MULU -- Unsigned multiply
//
//SYNOPSIS
//	MULS.W	<ea>,Dn		16*16->32
//	MULS.L	<ea>,Dn		32*32->32	68020+
//	MULS.L	<ea>,Dh:Dl	32*32->64	68020+
//
//	MULU.W	<ea>,Dn		16*16->32
//	MULU.L	<ea>,Dn		32*32->32	68020+
//	MULU.L	<ea>,Dh:Dl	32*32->64	68020+
//
//	Size = (Word)
//	Size = (Word, Long)			68020+
//
//FUNCTION
//	Multiply two signed (MULS) or unsigned (MULU) integers
//	to produce either a signed or unsigned, respectivly,
//	result.
//
//	This instruction has three forms. They are basically
//	word, long word, and quad word. The first version is
//	the only one available on a processore lower than a
//	68020. It will multiply two 16-bit integers are produce
//	a 32-bit result. The second will multiply two 32-bit
//	integers and produce a 32-bit result.
//
//FORMAT
//	In the case of MULS.W:
//	~~~~~~~~~~~~~~~~~~~~~                              <ea>
//	----------------------------------------=========================
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|---|---|---|-----------|-----------|
//	| 1 | 1 | 0 | 0 | REGISTER  | 1 | 1 | 1 |    MODE   | REGISTER  |
//	-----------------------------------------------------------------
//
//	In the case of MULU.W:
//	~~~~~~~~~~~~~~~~~~~~~                              <ea>
//	----------------------------------------=========================
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|---|---|---|-----------|-----------|
//	| 1 | 1 | 0 | 0 | REGISTER  | 0 | 1 | 1 |    MODE   | REGISTER  |
//	-----------------------------------------------------------------
//
//	"REGISTER" indicates the number of data register.
//
//	<ea> field specifies source operand, allowed addressing modes are:
//
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn       |000 |N° reg. Dn| |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An       | -  |    -     | |    Abs.L      |111 |  001   |
//	|-------------------------------| |-----------------------------|
//	|     (An)      |010 |N° reg. An| |   (d16,PC)    |111 |  010   |
//	|-------------------------------| |-----------------------------|
//	|     (An)+     |011 |N° reg. An| |   (d8,PC,Xi)  |111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|    -(An)      |100 |N° reg. An| |   (bd,PC,Xi)  |111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|    (d16,An)   |101 |N° reg. An| |([bd,PC,Xi],od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (d8,An,Xi)  |110 |N° reg. An| |([bd,PC],Xi,od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (bd,An,Xi)  |110 |N° reg. An| |    #data      |111 |  100   |
//	|-------------------------------| -------------------------------
//	|([bd,An,Xi]od) |110 |N° reg. An|
//	|-------------------------------|
//	|([bd,An],Xi,od)|110 |N° reg. An|
//	---------------------------------
//
//RESULT
//	X - Not affected.
//	N - Set if the result is negative. Cleared otherwise.
//	Z - Set if the result is zero. Cleared otherwise.
//	V - Set if overflow. Cleared otherwise.
//	C - Always cleared.
	
	@Override
	public void generate() {
		int base = 0xC1C0;
		GenInstruction ins = null;
		
		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				MULSWord(opcode);
			}
		};
		
		for (int register = 0; register < 8; register++) {
			for (int m = 0; m < 8; m++) {
				if (m == 1) {
					continue;
				}
				for (int r = 0; r < 8; r++) {
					if (m == 0b111 & r > 0b100) {
						continue;
					}
					int opcode = base | (register << 9) | (m << 3) | r;
					cpu.addInstruction(opcode, ins);
				}
			}
		}
		
	}
	
	private void MULSWord(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long data = o.getAddressingMode().getWord(o);
		if ((data & 0x8000) > 0) {
			data |= 0xFFFF_0000L;
		}
		
		long mult = cpu.getD(dataRegister) & 0xFFFF;
		if ((mult & 0x8000) > 0) {	// negative
			mult |= 0xFFFF_0000L;
		}
		
		long tot = mult * data;
		
		cpu.setDLong(dataRegister, tot);
		
		calcFlags(tot);
	}
	
	void calcFlags(long tot) {//TODO  overflow siempre clear ?
		if (tot < 0) {
			cpu.setN();
		} else {
			cpu.clearN();
		}
		if (tot == 0) {
			cpu.setZ();
		} else {
			cpu.clearZ();
		}
		cpu.clearV();
		cpu.clearX();
	}
	
}
