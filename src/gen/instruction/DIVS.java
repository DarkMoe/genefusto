package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class DIVS implements GenInstructionHandler {

	final Gen68 cpu;
	
	public DIVS(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	DIVS, DIVSL -- Signed divide
//
//SYNOPSIS
//	DIVS.W	<ea>,Dn     32/16 -> 16r:16q
//
//	Size = (Word, Long)
//
//FUNCTION
//	Divides the signed destination operand by the signed source
//	operand and stores the signed result in the destination.
//
//	The instruction has a word form and three long forms. For the
//	word form, the destination operand is a long word and the source
//	operand is a word. The resultant quotient is placed in the lower
//	word of the destination and the resultant remainder is placed in the
//	upper word of the destination. The sign of the remainder is the
//	same as the sign of the dividend.
//
//FORMAT
//	In the case of DIVS.W:
//	~~~~~~~~~~~~~~~~~~~~~                              <ea>
//	----------------------------------------=========================
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|---|---|---|-----------|-----------|
//	| 1 | 0 | 0 | 0 | REGISTER  | 1 | 1 | 1 |    MODE   | REGISTER  |
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
//	X - Not affected
//	N - Set if the quotient is negative, cleared otherwise. Undefined if
//	    overflow or divide by zero occurs.
//	Z - Set if the quotient is zero, cleared otherwise. Undefined if
//	    overflow or divide by zero occurs.
//	V - Set if overflow occurs, cleared otherwise. Undefined if divide by
//	    zero occurs.
//	C - Always cleared.
//
//	Notes:
//	1. If divide by zero occurs, an exception occurs.
//	2. If overflow occurs, neither operand is affected.
	
	@Override
	public void generate() {
		int base = 0x81C0;
		GenInstruction ins = null;
		
		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				DIVSWord(opcode);
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
	
	private void DIVSWord(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		int s = (int) o.getAddressingMode().getWord(o);
		if ((s & 0x8000) > 0) {
			s |= 0xFFFF_0000L;
		}
		
		int d = (int) cpu.getDLong(dataRegister);

		if (s == 0) {
			throw new RuntimeException("div por 0");
		}
		
		int quot = d / s;

		if (quot > 32767 || quot < -32768) {
			//Overflow
			cpu.setV();
		} else {
			long remain = (d % s) & 0xFFFF;
			long result = (remain << 16) | (quot & 0xFFFF) ;
			cpu.setDLong(dataRegister, result);

			if ((quot & 0x8000) != 0) {
				cpu.setN();
				cpu.clearZ();
			} else {
				cpu.clearN();

				if (quot == 0) {
					cpu.setZ();
				} else {
					cpu.clearZ();
				}
			}

			cpu.clearV();
			cpu.clearC();
		}
	}
	
	void calcFlags(long tot) {//	TODO pasar los calculos aca
	}
	
}
