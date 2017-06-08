package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class ADD implements GenInstructionHandler {

	final Gen68 cpu;
	
	public ADD(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	ADD -- Add integer
//
//SYNOPSIS
//	ADD	<ea>,Dn
//	ADD	Dn,<ea>
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//	Adds the source operand to the destination operand using
//	binary addition, and stores the result in the destination location.
//	The size of the operation may be specified as byte, word, or long.
//	The mode of the instruction indicates which operand is the source and
//	which is the destination as well as the operand size.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|-----------|-----------|-----------|
//	| 1 | 1 | 0 | 1 |  REGISTER |  OP-MODE  |    MODE   |  REGISTER |
//	----------------------------------------=========================
//                                                          <ea>
//
//OP-MODE
//	Byte	Word	Long
//	~~~~	~~~~	~~~~
//	000		001		010	(Dn) + (<ea>) -> Dn
//	100		101		110	(<ea>) + (Dn) -> <ea>
//
//REGISTER
//	One of the 8 datas registers
//
//	If <ea> is source, allowed addressing modes are:
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn       |000 |N° reg. Dn| |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An *     |001 |N° reg. An| |    Abs.L      |111 |  001   |
//	|-------------------------------| |-----------------------------|
//	|     (An)      |010 |N° reg. An| |   (d16,PC)    |111 |  010   |
//	|-------------------------------| |-----------------------------|
//	|     (An)+     |011 |N° reg. An| |   (d8,PC,Xi)  |111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|    -(An)      |100 |N° reg. An| |   (bd,PC,Xi)  |111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (d16,An)    |101 |N° reg. An| |([bd,PC,Xi],od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (d8,An,Xi)  |110 |N° reg. An| |([bd,PC],Xi,od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (bd,An,Xi)  |110 |N° reg. An| |    #data      |111 |  100   |
//	|-------------------------------| -------------------------------
//	|([bd,An,Xi]od) |110 |N° reg. An|
//	|-------------------------------|
//	|([bd,An],Xi,od)|110 |N° reg. An|
//	---------------------------------
//	 * Word or Long only
//
//	If <ea> is destination, allowed addressing modes are:
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
//	When destination is an Address Register, ADDA instruction is used.
//
//RESULT
//	X - Set the same as the carry bit.
//	N - Set if the result is negative. Cleared otherwise.
//	Z - Set if the result is zero. Cleared otherwise.
//	V - Set if an overflow is generated. Cleared otherwise.
//	C - Set if a carry is generated. Cleared otherwise.
	
	@Override
	public void generate() {
		generateEASource();
		generateEADest();
	}
	
	private void generateEASource() {
		int base = 0xD000;
		GenInstruction ins = null;
		
		for (int opMode = 0; opMode < 3; opMode++) {
			if (opMode == 0b000) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ADD_EASource_Byte(opcode);
					}
				};
			} else if (opMode == 0b001) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ADD_EASource_Word(opcode);
					}
				};
			} else if (opMode == 0b010) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ADD_EASource_Long(opcode);
					}
				};
			}
			
			for (int m = 0; m < 8; m++) {
				for (int r = 0; r < 8; r++) {
					for (int register = 0; register < 8; register++) {
						if (m == 1 && opMode == 0b000) {	// byte size no tiene este modo
							continue;
						}
						if (m == 0b111 && r > 0b100) {
							continue;
						}
						int opcode = base | (register << 9) | (opMode << 6) | (m << 3) | r;
						cpu.addInstruction(opcode, ins);
					}
				}
			}
		}
	}
	
	private void generateEADest() {
		int base = 0xD100;
		GenInstruction ins = null;
		
		for (int opMode = 0; opMode < 3; opMode++) {
			if (opMode == 0b000) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ADD_EADest_Byte(opcode);
					}
				};
				
			} else if (opMode == 0b001) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ADD_EADest_Word(opcode);
					}
				};
				
			} else if (opMode == 0b010) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ADD_EADest_Long(opcode);
					}
				};
				
			}
			
			for (int m = 0; m < 8; m++) {
				for (int r = 0; r < 8; r++) {
					for (int register = 0; register < 8; register++) {
						if (m == 0 || m == 1) {
							continue;
						}
						if (m == 0b111 && r > 0b001) {
							continue;
						}
						int opcode = base | (register << 9) | (opMode << 6) | (m << 3) | r;
						cpu.addInstruction(opcode, ins);
					}
				}
			}
		}
	}


	private void ADD_EASource_Byte(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.BYTE, mode, register);
		long data = o.getAddressingMode().getByte(o);
		
		long toAdd = cpu.getDByte(dataRegister);
		
		long tot = (toAdd + data);
		cpu.setDByte(dataRegister, tot);
		
		boolean Dm = (data & 0x80) > 0;
		boolean Sm = (toAdd & 0x80) > 0;
		boolean Rm = (tot & 0x80) > 0;
		
		if((Sm && Dm && !Rm) || (!Sm && !Dm && Rm)) {
			cpu.setV();
		} else {
			cpu.clearV();
		}
		
		calcFlags(tot, data, toAdd, Size.BYTE.getMsb(), 0xFF);
	}
	
	private void ADD_EASource_Word(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long data = o.getAddressingMode().getWord(o);
		
		long toAdd = cpu.getDWord(dataRegister);
		
		long tot = (toAdd + data);
		cpu.setDWord(dataRegister, tot);
		
		boolean Dm = (data & 0x8000) > 0;
		boolean Sm = (toAdd & 0x8000) > 0;
		boolean Rm = (tot & 0x8000) > 0;
		
		if((Sm && Dm && !Rm) || (!Sm && !Dm && Rm)) {
			cpu.setV();
		} else {
			cpu.clearV();
		}
		
		calcFlags(tot, data, toAdd, Size.WORD.getMsb(), 0xFFFF);
	}
	
	private void ADD_EASource_Long(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		long data = cpu.getDLong(dataRegister);
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long toAdd = o.getAddressingMode().getLong(o);
		
		long tot = (data + toAdd);
		cpu.setDLong(dataRegister, tot);
		
		boolean Dm = (data & 0x8000_0000L) > 0;
		boolean Sm = (toAdd & 0x8000_0000L) > 0;
		boolean Rm = (tot & 0x8000_0000L) > 0;
		
		if((Sm && Dm && !Rm) || (!Sm && !Dm && Rm)) {
			cpu.setV();
		} else {
			cpu.clearV();
		}
		
		calcFlags(tot, data, toAdd, Size.LONG.getMsb(), 0xFFFF_FFFFL);
	}
	
	private void ADD_EADest_Byte(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		long toAdd = cpu.getDByte(dataRegister);
		
		Operation o = cpu.resolveAddressingMode(Size.BYTE, mode, register);
		long data = o.getAddressingMode().getByte(o);
		
		long tot = (toAdd + data);
		
		boolean Dm = (data & 0x80) > 0;
		boolean Sm = (toAdd & 0x80) > 0;
		boolean Rm = (tot & 0x80) > 0;
		
		if((Sm && Dm && !Rm) || (!Sm && !Dm && Rm)) {
			cpu.setV();
		} else {
			cpu.clearV();
		}
		
		cpu.writeKnownAddressingMode(o, tot, Size.BYTE);
		
		calcFlags(tot, data, toAdd, Size.BYTE.getMsb(), 0xFF);
	}
	
	private void ADD_EADest_Word(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		long toAdd = cpu.getDWord(dataRegister);
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long data = o.getAddressingMode().getWord(o);
		
		long tot = (data + toAdd);
		
		cpu.writeKnownAddressingMode(o, tot, Size.WORD);
		
		calcFlags(tot, data, toAdd, Size.WORD.getMsb(), 0xFFFF);
	}
	
	private void ADD_EADest_Long(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long data = o.getAddressingMode().getLong(o);

		long toAdd = cpu.getDLong(dataRegister);
		
		long tot = (data + toAdd);
		cpu.writeKnownAddressingMode(o, tot, Size.LONG);
		
		calcFlags(tot, data, toAdd, Size.LONG.getMsb(), Size.LONG.getMax());
	}
	
	void calcFlags(long tot, long data, long toAdd, long msb, long maxSize) {
		if ((tot & maxSize) == 0) {
			cpu.setZ();
		} else {
			cpu.clearZ();
		}
		if ((tot & msb) > 0) {
			cpu.setN();
		} else {
			cpu.clearN();
		}
		
		boolean Dm = (data & msb) > 0;
		boolean Sm = (toAdd & msb) > 0;
		boolean Rm = (tot & msb) > 0;
		
		if ((Sm && Dm && !Rm) || (!Sm && !Dm && Rm)) {
			cpu.setV();
		} else {
			cpu.clearV();
		}
		
		if ((Sm && Dm) || (!Rm && Dm) || (Sm && !Rm)) {
			cpu.setC();
			cpu.setX();
		} else {
			cpu.clearC();
			cpu.clearX();
		}
	}
	
}
