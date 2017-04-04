package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class ROXR implements GenInstructionHandler {

	final Gen68 cpu;
	
	public ROXR(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	ROXL, ROXD -- Rotate left with extend and rotate right with extend
//
//SYNOPSIS
//	ROXd	Dx,Dy
//	ROXd	#<data>,Dy
//	ROXd	<ea>
//	where d is direction, L or R
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//        A rotation is made on destination operand bits.
//        Rotation uses bit X.
//
//                  .-------->-----------.
//	ROXL:     |                    |
//	      C <------ OPERAND <- X --'
//	                  <---	       
//
//	ROXR:    ,---------<-------.
//	         |                 |
//	         `-- X -> OPERAND -----> C
//	                   --->
//
//FORMAT
//	In the case of the rotating of a register:
//	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|---|-------|---|---|---|-----------|
//	| 1 | 1 | 1 | 0 |  NUMBER/  |dr |  SIZE |i/r| 1 | 0 | REGISTER  |
//	|   |   |   |   |  REGISTER |   |       |   |   |   |           |
//	-----------------------------------------------------------------
//
//	In the case of the rotating of a memory area:
//	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|-----------|-----------|
//	| 1 | 1 | 1 | 0 | 0 | 1 | 0 |dr | 1 | 1 |    MODE   | REGISTER  |
//	----------------------------------------=========================
//	                                                  <ea>
//
//NUMBER/REGISTER
//	Specifies number of rotating or number of register which contents
//	the number of rotating.
//	If i/r = 0, number of rotating is specified in the instruction as
//	immediate data
//	If i/r = 1, it's specified in the data register.
//	If dr = 0, right rotating
//	If dr = 1, left rotating
//
//SIZE
//	00->one Byte operation
//	01->one Word operation
//	10->one Long operation
//
//REGISTER
//	For a register rotating:
//	Indicates the number of data register on which rotating is applied.
//
//	For a memory rotating:
//	<ea> indicates operand which should be rotated.
//	Only addressing modes relatives to memory are allowed:
//
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn       | -  |     -    | |    Abs.W      |111 |  000   |
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
//	X - Set by the last bit out of operand.
//            Not changed if rotation is zero.
//	N - Set if the result is negative. Cleared otherwise.
//	Z - Set if the result is zero. Cleared otherwise.
//	V - Always cleared
//	C - Set according to the last bit shifted out of the operand.
	
	@Override
	public void generate() {
		int base = 0xE010;
		GenInstruction ins = null;
		
		for (int s = 0; s < 3; s++) {
			if (s == 0b00) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ROXRRegisterByte(opcode);
					}
				};
			} else if (s == 0b01) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ROXRRegisterWord(opcode);
					}
				};
			} else if (s == 0b10) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ROXRRegisterLong(opcode);
					}
				};
			}
			
			for (int ir = 0; ir < 2; ir++) {
				for (int r = 0; r < 8; r++) {
					for (int nReg = 0; nReg < 8; nReg++) {
						int opcode = base | (nReg << 9) | (s << 6) | (ir << 5) | r;
						cpu.addInstruction(opcode, ins);
					}
				}
			}
		}
	}
	
	private void ROXRRegisterByte(int opcode) {
		throw new RuntimeException("AA");
	}
	
	private void ROXRRegisterWord(int opcode) {
		int register = (opcode & 0x7);
		boolean ir = cpu.bitTest(opcode, 5);
		int numRegister = (opcode >> 9) & 0x7;
		
		long toShift;
		if (!ir) {
			if (numRegister == 0) {
				numRegister = 8;
			}
			toShift = numRegister;
		} else {
			toShift = cpu.getD(numRegister);
		}
		
		long data = cpu.getD(register) & 0xFFFF;
		
		int extended = cpu.isX() ? 0x8000 : 0;
		long res = (data >> toShift) | extended;
		cpu.setDWord(register, res);
		
		boolean carry = false;
		if (toShift != 0) {
			if (((data >> toShift - 1) & 1) > 0) {
				carry = true;
			}
		}
		
		calcFlags(res, Size.WORD.getMsb(), 0xFFFF, carry);
	}

	private void ROXRRegisterLong(int opcode) {
		throw new RuntimeException("AA");
	}

	void calcFlags(long data, long msb, long maxSize, boolean carry) {
		long wrapped = data & maxSize;
		if (wrapped == 0) {
			cpu.setZ();
		} else {
			cpu.clearZ();
		}
		if ((wrapped & msb) > 0) {
			cpu.setN();
		} else {
			cpu.clearN();
		}

		cpu.clearV();
		
		if (carry) {
			cpu.setC();
			cpu.setX();
		} else {
			cpu.clearC();
			cpu.clearX();
		}
	}
	
}
