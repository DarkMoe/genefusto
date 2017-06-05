package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class ROXL implements GenInstructionHandler {

	final Gen68 cpu;
	
	public ROXL(Gen68 cpu) {
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
		generateImmRegROXL();
		generateMemoryROXL();
	}
	
	private void generateImmRegROXL() {
		int base = 0xE110;
		GenInstruction ins = null;
		
		for (int s = 0; s < 3; s++) {
			if (s == 0b00) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ROXLRegisterByte(opcode);
					}
				};
			} else if (s == 0b01) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ROXLRegisterWord(opcode);
					}
				};
			} else if (s == 0b10) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ROXLRegisterLong(opcode);
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
	
	private void generateMemoryROXL() {
		int base = 0xE5C0;
		GenInstruction ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				ROXLMemoryWord(opcode);
			}
		};
		
		for (int m = 0; m < 8; m++) {
			for (int r = 0; r < 8; r++) {
				int opcode = base | (m << 3) | r;
				cpu.addInstruction(opcode, ins);
			}
		}
	}

	private void ROXLRegisterByte(int opcode) {
		int register = (opcode & 0x7);
		boolean ir = cpu.bitTest(opcode, 5);
		int numRegister = (opcode >> 9) & 0x7;
		
		long shift;
		if (!ir) {
			if (numRegister == 0) {
				numRegister = 8;
			}
			shift = numRegister;
		} else {
			shift = cpu.getD(numRegister);
			shift = shift & 63;
		}
		
		long data = cpu.getD(register) & 0xFF;
		
		int last_out = 0;
		boolean extended = cpu.isX();
		for (int s= 0; s < shift; s++) {
			last_out = (int) (data & 0x80);                 // bit rotated out before ths shift
			data <<= 1;
			if (extended)
				data |= 1;                             // if xflag was set before the shift, set LSB
			if(last_out != 0)                       // bit goes to xflag
				extended = true;
			else
				extended = false;
		}
		data &= 0xFF;
		
		cpu.setDByte(register, data);
		
		calcFlags(data, Size.BYTE.getMsb(), 0xFF, extended);
	}
	
	private void ROXLRegisterWord(int opcode) {
		int register = (opcode & 0x7);
		boolean ir = cpu.bitTest(opcode, 5);
		int numRegister = (opcode >> 9) & 0x7;
		
		long shift;
		if (!ir) {
			if (numRegister == 0) {
				numRegister = 8;
			}
			shift = numRegister;
		} else {
			shift = cpu.getD(numRegister);
			shift = shift & 63;
		}
		
		long data = cpu.getD(register) & 0xFFFF;
		
		int last_out = 0;
		boolean extended = cpu.isX();
		for (int s= 0; s < shift; s++) {
			last_out = (int) (data & 0x8000);                 // bit rotated out before ths shift
			data <<= 1;
			if (extended)
				data |= 1;                             // if xflag was set before the shift, set LSB
			if(last_out != 0)                       // bit goes to xflag
				extended = true;
			else
				extended = false;
		}
		data &= 0xFFFF;
		
		cpu.setDWord(register, data);
		
		calcFlags(data, Size.WORD.getMsb(), 0xFFFF, extended);
	}
	
	private void ROXLRegisterLong(int opcode) {
		int register = (opcode & 0x7);
		boolean ir = cpu.bitTest(opcode, 5);
		int numRegister = (opcode >> 9) & 0x7;
		
		long shift;
		if (!ir) {
			if (numRegister == 0) {
				numRegister = 8;
			}
			shift = numRegister;
		} else {
			shift = cpu.getD(numRegister);
			shift = shift & 63;
		}
		
		long data = cpu.getD(register);
		
		int last_out = 0;
		boolean extended = cpu.isX();
		for (int s= 0; s < shift; s++) {
			last_out = (int) (data & 0x8000_0000);                 // bit rotated out before ths shift
			data <<= 1;
			if (extended)
				data |= 1;                             // if xflag was set before the shift, set LSB
			if(last_out != 0)                       // bit goes to xflag
				extended = true;
			else
				extended = false;
		}
		
		cpu.setDLong(register, data);
		
		calcFlags(data, Size.LONG.getMsb(), 0xFFFF_FFFFL, extended);
	}
	
	private void ROXLMemoryWord(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long data = o.getData() & 0xFFFF;
		
		int last_out = (int) (data & 0x8000);
		boolean extended = cpu.isX();
		data <<= 1;
		if (extended) {
			data |= 0x01;
		}
		data &= 0xFFFF;
		
		cpu.setDWord(register, data);
		
		calcFlags(data, Size.WORD.getMsb(), 0xFFFF, last_out != 0);
	}
	
	void calcFlags(long data, long msb, long maxSize, boolean ext) {
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
		
		if (ext) {
			cpu.setC();
			cpu.setX();
		} else {
			cpu.clearC();
			cpu.clearX();
		}
	}
	
}
