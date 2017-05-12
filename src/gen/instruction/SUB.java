package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class SUB implements GenInstructionHandler {

	final Gen68 cpu;
	
	public SUB(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	SUB -- Subtract
//
//SYNOPSIS
//	SUB	<ea>,Dn
//	SUB	Dn,<ea>
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//        Subtracts source operand to destination operand.
//        Result is stored to destination's place.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|-----------|-----------|-----------|
//	| 1 | 0 | 0 | 1 |  REGISTER |  OP-MODE  |    MODE   |  REGISTER |
//	----------------------------------------=========================
//                                                          <ea>
//
//OP-MODE
//	Byte	Word	Long
//	~~~~	~~~~	~~~~
//	000	001	010	(Dn) - (<ea>) -> Dn
//	100	101	110	(<ea>) - (Dn) -> <ea>
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
//	When destination is an Address Register, SUBA instruction is used.
//
//RESULT
//	X - Set the same as the carry bit.
//	N - Set if the result is negative. Cleared otherwise.
//	Z - Set if the result is zero. Cleared otherwise.
//	V - Set if an overflow is generated. Cleared otherwise.
//	C - Set if a carry is generated. Cleared otherwise.
	
	@Override
	public void generate() {
		int base = 0x9000;
		GenInstruction ins = null;
		
		for (int opMode = 0; opMode < 7; opMode++) {
			if (opMode == 0b000) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						SUB_EASource_Byte(opcode);
					}
				};
			} else if (opMode == 0b001) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						SUB_EASource_Word(opcode);
					}
				};
			} else if (opMode == 0b010) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						SUB_EASource_Long(opcode);
					}
				};
			} else if (opMode == 0b100) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						SUB_EADest_Byte(opcode);
					}
				};
			} else if (opMode == 0b101) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						SUB_EADest_Word(opcode);
					}
				};
			} else if (opMode == 0b110) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						SUB_EADest_Long(opcode);
					}
				};
			}
			
			if (opMode == 3) {	// opMode es 0, 1, 2 .. o 4, 5, 6
				continue;
			}
			
			for (int register = 0; register < 8; register++) {
				for (int m = 0; m < 8; m++) {
					if (m == 1 && opMode == 0b000) {	// byte size no tiene este modo
						continue;
					}
					for (int r = 0; r < 8; r++) {
						if (m == 0b111 & r > 0b100) {
							continue;
						}
						int opcode = base + ((register << 9) | (opMode << 6) | (m << 3) | r);
						cpu.addInstruction(opcode, ins);
					}
				}
			}
		}
		
	}
	
	private void SUB_EASource_Byte(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.BYTE, mode, register);
		long data = o.getAddressingMode().getByte(o);
		
		long toSub = (cpu.getD(dataRegister) & 0xFF);
		
		long tot = (toSub - data);
		cpu.setDByte(dataRegister, tot);
		
		calcFlags(tot, data, toSub, Size.BYTE.getMsb(), Size.BYTE.getMax());
	}
	
	private void SUB_EASource_Word(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long data = o.getAddressingMode().getWord(o);
		
		long toSub = (cpu.getD(dataRegister) & 0xFFFF);
		
		long tot = (toSub - data);
		cpu.setDWord(dataRegister, tot);
		
		calcFlags(tot, data, toSub, Size.WORD.getMsb(), Size.WORD.getMax());
	}
	
	private void SUB_EASource_Long(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long data = o.getAddressingMode().getLong(o);
		
		long toSub = cpu.getD(dataRegister);
		
		long tot = (toSub - data);
		cpu.setDLong(dataRegister, tot);
		
		calcFlags(tot, data, toSub, Size.LONG.getMsb(), Size.LONG.getMax());
	}
	
	private void SUB_EADest_Byte(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.BYTE, mode, register);
		long data = o.getAddressingMode().getByte(o);
		
		long toSub = cpu.getD(dataRegister) & 0xFF;
		long tot = (data - toSub);
	
		cpu.writeKnownAddressingMode(o, tot, Size.BYTE);
		
		calcFlags(tot, data, toSub, Size.BYTE.getMsb(), Size.BYTE.getMax());
	}
	
	private void SUB_EADest_Word(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long data = o.getAddressingMode().getWord(o);
		
		long toSub = cpu.getD(dataRegister) & 0xFFFF;
		long tot = (data - toSub);
	
		cpu.writeKnownAddressingMode(o, tot, Size.WORD);
		
		calcFlags(tot, data, toSub, Size.WORD.getMsb(), Size.WORD.getMax());
	}
	
	private void SUB_EADest_Long(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long data = o.getAddressingMode().getLong(o);
		
		long toSub = cpu.getD(dataRegister);
		long tot = (data - toSub);
	
		cpu.writeKnownAddressingMode(o, tot, Size.LONG);
		
		calcFlags(tot, data, toSub, Size.LONG.getMsb(), Size.LONG.getMax());
	}
	
	void calcFlags(long tot, long data, long toSub, long msb, long maxSize) {
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
		boolean Sm = (toSub & msb) > 0;
		boolean Rm = (tot & msb) > 0;
		if ((!Sm && Dm && !Rm) || (Sm && !Dm && Rm)) {
			cpu.setV();
		} else {
			cpu.clearV();
		}
		
		if (tot < 0) {
			cpu.setC();
			cpu.setX();
		} else {
			cpu.clearC();
			cpu.clearX();
		}
	}
	
}
