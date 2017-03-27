package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class ROR implements GenInstructionHandler {

	final Gen68 cpu;
	
	public ROR(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	ROL, ROR -- Rotate left and rotate right
//
//SYNOPSIS
//	ROd	Dx,Dy
//	ROd	#<data>,Dy
//	ROd	<ea>
//	where d is direction, L or R
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//	Rotate the bits of the operand in the specified direction.
//	The rotation count may be specified in two different ways:
//
//	1. Immediate - the rotation count is specified in the instruction
//
//	2. Register  - the rotation count is contained in a data register
//	               specified in the instruction
//
//	For a register, the size may be byte, word, or long, but for a memory
//	location, the size must be a word. The rotation count is also
//	restricted to one for a memory location.
//
//                  .-------->--------.
//	ROL:      |                 |
//	      C <------ OPERAND <---'
//	                  <---	       
//
//	ROR:     ,-------<-------.
//	         |               |
//	         `--> OPERAND -----> C
//	                --->
//
//FORMAT
//	In the case of the rotating of a register:
//	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|---|-------|---|---|---|-----------|
//	| 1 | 1 | 1 | 0 |  NUMBER/  |dr |  SIZE |i/r| 1 | 1 | REGISTER  |
//	|   |   |   |   |  REGISTER |   |       |   |   |   |           |
//	-----------------------------------------------------------------
//
//	In the case of the rotating of a memory area:
//	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|-----------|-----------|
//	| 1 | 1 | 1 | 0 | 0 | 1 | 1 |dr | 1 | 1 |    MODE   | REGISTER  |
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
//	X - Not affected
//	N - Set if the result is negative. Cleared otherwise.
//	Z - Set if the result is zero. Cleared otherwise.
//	V - Always cleared
//	C - Set according to the last bit shifted out of the operand.
	
	@Override
	public void generate() {
		int base = 0xE018;
		GenInstruction ins = null;
		for (int dr = 0; dr < 2; dr++) {
			if (dr == 0) {
				for (int s = 0; s < 3; s++) {
					if (s == 0b00) {
						ins = new GenInstruction() {
							@Override
							public void run(int opcode) {
								RORRegisterByte(opcode);
							}
						};
					} else if (s == 0b01) {
						ins = new GenInstruction() {
							@Override
							public void run(int opcode) {
								RORRegisterWord(opcode);
							}
						};
					} else if (s == 0b10) {
						ins = new GenInstruction() {
							@Override
							public void run(int opcode) {
								RORRegisterLong(opcode);
							}
						};
					}
					
					for (int ir = 0; ir < 2; ir++) {
						for (int r = 0; r < 8; r++) {
							for (int nReg = 0; nReg < 8; nReg++) {
								int opcode = base | (nReg << 9) | (dr << 8) |(s << 6) | (ir << 5) | r;
								cpu.addInstruction(opcode, ins);
							}
						}
					}
				}
			} else {
				for (int s = 0; s < 3; s++) {
					if (s == 0b00) {
						ins = new GenInstruction() {
							@Override
							public void run(int opcode) {
								ROLRegisterByte(opcode);
							}
						};
					} else if (s == 0b01) {
						ins = new GenInstruction() {
							@Override
							public void run(int opcode) {
								ROLRegisterWord(opcode);
							}
						};
					} else if (s == 0b10) {
						ins = new GenInstruction() {
							@Override
							public void run(int opcode) {
								ROLRegisterLong(opcode);
							}
						};
					}
					
					for (int ir = 0; ir < 2; ir++) {
						for (int r = 0; r < 8; r++) {
							for (int nReg = 0; nReg < 8; nReg++) {
								int opcode = base | (nReg << 9) | (dr << 8) |(s << 6) | (ir << 5) | r;
								cpu.addInstruction(opcode, ins);
							}
						}
					}
				}
			}
		}
	}
	
	private void ROLRegisterByte(int opcode) {
		throw new RuntimeException("AA");
	}
	
	private void ROLRegisterWord(int opcode) {
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
		
		toShift &= 15;	//	wrap
		
		long data = cpu.getD(register) & 0xFFFF;
		long rot = (data << toShift);
		
		boolean carry = (rot & 0xFFFF_0000L) > 0;	// si se pasa del limite del size, carry = true
		
		long res = rot & 0xFFFF;
		for (int i = 0; i < toShift; i++) {		// rotacion de bits
			res = res | ((rot & (1 << (16 + i))) >> (16 - i));
		}
		cpu.setDWord(register, res);
		
		calcFlags(res, Size.WORD.getMsb(), 0xFFFF, carry);
	}
	
	private void ROLRegisterLong(int opcode) {
		throw new RuntimeException("AA");
	}
	
	private void RORRegisterByte(int opcode) {
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
		
		toShift &= 7;	//	wrap
		
		long data = cpu.getD(register) & 0xFF;
		long rot = (data >> toShift);
		
		long res = rot;
		for (int i = 0; i < toShift; i++) {		// rotacion de bits
			res = res | ((data & (1 << i)) << (8 - toShift));
		}
		boolean carry = (res != rot);	//	FIXME solo el ultimo bit !!!
		
		cpu.setDByte(register, res);
		
		calcFlags(res, Size.BYTE.getMsb(), 0xFF, carry);
	}
	
	private void RORRegisterWord(int opcode) {
		throw new RuntimeException("AA");
	}

	private void RORRegisterLong(int opcode) {
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
		
		toShift &= 31;	//	wrap
		
		long data = cpu.getD(register);
		long rot = (data >> toShift);
		
		long res = rot;
		for (int i = 0; i < toShift; i++) {		// rotacion de bits
			res = res | ((data & (1 << i)) << (32 - toShift));
		}
		boolean carry = (res != rot);	//	//	FIXME solo el ultimo bit !!!
		
		cpu.setDLong(register, res);
		
		calcFlags(res, Size.LONG.getMsb(), 0xFFFF_FFFFL, carry);
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
		} else {
			cpu.clearC();
		}
	}
	
}
